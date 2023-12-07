/*
 * Copyright (c) 2023 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.mappingio.adapter;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;

/**
 * Searches for inner classes with no mapped name, whose enclosing classes have mapped names,
 * and applies the latter to the outer part of the former's fully qualified name.
 *
 * <p>E.g it takes a class {@code class_123$class_124} that doesn't have a mapping,
 * tries to find {@code class_123}, which let's say has the mapping
 * {@code someClass}, and changes the former's name to {@code someClass$class124}.
 */
public class OuterClassNameInheritingVisitor extends ForwardingMappingVisitor {
	protected OuterClassNameInheritingVisitor(MappingVisitor next) {
		super(next);
	}

	@Override
	public Set<MappingFlag> getFlags() {
		Set<MappingFlag> ret = EnumSet.noneOf(MappingFlag.class);
		ret.addAll(next.getFlags());
		ret.add(MappingFlag.NEEDS_MULTIPLE_PASSES);

		return ret;
	}

	@Override
	public boolean visitHeader() throws IOException {
		if (pass < firstEmitPass) return true;

		return super.visitHeader();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		dstNsCount = dstNamespaces.size();

		if (pass == collectClassesPass) {
			visitedDstName = new boolean[dstNsCount];
		} else if (pass >= firstEmitPass) {
			super.visitNamespaces(srcNamespace, dstNamespaces);
		}
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		if (pass < firstEmitPass) return;

		super.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() throws IOException {
		if (pass < firstEmitPass) return true;

		return super.visitContent();
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		this.srcName = srcName;

		if (pass == collectClassesPass) {
			dstNameBySrcName.putIfAbsent(srcName, new String[dstNsCount]);
		} else if (pass >= firstEmitPass) {
			super.visitClass(srcName);
		}

		return true;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (pass == collectClassesPass) {
			if (targetKind != MappedElementKind.CLASS) return;

			dstNameBySrcName.get(srcName)[namespace] = name;
		} else if (pass >= firstEmitPass) {
			if (targetKind == MappedElementKind.CLASS) {
				visitedDstName[namespace] = true;
				name = dstNameBySrcName.get(srcName)[namespace];
			}

			super.visitDstName(targetKind, namespace, name);
		}
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		if (pass < firstEmitPass) return;

		// TODO: Remap type descriptors of changed classes
		super.visitDstDesc(targetKind, namespace, desc);
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (targetKind == MappedElementKind.CLASS && pass > collectClassesPass) {
			String[] dstNames = dstNameBySrcName.get(srcName);

			for (int ns = 0; ns < dstNames.length; ns++) {
				String dstName = dstNames[ns];

				if (pass == fixOuterClassesPass) {
					if (dstName != null) continue; // skip if already mapped

					String[] parts = srcName.split(Pattern.quote("$"));

					for (int pos = parts.length - 2; pos >= 0; pos--) {
						String outerSrcName = String.join("$", Arrays.copyOfRange(parts, 0, pos + 1));
						String outerDstName = dstNameBySrcName.get(outerSrcName)[ns];

						if (outerDstName != null) {
							dstName = outerDstName + "$" + String.join("$", Arrays.copyOfRange(parts, pos + 1, parts.length));

							dstNames[ns] = dstName;
							break;
						}
					}
				} else if (!visitedDstName[ns]) {
					if (dstName == null) continue; // skip if not mapped

					// Class didn't have a mapping before we added one,
					// so we have to call visitDstName manually.
					super.visitDstName(targetKind, ns, dstName);
				}
			}
		}

		if (pass < firstEmitPass) {
			return false; // prevent other element visits, we only care about classes here
		}

		Arrays.fill(visitedDstName, false);
		return super.visitElementContent(targetKind);
	}

	@Override
	public boolean visitEnd() throws IOException {
		if (pass++ < firstEmitPass) {
			return false;
		}

		return super.visitEnd();

	}

	private static final int collectClassesPass = 1;
	private static final int fixOuterClassesPass = 2;
	private static final int firstEmitPass = 3;
	private final boolean nextNeedsMultiplePasses = next.getFlags().contains(MappingFlag.NEEDS_MULTIPLE_PASSES);
	private final Map<String, String[]> dstNameBySrcName = new HashMap<>();
	private int pass = 1;
	private int dstNsCount = -1;
	private String srcName;
	private boolean[] visitedDstName;
}

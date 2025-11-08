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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;

/**
 * Searches for inner classes whose effective destination name contains outer classes referenced via their source name,
 * waits for mappings for these enclosing classes, and applies the latters' destination names
 * to the formers' fully qualified name.
 *
 * <p>For example, it takes a class {@code class_1$class_2} that doesn't have a mapping,
 * tries to find {@code class_1}, which let's say has the mapping {@code SomeClass},
 * and changes the former's destination name to {@code SomeClass$class_2}.
 *
 * @implNote This visitor requires two pre-passes: one to collect all classes and their mappings,
 * the other to actually apply the outer names. The third pass onwards will then emit the final mappings.
 */
public class OuterClassNamePropagator extends ForwardingMappingVisitor {
	/**
	 * Constructs a new {@link OuterClassNamePropagator} which processes all destination namespaces,
	 * including already remapped class destination names therein.
	 */
	public OuterClassNamePropagator(MappingVisitor next) {
		this(next, null, true);
	}

	/**
	 * Constructs a new {@link OuterClassNamePropagator} which processes the selected destination namespaces.
	 *
	 * @param namespaces The destination namespaces where outer class names shall be propagated. Pass {@code null} to process all destination namespaces.
	 * @param processRemappedDstNames Whether already remapped destination names should also get their unmapped outer classes replaced.
	 */
	public OuterClassNamePropagator(MappingVisitor next, @Nullable Collection<String> namespaces, boolean processRemappedDstNames) {
		super(next);
		this.dstNamespacesToProcess = namespaces;
		this.processRemappedDstNames = processRemappedDstNames;
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
		if (pass < FIST_EMIT_PASS) return true;

		return super.visitHeader();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		if (pass == COLLECT_CLASSES_PASS) {
			if (dstNamespacesToProcess == null) {
				dstNamespacesToProcess = dstNamespaces;
			} else {
				if (dstNamespacesToProcess.contains(srcNamespace)) {
					throw new UnsupportedOperationException(srcNamespace + " was passed as a destination namespace"
							+ " to propagate outer class names in, but has been visited as the source namespace.");
				}

				for (String ns : dstNamespacesToProcess) {
					if (!dstNamespaces.contains(ns)) {
						throw new IllegalArgumentException(ns + " was passed as a destination namespace to propagate outer class names in,"
								+ " but is not present in the namespaces of the current visitation pass.");
					}
				}
			}

			this.dstNamespaces = dstNamespaces;
			this.dstNsCount = dstNamespaces.size();

			if (dstNamespaceIndicesToProcess == null) {
				dstNamespaceIndicesToProcess = new ArrayList<>();

				for (int i = 0; i < dstNsCount; i++) {
					if (dstNamespacesToProcess.contains(dstNamespaces.get(i))) {
						dstNamespaceIndicesToProcess.add(i);
					}
				}
			}

			visitedDstName = new boolean[dstNsCount];
			dstNameBySrcNameByNamespace = new HashMap[dstNsCount];
		} else if (pass >= FIST_EMIT_PASS) {
			super.visitNamespaces(srcNamespace, dstNamespaces);
		}
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		if (pass < FIST_EMIT_PASS) return;

		super.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() throws IOException {
		if (pass < FIST_EMIT_PASS) return true;

		return super.visitContent();
	}

	@Override
	public boolean visitPackage(String srcName) throws IOException {
		if (pass < FIST_EMIT_PASS) return false;

		return super.visitPackage(srcName);
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		this.srcName = srcName;

		if (pass == COLLECT_CLASSES_PASS) {
			dstNamesBySrcName.putIfAbsent(srcName, new String[dstNsCount]);
		} else if (pass >= FIST_EMIT_PASS) {
			super.visitClass(srcName);
		}

		return true;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (pass == COLLECT_CLASSES_PASS) {
			if (targetKind != MappedElementKind.CLASS) return;

			dstNamesBySrcName.get(srcName)[namespace] = name;
		} else if (pass >= FIST_EMIT_PASS) {
			if (targetKind == MappedElementKind.CLASS) {
				visitedDstName[namespace] = true;
				name = dstNamesBySrcName.get(srcName)[namespace];
			}

			super.visitDstName(targetKind, namespace, name);
		}
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		if (pass < FIST_EMIT_PASS) return;

		if (modifiedClasses.contains(srcName)) {
			Map<String, String> nsDstNameBySrcName = dstNameBySrcNameByNamespace[namespace];

			if (nsDstNameBySrcName == null) {
				dstNameBySrcNameByNamespace[namespace] = nsDstNameBySrcName = dstNamesBySrcName.entrySet()
						.stream()
						.filter(entry -> entry.getValue()[namespace] != null)
						.collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()[namespace]), HashMap::putAll);
			}

			desc = MappingUtil.mapDesc(desc, nsDstNameBySrcName);
		}

		super.visitDstDesc(targetKind, namespace, desc);
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (targetKind == MappedElementKind.CLASS && pass > COLLECT_CLASSES_PASS) {
			String[] dstNames = dstNamesBySrcName.get(srcName);

			for (int ns = 0; ns < dstNames.length; ns++) {
				if (!dstNamespacesToProcess.contains(dstNamespaces.get(ns))) {
					continue;
				}

				String dstName = dstNames[ns];

				if (pass == FIX_OUTER_CLASSES_PASS) {
					if (!processRemappedDstNames && dstName != null && !dstName.equals(srcName)) {
						continue;
					}

					String[] srcParts = srcName.split(Pattern.quote("$"));
					String[] dstParts = dstName == null ? srcParts : dstName.split(Pattern.quote("$"));
					assert dstParts.length == srcParts.length;

					for (int pos = srcParts.length - 2; pos >= 0; pos--) {
						String outerSrcName = String.join("$", Arrays.copyOfRange(srcParts, 0, pos + 1));

						if (dstName != null && !dstParts[pos].equals(srcParts[pos])) {
							// That part already has a different mapping
							continue;
						}

						String outerDstName = dstNamesBySrcName.get(outerSrcName)[ns];

						if (outerDstName != null && !outerDstName.equals(outerSrcName)) {
							dstName = outerDstName + "$" + String.join("$", Arrays.copyOfRange(dstParts, pos + 1, dstParts.length));

							dstNames[ns] = dstName;
							modifiedClasses.add(srcName);
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

		if (pass < FIST_EMIT_PASS) {
			return false; // prevent other element visits, we only care about classes here
		}

		Arrays.fill(visitedDstName, false);
		return super.visitElementContent(targetKind);
	}

	@Override
	public boolean visitEnd() throws IOException {
		if (pass++ < FIST_EMIT_PASS) {
			return false;
		}

		return super.visitEnd();
	}

	private static final int COLLECT_CLASSES_PASS = 1;
	private static final int FIX_OUTER_CLASSES_PASS = 2;
	private static final int FIST_EMIT_PASS = 3;
	private final boolean processRemappedDstNames;
	private final Map<String, String[]> dstNamesBySrcName = new HashMap<>();
	private final Set<String> modifiedClasses = new HashSet<>();
	private List<String> dstNamespaces;
	private Collection<String> dstNamespacesToProcess;
	private Collection<Integer> dstNamespaceIndicesToProcess;
	private int pass = 1;
	private int dstNsCount = -1;
	private String srcName;
	private boolean[] visitedDstName;
	private Map<String, String>[] dstNameBySrcNameByNamespace;
}

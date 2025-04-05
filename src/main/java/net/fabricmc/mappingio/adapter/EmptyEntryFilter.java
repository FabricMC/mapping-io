/*
 * Copyright (c) 2025 FabricMC
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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;

/**
 * A mapping visitor that filters out elements which effectively don't contain any data.
 *
 * @implNote This visitor requires one pre-pass in which it determines which elements contain data worth forwarding.
 */
public final class EmptyEntryFilter extends ForwardingMappingVisitor {
	/**
	 * Creates a new {@link EmptyEntryFilter} that treats destination names and descriptors which are equal to their source counterparts as empty.
	 *
	 * @param next The next visitor to forward the data to.
	 */
	public EmptyEntryFilter(MappingVisitor next) {
		this(next, true);
	}

	/**
	 * @param next The next visitor to forward the data to.
	 * @param treatSrcOnDstAsEmpty Whether destination names and descriptors that are equal to their source counterparts should be treated as empty.
	 */
	public EmptyEntryFilter(MappingVisitor next, boolean treatSrcOnDstAsEmpty) {
		super(next);
		this.treatSrcOnDstAsEmpty = treatSrcOnDstAsEmpty;
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
		if (pass == COLLECT_PASS) {
			return true;
		}

		return super.visitHeader();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		clsCounter = memberCounter = localCounter = -129; // lowest cached Integer by HotSpot - 1

		if (pass == COLLECT_PASS) {
			return;
		}

		super.visitNamespaces(srcNamespace, dstNamespaces);
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		if (pass == COLLECT_PASS) {
			return;
		}

		super.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() throws IOException {
		if (pass == COLLECT_PASS) {
			return true;
		}

		return super.visitContent();
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		this.srcName = srcName;
		clsCounter++;

		if (pass > COLLECT_PASS) {
			if (forward = classesToForward.contains(clsCounter)) {
				super.visitClass(srcName);
			}
		}

		return true; // need to increment potential child elements' counters
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		this.srcName = srcName;
		this.srcDesc = srcDesc;
		memberCounter++;

		if (pass > COLLECT_PASS) {
			if (forward = membersToForward.contains(memberCounter)) {
				super.visitField(srcName, srcDesc);
			}
		}

		return true; // need to increment potential child elements' counters
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		this.srcName = srcName;
		this.srcDesc = srcDesc;
		memberCounter++;

		if (pass > COLLECT_PASS) {
			if (forward = membersToForward.contains(memberCounter)) {
				super.visitMethod(srcName, srcDesc);
			}
		}

		return true; // need to increment potential child elements' counters
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		this.srcName = srcName;
		localCounter++;

		if (pass == COLLECT_PASS) {
			return true;
		}

		return localsToForward.contains(localCounter)
				? super.visitMethodArg(argPosition, lvIndex, srcName)
				: false; // no child counters to increment, abort directly
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		this.srcName = srcName;
		localCounter++;

		if (pass == COLLECT_PASS) {
			return true;
		}

		return localsToForward.contains(localCounter)
				? super.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName)
				: false; // no child counters to increment, abort directly
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (pass > COLLECT_PASS) {
			if (forward) {
				super.visitDstName(targetKind, namespace, name);
			}

			return;
		}

		if (name == null || (treatSrcOnDstAsEmpty && name.equals(srcName))) {
			return;
		}

		switch (targetKind) {
		case METHOD_ARG:
		case METHOD_VAR:
			localsToForward.add(localCounter);
		case FIELD:
		case METHOD:
			membersToForward.add(memberCounter);
		case CLASS:
			classesToForward.add(clsCounter);
			break;
		default:
			throw new IllegalArgumentException("Unknown target kind: " + targetKind);
		}
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		if (pass > COLLECT_PASS) {
			if (forward) {
				super.visitDstDesc(targetKind, namespace, desc);
			}

			return;
		}

		if (desc == null || (treatSrcOnDstAsEmpty && desc.equals(srcDesc))) {
			return;
		}

		assert targetKind == MappedElementKind.FIELD || targetKind == MappedElementKind.METHOD;
		membersToForward.add(memberCounter);
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (pass == COLLECT_PASS) {
			return true;
		}

		if (forward) {
			return super.visitElementContent(targetKind);
		}

		return true;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		if (pass == COLLECT_PASS && comment != null) {
			switch (targetKind) {
			case METHOD_ARG:
			case METHOD_VAR:
				localsToForward.add(localCounter);
			case FIELD:
			case METHOD:
				membersToForward.add(memberCounter);
			case CLASS:
				classesToForward.add(clsCounter);
				break;
			default:
				throw new IllegalArgumentException("Unknown target kind: " + targetKind);
			}

			return;
		}

		if (forward) {
			super.visitComment(targetKind, comment);
		}
	}

	@Override
	public boolean visitEnd() throws IOException {
		if (pass++ == COLLECT_PASS) {
			return false;
		}

		return super.visitEnd();
	}

	private static final int COLLECT_PASS = 0;
	private final Set<Integer> classesToForward = new HashSet<>();
	private final Set<Integer> membersToForward = new HashSet<>();
	private final Set<Integer> localsToForward = new HashSet<>();
	private final boolean treatSrcOnDstAsEmpty;
	private int pass;
	private String srcName;
	private String srcDesc;
	private int clsCounter;
	private int memberCounter;
	private int localCounter;
	private boolean forward;
}

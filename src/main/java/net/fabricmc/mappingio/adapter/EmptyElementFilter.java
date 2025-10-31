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
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

/**
 * A mapping visitor that filters out elements which effectively don't contain any data.
 * Elements are only forwarded if they have:
 * <ul>
 * <li>a non-{@code null} comment,
 * <li>a non-{@code null} or non-equal destination name,
 * <li>a non-{@code null} or non-equal destination descriptor,
 * <li>or a child element to which one of these points applies.
 * </ul>
 */
public final class EmptyElementFilter extends ForwardingMappingVisitor {
	/**
	 * Creates a new {@link EmptyElementFilter} that treats destination names and descriptors which are equal to their source counterparts as empty.
	 *
	 * @param next The next visitor to forward the data to.
	 */
	public EmptyElementFilter(MappingVisitor next) {
		this(next, true);
	}

	/**
	 * @param next The next visitor to forward the data to.
	 * @param treatSrcOnDstAsEmpty Whether destination names and descriptors that are equal to their source counterparts should be treated as empty.
	 */
	public EmptyElementFilter(MappingVisitor next, boolean treatSrcOnDstAsEmpty) {
		super(next);
		this.treatSrcOnDstAsEmpty = treatSrcOnDstAsEmpty;
		init();
	}

	private void init() {
		relayHeaderElements = false;
		relayMetadata = false;
		dstNsCount = -1;
		memberKind = null;
		localKind = null;
		classSrcName = null;
		memberSrcName = null;
		memberSrcDesc = null;
		localSrcName = null;
		localLvIndex = -1;
		argPosition = -1;
		varLvtRowIndex = -1;
		varStartOpIdx = -1;
		varEndOpIdx = -1;
		classDstNames = null;
		memberDstNames = null;
		memberDstDescs = null;
		localDstNames = null;
		classComment = null;
		memberComment = null;
		localComment = null;
		forwardClass = false;
		forwardMember = false;
		forwardLocal = false;
		forwardedClass = false;
		forwardedMember = false;
		forwardedLocal = false;
		visitClass = true;
		visitMember = true;
		visitLocal = true;
	}

	@Override
	public void reset() {
		init();
		super.reset();
	}

	@Override
	public boolean visitHeader() throws IOException {
		relayMetadata = relayHeaderElements = super.visitHeader();
		return true;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		if (relayHeaderElements) {
			super.visitNamespaces(srcNamespace, dstNamespaces);
		}

		dstNsCount = dstNamespaces.size();
		classDstNames = new String[dstNsCount];
		memberDstNames = new String[dstNsCount];
		memberDstDescs = new String[dstNsCount];
		localDstNames = new String[dstNsCount];
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		if (relayMetadata) {
			super.visitMetadata(key, value);
		}
	}

	@Override
	public boolean visitContent() throws IOException {
		return relayMetadata = super.visitContent(); // for in-content metadata
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		forwardClass = false;
		forwardMember = false;
		forwardLocal = false;
		forwardedClass = false;
		visitClass = true;
		classSrcName = srcName;
		Arrays.fill(classDstNames, null);
		classComment = null;
		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		memberKind = MappedElementKind.FIELD;
		return visitMember(srcName, srcDesc);
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		memberKind = MappedElementKind.METHOD;
		return visitMember(srcName, srcDesc);
	}

	private boolean visitMember(String srcName, @Nullable String srcDesc) throws IOException {
		forwardMember = false;
		forwardLocal = false;
		forwardedMember = false;
		visitMember = true;
		memberSrcName = srcName;
		memberSrcDesc = srcDesc;
		Arrays.fill(memberDstNames, null);
		Arrays.fill(memberDstDescs, null);
		memberComment = null;
		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		localKind = MappedElementKind.METHOD_ARG;
		this.argPosition = argPosition;
		return visitLocal(lvIndex, srcName);
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		localKind = MappedElementKind.METHOD_VAR;
		this.varLvtRowIndex = lvtRowIndex;
		this.varStartOpIdx = startOpIdx;
		this.varEndOpIdx = endOpIdx;
		return visitLocal(lvIndex, srcName);
	}

	private boolean visitLocal(int lvIndex, @Nullable String srcName) throws IOException {
		forwardLocal = false;
		forwardedLocal = false;
		visitLocal = true;
		localSrcName = srcName;
		localLvIndex = lvIndex;
		Arrays.fill(localDstNames, null);
		localComment = null;
		return true;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		boolean forward = name != null && !(treatSrcOnDstAsEmpty && name.equals(getSrcName(targetKind)));

		switch (targetKind) {
		case CLASS:
			forwardClass |= forward;
			classDstNames[namespace] = name;
			break;
		case FIELD:
		case METHOD:
			forwardClass |= forward;
			forwardMember |= forward;
			memberDstNames[namespace] = name;
			break;
		case METHOD_ARG:
		case METHOD_VAR:
			forwardClass |= forward;
			forwardMember |= forward;
			forwardLocal |= forward;
			localDstNames[namespace] = name;
			break;
		default:
			throw new IllegalArgumentException("Unknown target kind: " + targetKind);
		}
	}

	private String getSrcName(MappedElementKind targetKind) {
		switch (targetKind) {
		case CLASS:
			return classSrcName;
		case FIELD:
		case METHOD:
			return memberSrcName;
		case METHOD_ARG:
		case METHOD_VAR:
			return localSrcName;
		default:
			throw new IllegalArgumentException("Unknown target kind: " + targetKind);
		}
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		assert targetKind == memberKind;
		boolean forward = desc != null && !(treatSrcOnDstAsEmpty && desc.equals(memberSrcDesc));

		forwardClass |= forward;
		forwardMember |= forward;
		memberDstDescs[namespace] = desc;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		forward(targetKind);
		return true;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		if (comment == null) {
			return;
		}

		switch (targetKind) {
		case CLASS:
			forwardClass = true;
			classComment = comment;
			break;
		case FIELD:
		case METHOD:
			forwardClass = true;
			forwardMember = true;
			memberComment = comment;
			break;
		case METHOD_ARG:
		case METHOD_VAR:
			forwardClass = true;
			forwardMember = true;
			forwardLocal = true;
			localComment = comment;
			break;
		default:
			throw new IllegalArgumentException("Unknown target kind: " + targetKind);
		}

		forward(targetKind);
	}

	private void forward(MappedElementKind targetKind) throws IOException {
		if (forwardClass && !forwardedClass && visitClass) {
			if (visitClass = super.visitClass(classSrcName)) {
				for (int i = 0; i < dstNsCount; i++) {
					if (classDstNames[i] != null) {
						super.visitDstName(MappedElementKind.CLASS, i, classDstNames[i]);
					}
				}

				visitClass = super.visitElementContent(MappedElementKind.CLASS);
				forwardedClass = true;

				if (visitClass && classComment != null) {
					super.visitComment(MappedElementKind.CLASS, classComment);
				}
			}
		}

		if (forwardMember && !forwardedMember && visitClass && visitMember) {
			if (memberKind == MappedElementKind.FIELD) {
				visitMember = super.visitField(memberSrcName, memberSrcDesc);
			} else {
				visitMember = super.visitMethod(memberSrcName, memberSrcDesc);
			}

			if (visitMember) {
				for (int i = 0; i < dstNsCount; i++) {
					if (memberDstNames[i] != null) {
						super.visitDstName(memberKind, i, memberDstNames[i]);
					}

					if (memberDstDescs[i] != null) {
						super.visitDstDesc(memberKind, i, memberDstDescs[i]);
					}
				}

				visitMember = super.visitElementContent(memberKind);
				forwardedMember = true;

				if (visitMember && memberComment != null) {
					super.visitComment(memberKind, memberComment);
				}
			}
		}

		if (forwardLocal && !forwardedLocal && visitClass && visitMember && visitLocal) {
			if (localKind == MappedElementKind.METHOD_ARG) {
				visitLocal = super.visitMethodArg(argPosition, localLvIndex, localSrcName);
			} else {
				visitLocal = super.visitMethodVar(varLvtRowIndex, localLvIndex, varStartOpIdx, varEndOpIdx, localSrcName);
			}

			if (visitLocal) {
				for (int i = 0; i < dstNsCount; i++) {
					if (localDstNames[i] != null) {
						super.visitDstName(localKind, i, localDstNames[i]);
					}
				}

				visitLocal = super.visitElementContent(localKind);
				forwardedLocal = true;

				if (visitLocal && localComment != null) {
					super.visitComment(localKind, localComment);
				}
			}
		}
	}

	@Override
	public boolean visitEnd() throws IOException {
		init();
		return super.visitEnd();
	}

	private final boolean treatSrcOnDstAsEmpty;
	private boolean relayHeaderElements;
	private boolean relayMetadata;
	private int dstNsCount;
	private MappedElementKind memberKind;
	private MappedElementKind localKind;
	private String classSrcName;
	private String memberSrcName;
	private String memberSrcDesc;
	private String localSrcName;
	private int localLvIndex;
	private int argPosition;
	private int varLvtRowIndex;
	private int varStartOpIdx;
	private int varEndOpIdx;
	private String[] classDstNames;
	private String[] memberDstNames;
	private String[] memberDstDescs;
	private String[] localDstNames;
	private String classComment;
	private String memberComment;
	private String localComment;
	private boolean forwardClass;
	private boolean forwardMember;
	private boolean forwardLocal;
	private boolean forwardedClass;
	private boolean forwardedMember;
	private boolean forwardedLocal;
	private boolean visitClass;
	private boolean visitMember;
	private boolean visitLocal;
}

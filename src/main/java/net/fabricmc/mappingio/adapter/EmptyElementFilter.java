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
 *
 * @apiNote Extending this class is allowed, but no guarantees are made regarding the stability of its protected members.
 */
public class EmptyElementFilter extends ForwardingMappingVisitor {
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

	protected void init() {
		relayHeaderElements = false;
		relayMetadata = false;
		dstNsCount = -1;
		memberKind = null;
		localKind = null;
		packageOrClassSrcName = null;
		memberSrcName = null;
		memberSrcDesc = null;
		localSrcName = null;
		localLvIndex = -1;
		argPosition = -1;
		varLvtRowIndex = -1;
		varStartOpIdx = -1;
		varEndOpIdx = -1;
		packageOrClassDstNames = null;
		memberDstNames = null;
		memberDstDescs = null;
		localDstNames = null;
		packageOrClassComment = null;
		memberComment = null;
		localComment = null;
		forwardPackageOrClass = false;
		forwardMember = false;
		forwardLocal = false;
		forwardedPackageOrClass = false;
		forwardedMember = false;
		forwardedLocal = false;
		visitPackageOrClass = true;
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
		packageOrClassDstNames = new String[dstNsCount];
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
	public boolean visitPackage(String srcName) throws IOException {
		packageOrClassKind = MappedElementKind.PACKAGE;
		return visitPackageOrClass(srcName);
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		packageOrClassKind = MappedElementKind.CLASS;
		return visitPackageOrClass(srcName);
	}

	protected boolean visitPackageOrClass(String srcName) throws IOException {
		forwardPackageOrClass = false;
		forwardMember = false;
		forwardLocal = false;
		forwardedPackageOrClass = false;
		visitPackageOrClass = true;
		packageOrClassSrcName = srcName;
		Arrays.fill(packageOrClassDstNames, null);
		packageOrClassComment = null;
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

	protected boolean visitMember(String srcName, @Nullable String srcDesc) throws IOException {
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

	protected boolean visitLocal(int lvIndex, @Nullable String srcName) throws IOException {
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
		case PACKAGE:
		case CLASS:
			forwardPackageOrClass |= forward;
			packageOrClassDstNames[namespace] = name;
			break;
		case FIELD:
		case METHOD:
			forwardPackageOrClass |= forward;
			forwardMember |= forward;
			memberDstNames[namespace] = name;
			break;
		case METHOD_ARG:
		case METHOD_VAR:
			forwardPackageOrClass |= forward;
			forwardMember |= forward;
			forwardLocal |= forward;
			localDstNames[namespace] = name;
			break;
		default:
			throw new IllegalArgumentException("Unknown target kind: " + targetKind);
		}
	}

	protected String getSrcName(MappedElementKind targetKind) {
		switch (targetKind) {
		case PACKAGE:
		case CLASS:
			return packageOrClassSrcName;
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

		forwardPackageOrClass |= forward;
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
		case PACKAGE:
		case CLASS:
			forwardPackageOrClass = true;
			packageOrClassComment = comment;
			break;
		case FIELD:
		case METHOD:
			forwardPackageOrClass = true;
			forwardMember = true;
			memberComment = comment;
			break;
		case METHOD_ARG:
		case METHOD_VAR:
			forwardPackageOrClass = true;
			forwardMember = true;
			forwardLocal = true;
			localComment = comment;
			break;
		default:
			throw new IllegalArgumentException("Unknown target kind: " + targetKind);
		}

		forward(targetKind);
	}

	protected void forward(MappedElementKind targetKind) throws IOException {
		if (forwardPackageOrClass && !forwardedPackageOrClass && visitPackageOrClass) {
			if (packageOrClassKind == MappedElementKind.PACKAGE) {
				visitPackageOrClass = super.visitPackage(packageOrClassSrcName);
			} else {
				visitPackageOrClass = super.visitClass(packageOrClassSrcName);
			}

			if (visitPackageOrClass) {
				for (int i = 0; i < dstNsCount; i++) {
					if (packageOrClassDstNames[i] != null) {
						super.visitDstName(packageOrClassKind, i, packageOrClassDstNames[i]);
					}
				}

				visitPackageOrClass = super.visitElementContent(packageOrClassKind);
				forwardedPackageOrClass = true;

				if (visitPackageOrClass && packageOrClassComment != null) {
					super.visitComment(packageOrClassKind, packageOrClassComment);
				}
			}
		}

		if (forwardMember && !forwardedMember && visitPackageOrClass && visitMember) {
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

		if (forwardLocal && !forwardedLocal && visitPackageOrClass && visitMember && visitLocal) {
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

	protected final boolean treatSrcOnDstAsEmpty;
	protected boolean relayHeaderElements;
	protected boolean relayMetadata;
	protected int dstNsCount;
	protected MappedElementKind packageOrClassKind;
	protected MappedElementKind memberKind;
	protected MappedElementKind localKind;
	protected String packageOrClassSrcName;
	protected String memberSrcName;
	protected String memberSrcDesc;
	protected String localSrcName;
	protected int localLvIndex;
	protected int argPosition;
	protected int varLvtRowIndex;
	protected int varStartOpIdx;
	protected int varEndOpIdx;
	protected String[] packageOrClassDstNames;
	protected String[] memberDstNames;
	protected String[] memberDstDescs;
	protected String[] localDstNames;
	protected String packageOrClassComment;
	protected String memberComment;
	protected String localComment;
	protected boolean forwardPackageOrClass;
	protected boolean forwardMember;
	protected boolean forwardLocal;
	protected boolean forwardedPackageOrClass;
	protected boolean forwardedMember;
	protected boolean forwardedLocal;
	protected boolean visitPackageOrClass;
	protected boolean visitMember;
	protected boolean visitLocal;
}

/*
 * Copyright (c) 2024 FabricMC
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

package net.fabricmc.mappingio;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;

/**
 * Visitor which verifies on each visit call that the invoked visits were in accordance
 * with the expected order of visitation, as defined in {@link MappingVisitor}'s Javadocs.
 */
public class VisitOrderVerifyingVisitor extends ForwardingMappingVisitor {
	public VisitOrderVerifyingVisitor(MappingVisitor next) {
		super(next);
		init();
	}

	private void init() {
		visitedHeader = false;
		visitedNamespaces = false;
		visitedMetadata = false;
		visitedContent = false;
		visitedClass = false;
		visitedField = false;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		resetVisitedElementContentUpTo(0);
		visitedEnd = false;
		lastVisitedElement = null;
		visitedLastElement = false;
		lastSrcInfo.clear();
	}

	private void resetVisitedElementContentUpTo(int inclusiveLevel) {
		for (int i = visitedElementContent.length - 1; i >= inclusiveLevel; i--) {
			visitedElementContent[i] = false;
		}
	}

	@Override
	public boolean visitHeader() throws IOException {
		assertHeaderNotVisited();

		visitedEnd = false;
		visitedHeader = true;
		return super.visitHeader();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		assertHeaderVisited();
		assertNamespacesNotVisited();

		visitedNamespaces = true;
		super.visitNamespaces(srcNamespace, dstNamespaces);
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		assertNamespacesVisited();

		visitedMetadata = true;
		visitedClass = false;
		visitedField = false;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		super.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() throws IOException {
		assertNamespacesVisited();
		assertContentNotVisited();

		visitedContent = true;
		return super.visitContent();
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		MappedElementKind elementKind = MappedElementKind.CLASS;
		SrcInfo srcInfo = new SrcInfo().srcName(srcName);

		assertContentVisited();
		assertLastElementContentVisited();
		assertNewSrcInfo(elementKind, srcInfo);

		visitedClass = true;
		visitedField = false;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		lastSrcInfo.put(elementKind, srcInfo);
		resetVisitedElementContentUpTo(elementKind.level);

		return visitedLastElement = super.visitClass(srcName);
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		MappedElementKind elementKind = MappedElementKind.FIELD;
		SrcInfo srcInfo = new SrcInfo()
				.srcName(srcName)
				.srcDesc(srcDesc);

		assertClassVisited();
		assertLastElementContentVisited();
		assertElementContentVisited(elementKind.level - 1);
		assertNewSrcInfo(elementKind, srcInfo);

		visitedField = true;
		visitedMethod = false;
		visitedMethodArg = false;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		lastSrcInfo.put(elementKind, srcInfo);
		resetVisitedElementContentUpTo(elementKind.level);

		return visitedLastElement = super.visitField(srcName, srcDesc);
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		MappedElementKind elementKind = MappedElementKind.METHOD;
		SrcInfo srcInfo = new SrcInfo()
				.srcName(srcName)
				.srcDesc(srcDesc);

		assertClassVisited();
		assertLastElementContentVisited();
		assertElementContentVisited(elementKind.level - 1);
		assertNewSrcInfo(elementKind, srcInfo);

		visitedField = false;
		visitedMethod = true;
		visitedMethodArg = false;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		lastSrcInfo.put(elementKind, srcInfo);
		resetVisitedElementContentUpTo(elementKind.level);

		return visitedLastElement = super.visitMethod(srcName, srcDesc);
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		MappedElementKind elementKind = MappedElementKind.METHOD_ARG;
		SrcInfo srcInfo = new SrcInfo()
				.argPosition(argPosition)
				.lvIndex(lvIndex)
				.srcName(srcName);

		assertFieldNotVisited();
		assertMethodVisited();
		assertLastElementContentVisited();
		assertElementContentVisited(elementKind.level - 1);
		assertNewSrcInfo(elementKind, srcInfo);

		visitedMethodArg = true;
		visitedMethodVar = false;
		lastVisitedElement = elementKind;
		lastSrcInfo.put(elementKind, srcInfo);
		resetVisitedElementContentUpTo(elementKind.level);

		return visitedLastElement = super.visitMethodArg(argPosition, lvIndex, srcName);
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		MappedElementKind elementKind = MappedElementKind.METHOD_VAR;
		SrcInfo srcInfo = new SrcInfo()
				.lvtRowIndex(lvtRowIndex)
				.lvIndex(lvIndex)
				.startOpIdx(startOpIdx)
				.endOpIdx(endOpIdx)
				.srcName(srcName);

		assertFieldNotVisited();
		assertMethodVisited();
		assertLastElementContentVisited();
		assertElementContentVisited(elementKind.level - 1);
		assertNewSrcInfo(elementKind, srcInfo);

		visitedMethodArg = false;
		visitedMethodVar = true;
		lastVisitedElement = elementKind;
		lastSrcInfo.put(elementKind, srcInfo);
		resetVisitedElementContentUpTo(elementKind.level);

		return visitedLastElement = super.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		assertElementVisited(targetKind);
		assertElementContentNotVisitedUpTo(targetKind.level + 1); // prevent visitation after visitElementContent of same level

		super.visitDstName(targetKind, namespace, name);
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		assertElementVisited(targetKind);
		assertElementContentNotVisitedUpTo(targetKind.level + 1); // prevent visitation after visitElementContent of same level

		super.visitDstDesc(targetKind, namespace, desc);
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		assertElementVisited(targetKind);
		assertElementContentNotVisitedUpTo(targetKind.level); // no +1 to prevent repeated visitation

		if (targetKind.level > 0) {
			assertElementContentVisited(targetKind.level - 1);
		}

		visitedElementContent[targetKind.level] = true;

		if (targetKind.level < visitedElementContent.length - 1) {
			resetVisitedElementContentUpTo(targetKind.level + 1);
		}

		return super.visitElementContent(targetKind);
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		assertElementVisited(targetKind);
		assertLastElementContentVisited();

		super.visitComment(targetKind, comment);
	}

	@Override
	public boolean visitEnd() throws IOException {
		assertContentVisited();
		assertLastElementContentVisited();
		assertEndNotVisited();

		init();
		visitedEnd = true;
		return super.visitEnd();
	}

	private void assertHeaderNotVisited() {
		if (visitedHeader) {
			throw new IllegalStateException("Header already visited");
		}
	}

	private void assertHeaderVisited() {
		if (!visitedHeader) {
			throw new IllegalStateException("Header not visited");
		}
	}

	private void assertNamespacesNotVisited() {
		if (visitedNamespaces) {
			throw new IllegalStateException("Namespaces already visited");
		}
	}

	private void assertNamespacesVisited() {
		if (!visitedNamespaces) {
			throw new IllegalStateException("Namespaces not visited");
		}
	}

	private void assertMetadataNotVisited() {
		if (visitedMetadata) {
			throw new IllegalStateException("Metadata already visited");
		}
	}

	private void assertMetadataVisited() {
		if (!visitedMetadata) {
			throw new IllegalStateException("Metadata not visited");
		}
	}

	private void assertContentNotVisited() {
		if (visitedContent) {
			throw new IllegalStateException("Content already visited");
		}
	}

	private void assertContentVisited() {
		if (!visitedContent) {
			throw new IllegalStateException("Content not visited");
		}
	}

	private void assertElementVisited(MappedElementKind kind) {
		if (lastVisitedElement != kind) {
			throw new IllegalStateException("Element not visited");
		}
	}

	private void assertClassNotVisited() {
		if (visitedClass) {
			throw new IllegalStateException("Class already visited");
		}
	}

	private void assertClassVisited() {
		if (!visitedClass) {
			throw new IllegalStateException("Class not visited");
		}
	}

	private void assertFieldNotVisited() {
		if (visitedField) {
			throw new IllegalStateException("Field already visited");
		}
	}

	private void assertFieldVisited() {
		if (!visitedField) {
			throw new IllegalStateException("Field not visited");
		}
	}

	private void assertMethodNotVisited() {
		if (visitedMethod) {
			throw new IllegalStateException("Method already visited");
		}
	}

	private void assertMethodVisited() {
		if (!visitedMethod) {
			throw new IllegalStateException("Method not visited");
		}
	}

	private void assertMethodArgNotVisited() {
		if (visitedMethodArg) {
			throw new IllegalStateException("Method argument already visited");
		}
	}

	private void assertMethodArgVisited() {
		if (!visitedMethodArg) {
			throw new IllegalStateException("Method argument not visited");
		}
	}

	private void assertMethodVarNotVisited() {
		if (visitedMethodVar) {
			throw new IllegalStateException("Method variable already visited");
		}
	}

	private void assertMethodVarVisited() {
		if (!visitedMethodVar) {
			throw new IllegalStateException("Method variable not visited");
		}
	}

	private void assertLastElementContentVisited() {
		if (visitedLastElement) {
			assertElementContentVisited(lastVisitedElement.level);
			assertElementContentNotVisitedUpTo(lastVisitedElement.level + 1);
		}
	}

	private void assertElementContentNotVisitedUpTo(int inclusiveLevel) {
		for (int i = visitedElementContent.length - 1; i >= inclusiveLevel; i--) {
			if (visitedElementContent[i]) {
				throw new IllegalStateException(lastVisitedElement + " element content already visited");
			}
		}
	}

	private void assertElementContentVisited(int depth) {
		if (!visitedElementContent[depth]) {
			throw new IllegalStateException(lastVisitedElement + " element content not visited");
		}
	}

	private void assertEndNotVisited() {
		if (visitedEnd) {
			throw new IllegalStateException("End already visited");
		}
	}

	private void assertNewSrcInfo(MappedElementKind kind, SrcInfo srcInfo) {
		if (srcInfo.equals(lastSrcInfo.get(kind))) {
			throw new IllegalStateException("Same source name visited twice in a row");
		}
	}

	boolean visitedHeader;
	boolean visitedNamespaces;
	boolean visitedMetadata;
	boolean visitedContent;
	boolean visitedClass;
	boolean visitedField;
	boolean visitedMethod;
	boolean visitedMethodArg;
	boolean visitedMethodVar;
	boolean[] visitedElementContent = new boolean[3];
	boolean visitedEnd;
	MappedElementKind lastVisitedElement;
	boolean visitedLastElement;
	Map<MappedElementKind, SrcInfo> lastSrcInfo = new HashMap<>();

	private static class SrcInfo {
		SrcInfo srcName(String srcName) {
			this.srcName = srcName;
			return this;
		}

		SrcInfo srcDesc(String srcDesc) {
			this.srcDesc = srcDesc;
			return this;
		}

		SrcInfo argPosition(int argPosition) {
			this.argPosition = argPosition;
			return this;
		}

		SrcInfo lvIndex(int lvIndex) {
			this.lvIndex = lvIndex;
			return this;
		}

		SrcInfo lvtRowIndex(int lvtRowIndex) {
			this.lvtRowIndex = lvtRowIndex;
			return this;
		}

		SrcInfo startOpIdx(int startOpIdx) {
			this.startOpIdx = startOpIdx;
			return this;
		}

		SrcInfo endOpIdx(int endOpIdx) {
			this.endOpIdx = endOpIdx;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null) {
				return false;
			}

			if (o instanceof SrcInfo) {
				SrcInfo other = (SrcInfo) o;

				return Objects.equals(srcName, other.srcName)
						&& Objects.equals(srcDesc, other.srcDesc)
						&& argPosition == other.argPosition
						&& lvIndex == other.lvIndex
						&& lvtRowIndex == other.lvtRowIndex
						&& startOpIdx == other.startOpIdx
						&& endOpIdx == other.endOpIdx;
			}

			return false;
		}

		private String srcName;
		private String srcDesc;
		private int argPosition;
		private int lvIndex;
		private int lvtRowIndex;
		private int startOpIdx;
		private int endOpIdx;
	}
}

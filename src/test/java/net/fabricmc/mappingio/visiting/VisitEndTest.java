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

package net.fabricmc.mappingio.visiting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.SubsetAssertingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class VisitEndTest {
	@Test
	public void enigmaFile() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;
		check(format);
	}

	@Test
	public void enigmaDirectory() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_DIR;
		check(format);
	}

	@Test
	public void tinyFile() throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;
		check(format);
	}

	@Test
	public void tinyV2File() throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;
		check(format);
	}

	@Test
	public void srgFile() throws Exception {
		MappingFormat format = MappingFormat.SRG_FILE;
		check(format);
	}

	@Test
	public void xrgFile() throws Exception {
		MappingFormat format = MappingFormat.XSRG_FILE;
		check(format);
	}

	@Test
	public void jamFile() throws Exception {
		MappingFormat format = MappingFormat.JAM_FILE;
		check(format);
	}

	@Test
	public void csrgFile() throws Exception {
		MappingFormat format = MappingFormat.CSRG_FILE;
		check(format);
	}

	@Test
	public void tsrgFile() throws Exception {
		MappingFormat format = MappingFormat.TSRG_FILE;
		check(format);
	}

	@Test
	public void tsrg2File() throws Exception {
		MappingFormat format = MappingFormat.TSRG_2_FILE;
		check(format);
	}

	@Test
	public void proguardFile() throws Exception {
		MappingFormat format = MappingFormat.PROGUARD_FILE;
		check(format);
	}

	private void check(MappingFormat format) throws Exception {
		checkDir(TestHelper.MappingDirs.DETECTION, format);
		checkDir(TestHelper.MappingDirs.VALID, format);
		checkDir(TestHelper.MappingDirs.VALID_WITH_HOLES, format);
	}

	private void checkDir(Path dir, MappingFormat format) throws Exception {
		Path path = dir.resolve(TestHelper.getFileName(format));
		MappingTreeView supTree = TestHelper.MappingDirs.getCorrespondingTree(dir);

		checkCompliance(format, path, 1, true, supTree);
		checkCompliance(format, path, 1, false, supTree);

		checkCompliance(format, path, 2, true, supTree);
		checkCompliance(format, path, 3, true, supTree);

		VisitEndTestVisitor nonFlaggedVisitor;

		try {
			nonFlaggedVisitor = checkCompliance(format, path, 2, false, supTree);
		} catch (Exception e) {
			return; // Reader doesn't support multiple passes without NEEDS_MULTIPLE_PASSES
		}

		// Reader didn't throw an exception, make sure it actually behaved as expected
		assertTrue(nonFlaggedVisitor.finishedVisitPassCount == nonFlaggedVisitor.visitPassCountToFinish);
	}

	private VisitEndTestVisitor checkCompliance(MappingFormat format, Path path, int visitPassCountToFinish, boolean setFlag, MappingTreeView supTree) throws Exception {
		VisitEndTestVisitor visitor = new VisitEndTestVisitor(visitPassCountToFinish, setFlag, supTree, format);
		MappingReader.read(path, format, visitor);
		assertTrue(visitor.finishedVisitPassCount == visitPassCountToFinish);
		return visitor;
	}

	private static class VisitEndTestVisitor implements MappingVisitor {
		private VisitEndTestVisitor(int visitPassCountToFinish, boolean setFlag, MappingTreeView supTree, MappingFormat subFormat) {
			this.visitPassCountToFinish = visitPassCountToFinish;
			this.setFlag = setFlag;
			this.supTree = supTree;
			this.subFormat = subFormat;
			this.tree = new MemoryMappingTree();
			this.oldTrees = new MappingTree[visitPassCountToFinish - 1];
		}

		@Override
		public Set<MappingFlag> getFlags() {
			return setFlag
					? EnumSet.of(MappingFlag.NEEDS_MULTIPLE_PASSES)
					: MappingFlag.NONE;
		}

		@Override
		public boolean visitHeader() throws IOException {
			check();
			tree.visitHeader();
			return true;
		}

		@Override
		public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
			check();
			tree.visitNamespaces(srcNamespace, dstNamespaces);
		}

		@Override
		public void visitMetadata(String key, @Nullable String value) throws IOException {
			check();
			tree.visitMetadata(key, value);
		}

		@Override
		public boolean visitContent() throws IOException {
			check();
			tree.visitContent();
			return true;
		}

		@Override
		public boolean visitClass(String srcName) throws IOException {
			check();
			tree.visitClass(srcName);
			return true;
		}

		@Override
		public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
			check();
			tree.visitField(srcName, srcDesc);
			return true;
		}

		@Override
		public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
			check();
			tree.visitMethod(srcName, srcDesc);
			return true;
		}

		@Override
		public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
			check();
			tree.visitMethodArg(argPosition, lvIndex, srcName);
			return true;
		}

		@Override
		public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
			check();
			tree.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
			return true;
		}

		@Override
		public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
			check();
			tree.visitDstName(targetKind, namespace, name);
		}

		@Override
		public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
			check();
			tree.visitDstDesc(targetKind, namespace, desc);
		}

		@Override
		public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
			check();
			tree.visitElementContent(targetKind);
			return true;
		}

		@Override
		public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
			check();
			tree.visitComment(targetKind, comment);
		}

		@Override
		public boolean visitEnd() throws IOException {
			check();
			tree.visitEnd();
			checkContent();
			finishedVisitPassCount++;

			if (finishedVisitPassCount == visitPassCountToFinish) {
				return true;
			}

			oldTrees[finishedVisitPassCount - 1] = new MemoryMappingTree(tree);
			tree = new MemoryMappingTree();
			return false;
		}

		private void check() {
			assertTrue(finishedVisitPassCount < visitPassCountToFinish);
		}

		/**
		 * Ensures every visit pass contains the same content.
		 */
		private void checkContent() throws IOException {
			MappingTreeView subTree, supTree;
			MappingFormat supFormat = null;

			if (finishedVisitPassCount == 0) {
				if (this.supTree == null) return;
				supTree = this.supTree;
			} else {
				supTree = oldTrees[finishedVisitPassCount - 1];
				if (finishedVisitPassCount > 1) supFormat = subFormat;
			}

			subTree = tree;
			subTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(supTree, supFormat, subFormat)));
			supTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(subTree, subFormat, supFormat)));
		}

		private final int visitPassCountToFinish;
		private final boolean setFlag;
		private final MappingTreeView supTree;
		private final MappingFormat subFormat;
		private final MappingTree[] oldTrees;
		private int finishedVisitPassCount;
		private MemoryMappingTree tree;
	}
}

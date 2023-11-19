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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.format.MappingFormat;

public class VisitEndTest {
	private static Set<Path> dirs = new HashSet<>();

	@BeforeAll
	public static void setup() throws Exception {
		dirs.add(TestHelper.MappingDirs.DETECTION);
		dirs.add(TestHelper.MappingDirs.VALID);
		dirs.add(TestHelper.MappingDirs.VALID_WITH_HOLES);
	}

	@Test
	public void testVisitEnd() throws Exception {
		for (MappingFormat format : MappingFormat.values()) {
			String filename = TestHelper.getFileName(format);
			if (filename == null) continue;

			for (Path dir : dirs) {
				MappingReader.read(dir.resolve(filename), format, new VisitEndTestVisitor(1, true));
				MappingReader.read(dir.resolve(filename), format, new VisitEndTestVisitor(1, false));

				VisitEndTestVisitor threePassVisitor = new VisitEndTestVisitor(2, true);
				MappingReader.read(dir.resolve(filename), format, threePassVisitor);
				assertTrue(threePassVisitor.finishedVisitPassCount == threePassVisitor.visitPassCountToFinish);

				threePassVisitor = new VisitEndTestVisitor(2, false);

				try {
					MappingReader.read(dir.resolve(filename), format, threePassVisitor);
				} catch (Exception e) {
					continue; // Reader doesn't support multiple passes without NEEDS_MULTIPLE_PASSES
				}

				// Reader didn't throw an exception, make sure it actually behaved as expected
				assertTrue(threePassVisitor.finishedVisitPassCount == threePassVisitor.visitPassCountToFinish);
			}
		}
	}

	private static class VisitEndTestVisitor implements MappingVisitor {
		private VisitEndTestVisitor(int visitPassCountToFinish, boolean setFlag) {
			this.visitPassCountToFinish = visitPassCountToFinish;
			this.setFlag = setFlag;
		}

		@Override
		public Set<MappingFlag> getFlags() {
			return setFlag
					? EnumSet.of(MappingFlag.NEEDS_MULTIPLE_PASSES)
					: MappingVisitor.super.getFlags();
		}

		@Override
		public boolean visitHeader() throws IOException {
			check();
			return true;
		}

		@Override
		public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
			check();
		}

		@Override
		public boolean visitContent() throws IOException {
			check();
			return true;
		}

		@Override
		public boolean visitClass(String srcName) throws IOException {
			check();
			return true;
		}

		@Override
		public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
			check();
			return true;
		}

		@Override
		public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
			check();
			return true;
		}

		@Override
		public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
			check();
			return true;
		}

		@Override
		public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
			check();
			return true;
		}

		@Override
		public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
			check();
		}

		@Override
		public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
			check();
		}

		@Override
		public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
			check();
			return true;
		}

		@Override
		public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
			check();
		}

		@Override
		public boolean visitEnd() throws IOException {
			finishedVisitPassCount++;
			return finishedVisitPassCount == visitPassCountToFinish;
		}

		private void check() {
			assertTrue(finishedVisitPassCount < visitPassCountToFinish);
		}

		private final int visitPassCountToFinish;
		private final boolean setFlag;
		private int finishedVisitPassCount;
	}
}

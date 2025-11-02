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

package net.fabricmc.mappingio.test.tests.filtering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.file.Files;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.adapter.NopMappingVisitor;
import net.fabricmc.mappingio.adapter.RedundantDstDataFilter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.test.TestMappings.MappingDir;

public class RedundantDstDataFilterTest {
	@Test
	public void run() {
		for (MappingDir dir : TestMappings.values()) {
			for (MappingFormat format : MappingFormat.values()) {
				assertDoesNotThrow(() -> check(dir, format), "Failed for " + dir + " with " + format);
			}
		}
	}

	private void check(MappingDir dir, MappingFormat format) throws Exception {
		if (!Files.exists(dir.pathFor(format))) {
			return;
		}

		dir.read(format, new RedundantDstDataFilter(new NoRedundantDstDataAsserter()));
	}

	private static class NoRedundantDstDataAsserter extends NopMappingVisitor {
		private NoRedundantDstDataAsserter() {
			super(true);
		}

		@Override
		public boolean visitClass(String srcName) throws IOException {
			this.srcName = srcName;
			return super.visitClass(srcName);
		}

		@Override
		public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
			this.srcName = srcName;
			this.srcDesc = srcDesc;
			return super.visitField(srcName, srcDesc);
		}

		@Override
		public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
			this.srcName = srcName;
			this.srcDesc = srcDesc;
			return super.visitMethod(srcName, srcDesc);
		}

		@Override
		public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
			this.srcName = srcName;
			return super.visitMethodArg(argPosition, lvIndex, srcName);
		}

		@Override
		public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
			this.srcName = srcName;
			return super.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
		}

		@Override
		public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
			assertNotEquals(srcName, name, "Redundant destination name for " + targetKind + " in destination namespace " + namespace + " (" + srcName + " -> " + name + ")");
		}

		@Override
		public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) {
			if (srcDesc == null) {
				return;
			}

			assertNotEquals(srcDesc, desc, "Redundant destination descriptor for " + targetKind + " in destination namespace " + namespace + " (" + srcDesc + " -> " + desc + ")");
		}

		private String srcName;
		private String srcDesc;
	}
}

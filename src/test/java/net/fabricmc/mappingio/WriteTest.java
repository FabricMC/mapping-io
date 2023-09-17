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

package net.fabricmc.mappingio;

import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class WriteTest {
	@TempDir
	private static Path dir;
	private static VisitableMappingTree tree;

	@BeforeAll
	public static void setup() throws Exception {
		tree = new MemoryMappingTree();
		tree.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, Arrays.asList(MappingUtil.NS_TARGET_FALLBACK));

		tree.visitClass("class_1");
		tree.visitDstName(MappedElementKind.CLASS, 0, "RenamedClass1");

		tree.visitField("field_1", "I");
		tree.visitDstName(MappedElementKind.FIELD, 0, "renamedField");

		tree.visitMethod("method_1", "(F)I");
		tree.visitDstName(MappedElementKind.METHOD, 0, "renamedMethod");

		tree.visitMethodArg(0, 0, "param_1");
		tree.visitDstName(MappedElementKind.METHOD_ARG, 0, "renamedParameter");

		tree.visitMethodVar(0, 0, 0, 0, "param_1");
		tree.visitDstName(MappedElementKind.METHOD_VAR, 0, "renamedVariable");

		tree.visitClass("class_2");
		tree.visitDstName(MappedElementKind.CLASS, 0, "RenamedClass2");
	}

	@Test
	public void enigmaFile() throws Exception {
		write(MappingFormat.ENIGMA_FILE);
	}

	@Test
	public void enigmaDirectory() throws Exception {
		write(MappingFormat.ENIGMA_DIR);
	}

	@Test
	public void tinyFile() throws Exception {
		write(MappingFormat.TINY_FILE);
	}

	@Test
	public void tinyV2File() throws Exception {
		write(MappingFormat.TINY_2_FILE);
	}

	private void write(MappingFormat format) throws Exception {
		TestHelper.writeToDir(tree, format, dir);
	}
}

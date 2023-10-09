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

package net.fabricmc.mappingio.write;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class WriteTest {
	@TempDir
	private static Path dir;
	private static VisitableMappingTree tree;
	private static VisitableMappingTree treeWithHoles;

	@BeforeAll
	public static void setup() throws Exception {
		tree = TestHelper.createTestTree();
		treeWithHoles = TestHelper.createTestTreeWithHoles();
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

	@Test
	public void recafSimpleFile() throws Exception {
		write(MappingFormat.RECAF_SIMPLE_FILE);
	}

	private void write(MappingFormat format) throws Exception {
		TestHelper.writeToDir(tree, format, dir);
		TestHelper.writeToDir(treeWithHoles, format, dir);
	}
}

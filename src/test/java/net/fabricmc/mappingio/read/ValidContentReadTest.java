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

package net.fabricmc.mappingio.read;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.SubsetAssertingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class ValidContentReadTest {
	private static MappingTree testTree;
	private static MappingTree testTreeWithHoles;

	@BeforeAll
	public static void setup() throws Exception {
		testTree = TestHelper.createTestTree();
		testTreeWithHoles = TestHelper.createTestTreeWithHoles();
	}

	@Test
	public void enigmaFile() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void enigmaDirectory() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_DIR;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void tinyFile() throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void tinyV2File() throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void srgFile() throws Exception {
		MappingFormat format = MappingFormat.SRG_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void xsrgFile() throws Exception {
		MappingFormat format = MappingFormat.XSRG_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void csrgFile() throws Exception {
		MappingFormat format = MappingFormat.CSRG_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void tsrgFile() throws Exception {
		MappingFormat format = MappingFormat.TSRG_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void tsrg2File() throws Exception {
		MappingFormat format = MappingFormat.TSRG_2_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void proguardFile() throws Exception {
		MappingFormat format = MappingFormat.PROGUARD_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void migrationMapFile() throws Exception {
		MappingFormat format = MappingFormat.INTELLIJ_MIGRATION_MAP_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	private VisitableMappingTree checkDefault(MappingFormat format) throws Exception {
		VisitableMappingTree tree = new MemoryMappingTree();
		MappingReader.read(TestHelper.MappingDirs.VALID.resolve(TestHelper.getFileName(format)), format, tree);

		assertSubset(tree, format, testTree, null);
		assertSubset(testTree, null, tree, format);

		return tree;
	}

	private VisitableMappingTree checkHoles(MappingFormat format) throws Exception {
		VisitableMappingTree tree = new MemoryMappingTree();
		MappingReader.read(TestHelper.MappingDirs.VALID_WITH_HOLES.resolve(TestHelper.getFileName(format)), format, tree);

		assertSubset(tree, format, testTreeWithHoles, null);
		assertSubset(testTreeWithHoles, null, tree, format);

		return tree;
	}

	private void assertSubset(MappingTree subTree, @Nullable MappingFormat subFormat, MappingTree supTree, @Nullable MappingFormat supFormat) throws Exception {
		subTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(supTree, supFormat, subFormat)));
	}
}

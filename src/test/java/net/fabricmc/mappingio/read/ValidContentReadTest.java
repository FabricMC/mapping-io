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

import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.SubsetAssertingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.VisitOrderVerifyingVisitor;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTreeView;
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
	public void jamFile() throws Exception {
		MappingFormat format = MappingFormat.JAM_FILE;
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
	public void tsrgV2File() throws Exception {
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
	public void recafSimpleFile() throws Exception {
		MappingFormat format = MappingFormat.RECAF_SIMPLE_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	@Test
	public void jobfFile() throws Exception {
		MappingFormat format = MappingFormat.JOBF_FILE;
		checkDefault(format);
		checkHoles(format);
	}

	private void checkDefault(MappingFormat format) throws Exception {
		Path path = TestHelper.MappingDirs.VALID.resolve(TestHelper.getFileName(format));

		VisitableMappingTree tree = new MemoryMappingTree();
		MappingReader.read(path, format, new VisitOrderVerifyingVisitor(tree));
		assertEqual(format, tree, testTree);

		tree = new MemoryMappingTree();
		MappingReader.read(path, format,
				new MappingSourceNsSwitch(
						new VisitOrderVerifyingVisitor(
								new MappingSourceNsSwitch(
										new VisitOrderVerifyingVisitor(tree),
										testTree.getSrcNamespace())),
						testTree.getDstNamespaces().get(0)));
		assertEqual(format, tree, testTree);
	}

	private void checkHoles(MappingFormat format) throws Exception {
		Path path = TestHelper.MappingDirs.VALID_WITH_HOLES.resolve(TestHelper.getFileName(format));

		VisitableMappingTree tree = new MemoryMappingTree();
		MappingReader.read(path, format, new VisitOrderVerifyingVisitor(tree));
		assertEqual(format, tree, testTreeWithHoles);
	}

	private void assertEqual(MappingFormat format, MappingTreeView tree, MappingTreeView referenceTree) throws Exception {
		assertSubset(tree, format, referenceTree, null);
		assertSubset(referenceTree, null, tree, format);
	}

	private void assertSubset(MappingTreeView subTree, @Nullable MappingFormat subFormat, MappingTreeView supTree, @Nullable MappingFormat supFormat) throws Exception {
		subTree.accept(
				new VisitOrderVerifyingVisitor(
						new FlatAsRegularMappingVisitor(
								new SubsetAssertingVisitor(supTree, supFormat, subFormat))));
	}
}

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
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.SubsetAssertingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.VisitOrderVerifyingVisitor;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class ValidContentReadTest {
	@Test
	public void enigmaFile() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
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
		checkRepeated(format, true);
	}

	@Test
	public void tinyV2File() throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true); // TODO: The Tiny v2 spec disallows repeated elements, there should at least be a warning
	}

	@Test
	public void srgFile() throws Exception {
		MappingFormat format = MappingFormat.SRG_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void xsrgFile() throws Exception {
		MappingFormat format = MappingFormat.XSRG_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void jamFile() throws Exception {
		MappingFormat format = MappingFormat.JAM_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void csrgFile() throws Exception {
		MappingFormat format = MappingFormat.CSRG_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void tsrgFile() throws Exception {
		MappingFormat format = MappingFormat.TSRG_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void tsrgV2File() throws Exception {
		MappingFormat format = MappingFormat.TSRG_2_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void proguardFile() throws Exception {
		MappingFormat format = MappingFormat.PROGUARD_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void migrationMapFile() throws Exception {
		MappingFormat format = MappingFormat.INTELLIJ_MIGRATION_MAP_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void recafSimpleFile() throws Exception {
		MappingFormat format = MappingFormat.RECAF_SIMPLE_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	@Test
	public void jobfFile() throws Exception {
		MappingFormat format = MappingFormat.JOBF_FILE;
		checkDefault(format);
		checkHoles(format);
		checkRepeated(format, true);
	}

	private void checkDefault(MappingFormat format) throws Exception {
		Path path = TestHelper.MappingDirs.VALID.resolve(TestHelper.getFileName(format));

		VisitableMappingTree referenceTree = TestHelper.acceptTestMappings(new MemoryMappingTree());
		VisitableMappingTree tree = new MemoryMappingTree();
		boolean allowConsecutiveDuplicateElementVisits = false;

		MappingReader.read(path, format, new VisitOrderVerifyingVisitor(tree, allowConsecutiveDuplicateElementVisits));
		assertEqual(tree, format, referenceTree, allowConsecutiveDuplicateElementVisits);

		tree = new MemoryMappingTree();
		MappingReader.read(path, format,
				new MappingSourceNsSwitch(
						new VisitOrderVerifyingVisitor(
								new MappingSourceNsSwitch(
										new VisitOrderVerifyingVisitor(tree, allowConsecutiveDuplicateElementVisits),
										referenceTree.getSrcNamespace()),
								allowConsecutiveDuplicateElementVisits),
						referenceTree.getDstNamespaces().get(0)));
		assertEqual(tree, format, referenceTree, allowConsecutiveDuplicateElementVisits);
	}

	private void checkRepeated(MappingFormat format, boolean allowConsecutiveDuplicateElementVisits) throws Exception {
		Path path = TestHelper.MappingDirs.REPEATED_ELEMENTS.resolve(TestHelper.getFileName(format));

		VisitableMappingTree referenceTree = TestHelper.acceptTestMappingsWithRepeats(new MemoryMappingTree(), true, true);
		VisitableMappingTree tree = new MemoryMappingTree();

		MappingReader.read(path, format, new VisitOrderVerifyingVisitor(tree, allowConsecutiveDuplicateElementVisits));
		assertEqual(tree, format, referenceTree, allowConsecutiveDuplicateElementVisits);

		tree = new MemoryMappingTree();
		MappingReader.read(path, format,
				new MappingSourceNsSwitch(
						new VisitOrderVerifyingVisitor(
								new MappingSourceNsSwitch(
										new VisitOrderVerifyingVisitor(tree, allowConsecutiveDuplicateElementVisits),
										referenceTree.getSrcNamespace()),
								allowConsecutiveDuplicateElementVisits),
						referenceTree.getDstNamespaces().get(0)));
		assertEqual(tree, format, referenceTree, allowConsecutiveDuplicateElementVisits);
	}

	private void checkHoles(MappingFormat format) throws Exception {
		Path path = TestHelper.MappingDirs.VALID_WITH_HOLES.resolve(TestHelper.getFileName(format));

		VisitableMappingTree referenceTree = TestHelper.acceptTestMappingsWithHoles(new MemoryMappingTree());
		VisitableMappingTree tree = new MemoryMappingTree();
		boolean allowConsecutiveDuplicateElementVisits = false;

		MappingReader.read(path, format, new VisitOrderVerifyingVisitor(tree, allowConsecutiveDuplicateElementVisits));
		assertEqual(tree, format, referenceTree, allowConsecutiveDuplicateElementVisits);
	}

	private void assertEqual(MappingTreeView tree, MappingFormat format, MappingTreeView referenceTree, boolean allowConsecutiveDuplicateElementVisits) throws Exception {
		assertSubset(tree, format, referenceTree, null, allowConsecutiveDuplicateElementVisits);
		assertSubset(referenceTree, null, tree, format, allowConsecutiveDuplicateElementVisits);
	}

	private void assertSubset(MappingTreeView subTree, @Nullable MappingFormat subFormat, MappingTreeView supTree, @Nullable MappingFormat supFormat, boolean allowConsecutiveDuplicateElementVisits) throws Exception {
		subTree.accept(
				new VisitOrderVerifyingVisitor(
						new FlatAsRegularMappingVisitor(
								new SubsetAssertingVisitor(supTree, supFormat, subFormat)),
						allowConsecutiveDuplicateElementVisits));
	}
}

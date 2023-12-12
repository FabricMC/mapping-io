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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.neoforged.srgutils.IMappingFile;
import net.neoforged.srgutils.INamedMappingFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.SubsetAssertingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class WriteTest {
	@TempDir
	private static Path dir;
	private static MappingTreeView validTree;
	private static Map<String, String> treeNsAltMap = new HashMap<>();
	private static MappingTreeView validWithHolesTree;
	private static Map<String, String> treeWithHolesNsAltMap = new HashMap<>();

	@BeforeAll
	public static void setup() throws Exception {
		validTree = TestHelper.createTestTree();
		treeNsAltMap.put(validTree.getDstNamespaces().get(0), validTree.getSrcNamespace());
		treeNsAltMap.put(validTree.getDstNamespaces().get(1), validTree.getSrcNamespace());

		validWithHolesTree = TestHelper.createTestTreeWithHoles();
		treeWithHolesNsAltMap.put(validWithHolesTree.getDstNamespaces().get(0), validWithHolesTree.getSrcNamespace());
		treeWithHolesNsAltMap.put(validWithHolesTree.getDstNamespaces().get(1), validWithHolesTree.getSrcNamespace());
	}

	@Test
	public void enigmaFile() throws Exception {
		check(MappingFormat.ENIGMA_FILE);
	}

	@Test
	public void enigmaDirectory() throws Exception {
		check(MappingFormat.ENIGMA_DIR);
	}

	@Test
	public void tinyFile() throws Exception {
		check(MappingFormat.TINY_FILE);
	}

	@Test
	public void tinyV2File() throws Exception {
		check(MappingFormat.TINY_2_FILE);
	}

	@Test
	public void srgFile() throws Exception {
		check(MappingFormat.SRG_FILE);
	}

	@Test
	public void xsrgFile() throws Exception {
		check(MappingFormat.XSRG_FILE);
	}

	@Test
	public void proguardFile() throws Exception {
		check(MappingFormat.PROGUARD_FILE);
	}

	private void check(MappingFormat format) throws Exception {
		Path path = TestHelper.writeToDir(validTree, dir, format);
		readWithMio(validTree, path, format);
		readWithLorenz(path, format);
		readWithSrgUtils(validTree, format, treeNsAltMap);

		path = TestHelper.writeToDir(validWithHolesTree, dir, format);
		readWithMio(validWithHolesTree, path, format);
		readWithLorenz(path, format);
		readWithSrgUtils(validWithHolesTree, format, treeWithHolesNsAltMap);
	}

	private void readWithMio(MappingTreeView origTree, Path outputPath, MappingFormat outputFormat) throws Exception {
		VisitableMappingTree writtenTree = new MemoryMappingTree();
		MappingReader.read(outputPath, outputFormat, writtenTree);

		writtenTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(origTree, null, outputFormat)));
		origTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(writtenTree, outputFormat, null)));
	}

	private void readWithLorenz(Path path, MappingFormat format) throws Exception {
		org.cadixdev.lorenz.io.MappingFormat lorenzFormat = TestHelper.toLorenzFormat(format);
		if (lorenzFormat == null) return;
		lorenzFormat.read(path);
	}

	private void readWithSrgUtils(MappingTreeView tree, MappingFormat format, Map<String, String> nsAltMap) throws Exception {
		IMappingFile.Format srgUtilsFormat = TestHelper.toSrgUtilsFormat(format);
		if (srgUtilsFormat == null) return;

		// SrgUtils can't handle empty dst names
		VisitableMappingTree dstNsCompTree = new MemoryMappingTree();
		tree.accept(new MappingNsCompleter(new ForwardingMappingVisitor(dstNsCompTree) {
			@Override
			public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
				// SrgUtil's Tiny v2 reader crashes on var sub-elements
				return !(format == MappingFormat.TINY_2_FILE && targetKind == MappedElementKind.METHOD_VAR);
			}
		}, nsAltMap));

		Path path = TestHelper.writeToDir(dstNsCompTree, dir, format);
		INamedMappingFile.load(path.toFile());
	}
}

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

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class WriteTest {
	@TempDir
	private static Path dir;
	private static VisitableMappingTree tree;
	private static Map<String, String> treeNsAltMap = new HashMap<>();
	private static VisitableMappingTree treeWithHoles;
	private static Map<String, String> treeWithHolesNsAltMap = new HashMap<>();

	@BeforeAll
	public static void setup() throws Exception {
		tree = TestHelper.createTestTree();
		treeNsAltMap.put(tree.getDstNamespaces().get(0), tree.getSrcNamespace());
		treeNsAltMap.put(tree.getDstNamespaces().get(1), tree.getSrcNamespace());

		treeWithHoles = TestHelper.createTestTreeWithHoles();
		treeWithHolesNsAltMap.put(treeWithHoles.getDstNamespaces().get(0), treeWithHoles.getSrcNamespace());
		treeWithHolesNsAltMap.put(treeWithHoles.getDstNamespaces().get(1), treeWithHoles.getSrcNamespace());
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

	private void check(MappingFormat format) throws Exception {
		Path path = TestHelper.writeToDir(tree, format, dir);
		readWithLorenz(path, format);
		readWithSrgUtils(tree, format, treeNsAltMap);

		path = TestHelper.writeToDir(treeWithHoles, format, dir);
		readWithLorenz(path, format);
		readWithSrgUtils(treeWithHoles, format, treeWithHolesNsAltMap);
	}

	private void readWithLorenz(Path path, MappingFormat format) throws Exception {
		org.cadixdev.lorenz.io.MappingFormat lorenzFormat = TestHelper.toLorenzFormat(format);
		if (lorenzFormat == null) return;
		lorenzFormat.read(path);
	}

	private void readWithSrgUtils(MappingTree tree, MappingFormat format, Map<String, String> nsAltMap) throws Exception {
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

		Path path = TestHelper.writeToDir(dstNsCompTree, format, dir);
		INamedMappingFile.load(path.toFile());
	}
}

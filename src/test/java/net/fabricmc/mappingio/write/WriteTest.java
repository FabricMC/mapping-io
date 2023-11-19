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

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.SubsetAssertingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class WriteTest {
	@TempDir
	private static Path dir;
	private static MappingTreeView validTree;
	private static MappingTreeView validWithHolesTree;

	@BeforeAll
	public static void setup() throws Exception {
		validTree = TestHelper.createTestTree();
		validWithHolesTree = TestHelper.createTestTreeWithHoles();
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
	public void tsrgFile() throws Exception {
		check(MappingFormat.TSRG_FILE);
	}

	@Test
	public void tsrg2File() throws Exception {
		check(MappingFormat.TSRG_2_FILE);
	}

	@Test
	public void proguardFile() throws Exception {
		check(MappingFormat.PROGUARD_FILE);
	}

	private void check(MappingFormat format) throws Exception {
		dogfood(validTree, dir, format);
		dogfood(validWithHolesTree, dir, format);
	}

	private void dogfood(MappingTreeView origTree, Path outputPath, MappingFormat outputFormat) throws Exception {
		outputPath = TestHelper.writeToDir(origTree, dir, outputFormat);
		VisitableMappingTree writtenTree = new MemoryMappingTree();

		MappingReader.read(outputPath, outputFormat, writtenTree);

		writtenTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(origTree, null, outputFormat)));
		origTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(writtenTree, outputFormat, null)));
	}
}

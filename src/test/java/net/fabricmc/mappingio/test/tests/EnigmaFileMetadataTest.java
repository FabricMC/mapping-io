/*
 * Copyright (c) 2026 FabricMC
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

package net.fabricmc.mappingio.test.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.enigma.EnigmaFileWriter;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class EnigmaFileMetadataTest {
	@Test
	void writeMetadataOnlyFile() throws Exception {
		MemoryMappingTree tree = new MemoryMappingTree();
		EXAMPLE_METADATA.forEach(tree::visitMetadata);
		StringWriter writer = new StringWriter();
		tree.accept(new EnigmaFileWriter(writer));
		assertEquals(METADATA_ONLY_FILE, writer.toString());
	}

	@Test
	void writeMetadataAndClasses() throws Exception {
		MemoryMappingTree tree = new MemoryMappingTree();

		tree.visitHeader();
		tree.visitNamespaces("a", Collections.singletonList("b"));
		tree.visitContent();
		tree.visitClass("Foo");
		tree.visitDstName(MappedElementKind.CLASS, 0, "Foo2");
		tree.visitClass("Bar");
		tree.visitDstName(MappedElementKind.CLASS, 0, "Bar2");
		EXAMPLE_METADATA.forEach(tree::visitMetadata);

		StringWriter writer = new StringWriter();
		tree.accept(new EnigmaFileWriter(writer));
		assertEquals(METADATA_AND_CLASS_FILE_SORTED, writer.toString());
	}

	@Test
	void readMetadataOnlyFile() throws Exception {
		MemoryMappingTree tree = new MemoryMappingTree();
		MappingReader.read(new StringReader(METADATA_ONLY_FILE), MappingFormat.ENIGMA_FILE, tree);
		assertEquals(EXAMPLE_METADATA, getMetadataMap(tree));
	}

	@Test
	void readMetadataInterspersedWithClasses() throws Exception {
		MemoryMappingTree tree = new MemoryMappingTree();
		MappingReader.read(new StringReader(METADATA_AND_CLASS_FILE), MappingFormat.ENIGMA_FILE, tree);
		assertEquals(EXAMPLE_METADATA, getMetadataMap(tree));
	}

	@Test
	void readMetadataWithoutKey() {
		MemoryMappingTree tree = new MemoryMappingTree();
		assertThrows(IOException.class, () -> MappingReader.read(new StringReader("METADATA"), MappingFormat.ENIGMA_FILE, tree));
	}

	private static Map<String, @Nullable String> getMetadataMap(MappingTreeView tree) {
		Map<String, @Nullable String> map = new HashMap<>();

		for (MappingTreeView.MetadataEntryView entry : tree.getMetadata()) {
			map.put(entry.getKey(), entry.getValue());
		}

		return map;
	}

	private static final String METADATA_ONLY_FILE = "METADATA key-only\nMETADATA key-and-value something\nMETADATA key-and-empty-value \nMETADATA key-and-value-with-special-chars Hello, world!\\nAnother line\n";
	private static final String METADATA_AND_CLASS_FILE = "CLASS Foo Foo2\nMETADATA key-only\nCLASS Bar Bar2\nMETADATA key-and-value something\nMETADATA key-and-empty-value \nMETADATA key-and-value-with-special-chars Hello, world!\\nAnother line\n";
	private static final String METADATA_AND_CLASS_FILE_SORTED = "METADATA key-only\nMETADATA key-and-value something\nMETADATA key-and-empty-value \nMETADATA key-and-value-with-special-chars Hello, world!\\nAnother line\nCLASS Foo Foo2\nCLASS Bar Bar2\n";
	private static final Map<String, String> EXAMPLE_METADATA = new LinkedHashMap<>();

	static {
		EXAMPLE_METADATA.put("key-only", null);
		EXAMPLE_METADATA.put("key-and-value", "something");
		EXAMPLE_METADATA.put("key-and-empty-value", "");
		EXAMPLE_METADATA.put("key-and-value-with-special-chars", "Hello, world!\nAnother line");
	}
}

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

package net.fabricmc.mappingio.test.tests.writing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.format.enigma.EnigmaFileReader;
import net.fabricmc.mappingio.format.enigma.EnigmaFileWriter;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class EnigmaInnerClassesRoundtripTest {
	@Test
	public void missingNestHost() throws Exception {
		String mappingContents = "CLASS innerclass outerclass$1\n";

		MemoryMappingTree tree = new MemoryMappingTree();

		assertDoesNotThrow(() -> EnigmaFileReader.read(new StringReader(mappingContents), tree), "Couldn't read mapping:\n" + mappingContents);

		ClassMapping mappingBySrc = tree.getClass("innerclass");
		ClassMapping mappingByDst = tree.getClass("outerclass$1", tree.getMaxNamespaceId() - 1);
		ClassMapping outerByDst = tree.getClass("outerclass", tree.getMaxNamespaceId() - 1);

		assertNotNull(mappingBySrc, "Cannot find class by source name");
		assertNotNull(mappingByDst, "Cannot find class by destination name");
		assertNull(outerByDst);
		assertEquals(mappingBySrc, mappingByDst);
		assertEquals("innerclass", mappingBySrc.getSrcName());
		assertEquals("outerclass$1", mappingBySrc.getDstName(tree.getMaxNamespaceId() - 1));

		StringWriter sw = new StringWriter();
		try (EnigmaFileWriter writer = new EnigmaFileWriter(sw)) {
			tree.accept(writer);
		}

		assertEquals(mappingContents, sw.toString());
	}
}

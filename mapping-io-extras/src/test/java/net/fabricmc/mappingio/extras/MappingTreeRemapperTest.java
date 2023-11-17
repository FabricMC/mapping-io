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

package net.fabricmc.mappingio.extras;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.tree.MappingTree;

public class MappingTreeRemapperTest {
	private static MappingTree mappingTree;
	private static MappingTreeRemapper remapper;

	@BeforeAll
	public static void setup() {
		mappingTree = TestHelper.createTestTree();
		remapper = new MappingTreeRemapper(mappingTree, "source", "target");
	}

	@Test
	public void testInvalidNamespaces() {
		assertThrows(IllegalArgumentException.class, () -> new MappingTreeRemapper(mappingTree, "unknown", "target"),
				"must throw on missing source namespace");
		assertThrows(IllegalArgumentException.class, () -> new MappingTreeRemapper(mappingTree, "source", "unknown"),
				"must throw on missing target namespace");
	}

	@Test
	public void testMapClass() {
		assertEquals("class1Ns0Rename", remapper.map("class_1"));
	}

	@Test
	public void testMapMethod() {
		assertEquals("method1Ns0Rename", remapper.mapMethodName("class_1", "method_1", "()I"));
	}

	@Test
	public void testMapField() {
		assertEquals("field1Ns0Rename", remapper.mapFieldName("class_1", "field_1", "I"));
	}

	@Test
	public void testMapRecordComponent() {
		// Record components are remapped as fields.
		assertEquals("field1Ns0Rename", remapper.mapRecordComponentName("class_1", "field_1", "I"));
	}

	@Test
	public void testMapDesc() {
		assertEquals("Lclass3Ns0Rename;", remapper.mapDesc("Lclass_3;"));
		assertEquals("()Lclass1Ns0Rename;", remapper.mapMethodDesc("()Lclass_1;"));
	}

	@Test
	public void testMapType() {
		Type fieldType = Type.getType("Lclass_3;");
		Type methodType = Type.getMethodType("()Lclass_1;");
		assertEquals(Type.getType("Lclass3Ns0Rename;"), remapper.mapValue(fieldType));
		assertEquals(Type.getMethodType("()Lclass1Ns0Rename;"), remapper.mapValue(methodType));
	}
}

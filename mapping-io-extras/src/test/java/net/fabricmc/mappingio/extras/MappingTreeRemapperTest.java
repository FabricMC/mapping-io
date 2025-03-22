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
import static net.fabricmc.mappingio.test.TestUtil.createTree;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;

public class MappingTreeRemapperTest {
	private static final String cls1SrcName = "class_1";
	private static final String cls3SrcName = "class_3";
	private static final String fld1SrcName = "field_1";
	private static final String mth1SrcName = "method_1";
	private static String srcNs;
	private static String dstNs;
	private static String cls1DstName;
	private static String cls3DstName;
	private static String fld1SrcDesc;
	private static String fld1DstName;
	private static String mth1SrcDesc;
	private static String mth1DstName;
	private static MappingTree mappingTree;
	private static MappingTreeRemapper remapper;

	@BeforeAll
	public static void setup() throws IOException {
		mappingTree = TestMappings.generateValid(createTree());
		srcNs = mappingTree.getSrcNamespace();
		dstNs = mappingTree.getDstNamespaces().get(0);

		remapper = new MappingTreeRemapper(mappingTree, srcNs, dstNs);

		ClassMapping cls1 = mappingTree.getClass(cls1SrcName);
		ClassMapping cls3 = mappingTree.getClass(cls3SrcName);
		cls1DstName = cls1.getDstName(0);
		cls3DstName = cls3.getDstName(0);

		FieldMapping fld1 = cls1.getField(fld1SrcName, null);
		fld1SrcDesc = fld1.getSrcDesc();
		fld1DstName = fld1.getDstName(0);

		MethodMapping mth1 = cls1.getMethod(mth1SrcName, null);
		mth1SrcDesc = mth1.getSrcDesc();
		mth1DstName = mth1.getDstName(0);
	}

	@Test
	public void testInvalidNamespaces() {
		assertThrows(IllegalArgumentException.class, () -> new MappingTreeRemapper(mappingTree, "unknown", dstNs),
				"must throw on missing source namespace");
		assertThrows(IllegalArgumentException.class, () -> new MappingTreeRemapper(mappingTree, srcNs, "unknown"),
				"must throw on missing target namespace");
	}

	@Test
	public void testMapClass() {
		assertEquals(cls1DstName, remapper.map(cls1SrcName));
	}

	@Test
	public void testMapMethod() {
		assertEquals(mth1DstName, remapper.mapMethodName(cls1SrcName, mth1SrcName, mth1SrcDesc));
	}

	@Test
	public void testMapField() {
		assertEquals(fld1DstName, remapper.mapFieldName(cls1SrcName, fld1SrcName, fld1SrcDesc));
	}

	@Test
	public void testMapRecordComponent() {
		// Record components are remapped as fields.
		assertEquals(fld1DstName, remapper.mapRecordComponentName(cls1SrcName, fld1SrcName, fld1SrcDesc));
	}

	@Test
	public void testMapDesc() {
		assertEquals(clsDesc(cls1DstName), remapper.mapDesc(clsDesc(cls1SrcName)));
		assertEquals(mthDesc(cls3DstName), remapper.mapMethodDesc(mthDesc(cls3SrcName)));
	}

	@Test
	public void testMapType() {
		Type fieldType = Type.getType(clsDesc(cls3SrcName));
		Type methodType = Type.getMethodType(mthDesc(cls1SrcName));

		assertEquals(Type.getType(clsDesc(cls3DstName)), remapper.mapValue(fieldType));
		assertEquals(Type.getMethodType(mthDesc(cls1DstName)), remapper.mapValue(methodType));
	}

	private String clsDesc(String name) {
		return "L" + name + ";";
	}

	private String mthDesc(String name) {
		return "()L" + name + ";";
	}
}

/*
 * Copyright (c) 2024 FabricMC
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

package net.fabricmc.mappingio.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.VisitOrderVerifyingVisitor;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree.ClassEntry;
import net.fabricmc.mappingio.tree.MemoryMappingTree.FieldEntry;

public class MergeTest {
	private static final String srcNs = "ns1";
	private static final String dstNs = "ns2";
	private static final String clsName = "class1Ns1Name";
	private static final String fldName = "field1Ns1Name";
	private static final String fldComment = "field1Ns1Comment";
	private static final String fldDesc = "I";
	private MemoryMappingTree tree;
	private MappingVisitor delegate;

	@BeforeEach
	public void setup() {
		tree = new MemoryMappingTree();
		delegate = new VisitOrderVerifyingVisitor(tree);
	}

	@Test
	public void descHoldingMemberIntoDescless() throws Exception {
		delegate.visitHeader();
		delegate.visitNamespaces(srcNs, Collections.singletonList(dstNs));
		delegate.visitContent();
		delegate.visitClass(clsName);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fldName, null);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitComment(MappedElementKind.FIELD, fldComment);
		delegate.visitEnd();

		ClassMapping cls = tree.getClass(clsName);
		FieldMapping fld = cls.addField(fieldMappingOf(cls, fldName, fldDesc));

		assertEquals(fldComment, fld.getComment());
	}

	@Test
	public void desclessMemberIntoDescHolder() throws Exception {
		delegate.visitHeader();
		delegate.visitNamespaces(srcNs, Collections.singletonList(dstNs));
		delegate.visitContent();
		delegate.visitClass(clsName);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fldName, fldDesc);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitComment(MappedElementKind.FIELD, fldComment);
		delegate.visitEnd();

		ClassMapping cls = tree.getClass(clsName);
		FieldMapping fld = cls.addField(fieldMappingOf(cls, fldName, fldDesc));

		assertEquals(fldDesc, fld.getSrcDesc());
	}

	private FieldMapping fieldMappingOf(ClassMapping cls, String name, String desc) throws Exception {
		return FieldEntry.class
				.getDeclaredConstructor(ClassEntry.class, String.class, String.class)
				.newInstance(cls, name, desc);
	}
}


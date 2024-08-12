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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
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
	private static final String ns1 = "ns1";
	private static final String ns2 = "ns2";
	private static final String ns3 = "ns3";
	private static final String ns4 = "ns4";
	private static final String clsName = "cls";
	private static final String fldName = "fld";
	private static final String fldComment = "fldComment";
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
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
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
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
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

	/**
	 * Test for <a href="https://github.com/FabricMC/mapping-io/issues/68">issue 68</a>.
	 */
	@Test
	public void issue68() throws Exception {
		delegate.visitHeader();
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
		delegate.visitContent();
		delegate.visitEnd();

		delegate.visitHeader();
		delegate.visitNamespaces(ns2, Collections.singletonList(ns3));
		delegate.visitContent();
		delegate.visitClass(clsName);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fldName, fldDesc);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitEnd();
	}

	@Test
	public void disassociatedNamespaces() throws Exception {
		delegate.visitHeader();
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
		delegate.visitContent();
		delegate.visitEnd();

		delegate.visitHeader();
		assertThrows(IllegalArgumentException.class, () -> delegate.visitNamespaces(ns3, Collections.singletonList(ns4)));

		delegate.reset();
		delegate.visitHeader();
		assertThrows(IllegalArgumentException.class, () -> delegate.visitNamespaces(ns4, Collections.singletonList(ns3)));
	}

	@Test
	public void srcNsDuplicatedToDstSide() throws Exception {
		// Uninitialized tree
		delegate.visitHeader();
		assertThrows(IllegalArgumentException.class, () -> delegate.visitNamespaces(ns1, Collections.singletonList(ns1)));

		// Initialized tree with subsequent incorrect visit
		setup();
		delegate.visitHeader();
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
		delegate.visitContent();
		delegate.visitEnd();
		delegate.visitHeader();
		assertThrows(IllegalArgumentException.class, () -> delegate.visitNamespaces(ns2, Collections.singletonList(ns2)));
	}

	@Test
	public void pendingElementsQueue() throws Exception {
		pendingElementsQueue0(false);
		pendingElementsQueue0(true);
	}

	private void pendingElementsQueue0(boolean visitDstNames) throws IOException {
		String cls1Ns1Name = "cls1Ns1Name";
		String cls1Ns2Name = "cls1Ns2Name";
		String fld1Ns1Name = "fld1Ns1Name";
		String fld1Ns2Name = "fld1Ns2Name";

		delegate.visitHeader();
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
		delegate.visitContent();
		delegate.visitEnd();

		delegate.visitHeader();
		delegate.visitNamespaces(ns2, Collections.singletonList(ns1));
		delegate.visitContent();
		delegate.visitClass(cls1Ns2Name);

		if (visitDstNames) {
			delegate.visitDstName(MappedElementKind.CLASS, 0, cls1Ns1Name);
		}

		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fld1Ns2Name, fldDesc);

		if (visitDstNames) {
			delegate.visitDstName(MappedElementKind.FIELD, 0, fld1Ns1Name);
		}

		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitEnd();

		assertEquals(tree.getClass(cls1Ns1Name) != null, visitDstNames);
		assertEquals(tree.getField(cls1Ns1Name, fld1Ns1Name, fldDesc) != null, visitDstNames);
	}

	/*
	 * Destination tree:
	 * ns1                              | ns2
	 * ---------------------------------|----------------
	 * cls1Ns1Name                      | cls1Ns2Name
	 *
	 *
	 * To-be-merged tree:
	 * ns2                              | ns1
	 * ---------------------------------|----------------
	 * cls2Ns2Name                      | cls2Ns1Name
	 *  \-- fldNs2Name : Lcls1Ns2Name;  |  \-- fldNs1Name
	 *
	 *
	 * To-be-merged tree, primitively flipped (e.g. via MappingSourceNsSwitch):
	 * ns1                              | ns2
	 * ---------------------------------|----------------
	 * cls2Ns1Name                      | cls2Ns2Name
	 *  \-- fldNs1Name : Lcls1Ns2Name;  |  \-- fldNs2Name
	 *
	 * fld's ns1 descriptor still references ns2's name of cls1,
	 * since the tree didn't contain any data for cls1.
	 *
	 *
	 * When merged into destination tree:
	 * ns1                              | ns2
	 * ---------------------------------|----------------
	 * cls1Ns1Name                      | cls1Ns2Name
	 * cls2Ns1Name                      | cls2Ns2Name
	 *  \-- fldNs1Name : Lcls1Ns2Name;  |  \-- fldNs2Name
	 *
	 * Incorrect, there's now no way of knowing that fld's descriptor was originally referencing cls1.
	 *
	 *
	 * When merging to-be-merged tree directly into destination tree:
	 * ns1                              | ns2
	 * ---------------------------------|----------------
	 * cls1Ns1Name                      | cls1Ns2Name
	 * cls2Ns1Name                      | cls2Ns2Name
	 *  \-- fldNs1Name : Lcls1Ns1Name;  |  \-- fldNs2Name
	 *
	 * Correct thanks to MemoryMappingTree's advanced merging capabilities.
	 */
	@Test
	public void descriptorCompletion() throws IOException {
		String cls1Ns1Name = "cls1Ns1Name";
		String cls1Ns2Name = "cls1Ns2Name";
		String cls2Ns1Name = "cls2Ns1Name";
		String cls2Ns2Name = "cls2Ns2Name";
		String fldNs1Name = "fldNs1Name";
		String fldNs2Name = "fldNs2Name";
		String fldNs1Desc = "L" + cls1Ns1Name + ";";
		String fldNs2Desc = "L" + cls1Ns2Name + ";";

		delegate.visitHeader();
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
		delegate.visitContent();
		delegate.visitClass(cls1Ns1Name);
		delegate.visitDstName(MappedElementKind.CLASS, 0, cls1Ns2Name);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitEnd();

		delegate.visitHeader();
		delegate.visitNamespaces(ns2, Collections.singletonList(ns1));
		delegate.visitContent();
		delegate.visitClass(cls2Ns2Name);
		delegate.visitDstName(MappedElementKind.CLASS, 0, cls2Ns1Name);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fldNs2Name, fldNs2Desc);
		delegate.visitDstName(MappedElementKind.FIELD, 0, fldNs1Name);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitEnd();

		assertEquals(fldNs1Desc, tree.getField(cls2Ns1Name, fldNs1Name, null).getSrcDesc());
	}
}


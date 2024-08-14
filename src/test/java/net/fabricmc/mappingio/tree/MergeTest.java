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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.SubsetAssertingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.VisitOrderVerifyingVisitor;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree.ClassEntry;
import net.fabricmc.mappingio.tree.MemoryMappingTree.FieldEntry;

public class MergeTest {
	private static final Path dir = TestHelper.MappingDirs.MERGING;
	private static final String ns1 = "ns1";
	private static final String ns2 = "ns2";
	private static final String ns3 = "ns3";
	private static final String ns4 = "ns4";
	private static final String cls1Ns1Name = "cls1Ns1Name";
	private static final String cls1Ns2Name = "cls1Ns2Name";
	private static final String cls2Ns1Name = "cls2Ns1Name";
	private static final String cls2Ns2Name = "cls2Ns2Name";
	private static final String fld1Ns1Name = "fld1Ns1Name";
	private static final String fld1Ns2Name = "fld1Ns2Name";
	private static final String fld1Ns1Desc = "L" + cls1Ns1Name + ";";
	private static final String fld1Ns2Desc = "L" + cls1Ns2Name + ";";
	private static final String fld1Comment = "fld1Comment";
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
		delegate.visitClass(cls1Ns1Name);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fld1Ns1Name, null);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitComment(MappedElementKind.FIELD, fld1Comment);
		delegate.visitEnd();

		ClassMapping cls = tree.getClass(cls1Ns1Name);
		FieldMapping fld = cls.addField(fieldMappingOf(cls, fld1Ns1Name, fld1Ns1Desc));

		assertEquals(fld1Comment, fld.getComment());
	}

	@Test
	public void desclessMemberIntoDescHolder() throws Exception {
		delegate.visitHeader();
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
		delegate.visitContent();
		delegate.visitClass(cls1Ns1Name);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fld1Ns1Name, fld1Ns1Desc);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitComment(MappedElementKind.FIELD, fld1Comment);
		delegate.visitEnd();

		ClassMapping cls = tree.getClass(cls1Ns1Name);
		FieldMapping fld = cls.addField(fieldMappingOf(cls, fld1Ns1Name, fld1Ns1Desc));

		assertEquals(fld1Ns1Desc, fld.getSrcDesc());
	}

	private FieldMapping fieldMappingOf(ClassMapping cls, String name, String desc) throws Exception {
		return FieldEntry.class
				.getDeclaredConstructor(ClassEntry.class, String.class, String.class)
				.newInstance(cls, name, desc);
	}

	@Test
	public void ns1ToNs2ThenNs2ToNs3() throws Exception {
		String cls1Ns3Name = "cls1Ns3Name";

		delegate.visitHeader();
		delegate.visitNamespaces(ns1, Collections.singletonList(ns2));
		delegate.visitContent();
		delegate.visitClass(cls1Ns1Name);
		delegate.visitDstName(MappedElementKind.CLASS, 0, cls1Ns2Name);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitEnd();

		delegate.visitHeader();
		delegate.visitNamespaces(ns2, Arrays.asList(ns3, ns1));
		delegate.visitContent();
		delegate.visitClass(cls1Ns2Name);
		delegate.visitDstName(MappedElementKind.CLASS, 0, cls1Ns3Name);
		delegate.visitElementContent(MappedElementKind.CLASS);
		delegate.visitField(fld1Ns2Name, fld1Ns2Desc);
		delegate.visitDstName(MappedElementKind.FIELD, 1, fld1Ns1Name);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitEnd();

		ClassMapping cls = tree.getClass(cls1Ns1Name);
		assertNotNull(cls);
		assertEquals(cls1Ns2Name, cls.getDstName(0));
		assertEquals(cls1Ns3Name, cls.getDstName(1));

		FieldMapping fld = cls.getField(fld1Ns1Name, fld1Ns1Desc);
		assertNotNull(fld);
		assertEquals(fld1Ns2Name, fld.getDstName(0));
		assertEquals(null, fld.getDstName(1));
		assertEquals(fld1Ns1Desc, fld.getSrcDesc());
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
		delegate.visitField(fld1Ns2Name, fld1Ns1Desc);

		if (visitDstNames) {
			delegate.visitDstName(MappedElementKind.FIELD, 0, fld1Ns1Name);
		}

		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitEnd();

		assertEquals(tree.getClass(cls1Ns1Name) != null, visitDstNames);
		assertEquals(tree.getField(cls1Ns1Name, fld1Ns1Name, fld1Ns1Desc) != null, visitDstNames);
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
		delegate.visitField(fld1Ns2Name, fld1Ns2Desc);
		delegate.visitDstName(MappedElementKind.FIELD, 0, fld1Ns1Name);
		delegate.visitElementContent(MappedElementKind.FIELD);
		delegate.visitEnd();

		assertEquals(fld1Ns1Desc, tree.getField(cls2Ns1Name, fld1Ns1Name, null).getSrcDesc());
	}

	@Test
	public void diskMappings() throws IOException {
		MappingReader.read(dir.resolve("tree1.tiny"), delegate);
		MappingReader.read(dir.resolve("tree2.tiny"), delegate);

		MemoryMappingTree referenceTree = new MemoryMappingTree();
		MappingReader.read(dir.resolve("tree1+2.tiny"), referenceTree);
		tree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(referenceTree, null, null)));
		referenceTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(tree, null, null)));

		MappingReader.read(dir.resolve("tree3.tiny"), delegate);

		referenceTree = new MemoryMappingTree();
		MappingReader.read(dir.resolve("tree1+2+3.tiny"), referenceTree);
		tree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(referenceTree, null, null)));
		referenceTree.accept(new FlatAsRegularMappingVisitor(new SubsetAssertingVisitor(tree, null, null)));
	}
}


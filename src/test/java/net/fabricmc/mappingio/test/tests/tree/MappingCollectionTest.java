/*
 * Copyright (c) 2025 FabricMC
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

package net.fabricmc.mappingio.test.tests.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class MappingCollectionTest {
	private static final String nsA = "nsA";
	private static final String nsB = "nsB";
	private static final String cls1NsAName = "cls1NsAName";
	private static final String cls1NsBName = "cls1NsBName";
	private static final String cls2NsAName = "cls2NsAName";
	private static final String fld1NsAName = "fld1NsAName";
	private static final String fld1NsBName = "fld1NsBName";
	private static final String fld2NsAName = "fld2NsAName";
	private static final String fld1NsADesc = "L" + cls1NsAName + ";";
	private static final String fld2NsADesc = "L" + cls2NsAName + ";";
	private static final String fld1Comment = "fld1Comment";
	private MemoryMappingTree tree1, tree2, emptyTree;

	@BeforeEach
	public void setup() throws Exception {
		tree1 = new MemoryMappingTree();
		tree2 = new MemoryMappingTree();
		emptyTree = new MemoryMappingTree();

		for (int i = 1; i <= 3; i++) {
			VisitableMappingTree tree = i == 1 ? tree1 : i == 2 ? tree2 : emptyTree;

			if (tree.visitHeader()) {
				tree.visitNamespaces(nsA, Collections.singletonList(nsB));
			}

			if (tree.visitContent()) {
				if (tree != emptyTree && tree.visitClass(cls1NsAName)) {
					tree.visitDstName(MappedElementKind.CLASS, 0, cls1NsBName);

					if (tree.visitElementContent(MappedElementKind.CLASS)) {
						if (tree.visitField(fld1NsAName, fld1NsADesc)) {
							if (tree == tree2) {
								tree.visitDstName(MappedElementKind.FIELD, 0, fld1NsBName);
							}

							if (tree.visitElementContent(MappedElementKind.FIELD) && tree == tree2) {
								tree.visitComment(MappedElementKind.FIELD, fld1Comment);
							}
						}

						if (tree == tree2 && tree.visitField(fld2NsAName, fld2NsADesc)) {
							tree.visitElementContent(MappedElementKind.FIELD);
						}
					}
				}
			}

			tree.visitEnd();
		}
	}

	@Test
	public void emptyCollections() {
		assertTrue(emptyTree.getClasses().isEmpty());

		tree2Cls1().getFields().clear();
		tree2Cls1().getMethods().clear();
		assertTrue(emptyTree.getClasses().add(tree2Cls1()));

		ClassMapping cls = emptyTree.getClass(cls1NsAName);
		assertTrue(cls.getFields().isEmpty());
		assertTrue(cls.getMethods().isEmpty());
	}

	@Test
	public void test() {
		assertTrue(tree1.getClasses().contains(tree1Cls1()));
		assertTrue(tree2.getClasses().contains(tree2Cls1()));
		assertFalse(tree1.getClasses().contains(tree2Cls1()));
		assertTrue(tree1.getClasses().containsCompatible(tree2Cls1()));
		assertFalse(tree2.getClasses().contains(tree1Cls1()));
		assertTrue(tree2.getClasses().containsCompatible(tree1Cls1()));
		assertTrue(tree1Cls1().getFields().contains(tree1Fld1()));
		assertFalse(tree1Cls1().getFields().contains(tree2Fld2()));
		assertFalse(tree2Cls1().getFields().contains(tree1Fld1()));
		assertTrue(tree2Cls1().getFields().containsCompatible(tree1Fld1()));
		assertTrue(tree2Cls1().getFields().contains(tree2Fld2()));
		assertTrue(tree2Cls1().getFields().containsCompatible(tree2Fld2()));

		assertEquals(1, tree1.getClasses().size());
		assertEquals(1, tree2.getClasses().size());
		assertEquals(1, tree1Cls1().getFields().size());
		assertEquals(2, tree2Cls1().getFields().size());
		assertEquals(0, tree1Cls1().getMethods().size());
		assertEquals(0, tree2Cls1().getMethods().size());

		assertNull(tree1Fld1().getComment());
		((ClassMapping) tree1Cls1()).getFields().add(tree2Fld1());
		assertEquals(fld1Comment, tree1Fld1().getComment());

		assertTrue(tree2Cls1().getFields().contains(tree2Fld1()));
		tree2Cls1().getFields().remove(tree2Fld1());
		assertFalse(tree2Cls1().getFields().contains(tree2Fld1()));
		assertEquals(1, tree2Cls1().getFields().size());

		for (int i = 0; i <= 4; i++) {
			switch (i) {
			case 0:
				((ClassMapping) tree1Cls1()).getFields().add(tree2Fld2());
				break;
			case 1:
				((ClassMapping) tree1Cls1()).getFields().addAllViews(tree2Cls1().getFields());
				break;
			case 2:
				((ClassMapping) tree1Cls1()).getFields().addAllViews(Collections.singleton((FieldMapping) tree2Fld2()));
				break;
			case 3:
			case 4:
				((ClassMapping) tree1Cls1()).getFields().addAllViews(Collections.singleton(tree2Fld2()));
				break;
			}

			assertFalse(tree1Cls1().getFields().contains(tree2Fld2()));
			assertTrue(tree1Cls1().getFields().containsCompatible(tree2Fld2()));
			assertEquals(2, tree1Cls1().getFields().size());

			switch (i) {
			case 0:
				assertFalse(tree1Cls1().getFields().remove(tree2Fld2()));
				assertTrue(tree1Cls1().getFields().removeCompatible(tree2Fld2()));
				break;
			case 1:
				assertFalse(tree1Cls1().getFields().removeAll(tree2Cls1().getFields()));
				assertTrue(tree1Cls1().getFields().removeAllCompatible(tree2Cls1().getFields()));
				break;
			case 2:
				assertFalse(tree1Cls1().getFields().removeAll(Collections.singleton(tree2Fld2())));
				assertTrue(tree1Cls1().getFields().removeAllCompatible(Collections.singleton(tree2Fld2())));
				break;
			case 3:
				assertTrue(tree1Cls1().getFields().retainAll(Collections.singleton(tree1Fld1())));
				assertFalse(tree1Cls1().getFields().retainAll(Collections.singleton(tree1Fld1())));
				assertFalse(tree1Cls1().getFields().retainAllCompatible(Collections.singleton(tree1Fld1())));
				break;
			case 4:
				assertTrue(tree1Cls1().getFields().retainAllCompatible(Collections.singleton(tree1Fld1())));
				assertFalse(tree1Cls1().getFields().retainAllCompatible(Collections.singleton(tree1Fld1())));
				assertFalse(tree1Cls1().getFields().retainAll(Collections.singleton(tree1Fld1())));
				break;
			}

			assertFalse(tree1Cls1().getFields().contains(tree2Fld2()));
			assertFalse(tree1Cls1().getFields().containsCompatible(tree2Fld2()));
			assertEquals(1, tree1Cls1().getFields().size());
		}

		tree1Cls1().getFields().clear();
		assertEquals(0, tree1Cls1().getFields().size());

		tree1.getClasses().clear();
		assertEquals(0, tree1.getClasses().size());
	}

	private ClassMappingView tree1Cls1() {
		return tree1.getClass(cls1NsAName);
	}

	private ClassMappingView tree2Cls1() {
		return tree2.getClass(cls1NsAName);
	}

	private ClassMappingView tree1Cls2() {
		return tree1.getClass(cls2NsAName);
	}

	private FieldMappingView tree1Fld1() {
		return tree1Cls1().getField(fld1NsAName, fld1NsADesc);
	}

	private FieldMappingView tree2Fld1() {
		return tree2Cls1().getField(fld1NsAName, fld1NsADesc);
	}

	private FieldMappingView tree2Fld2() {
		return tree2Cls1().getField(fld2NsAName, fld2NsADesc);
	}
}

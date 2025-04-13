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
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.test.visitors.VisitOrderVerifyingVisitor;
import net.fabricmc.mappingio.tree.MappingCollectionView.FieldMappingCollectionView;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodArgMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodVarMapping;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class MappingCollectionTest {
	private static final String nsA = "nsA";
	private static final String nsB = "nsB";
	private static final String cls1NsAName = "cls1NsAName";
	private static final String cls1NsBName = "cls1NsBName";
	private static final String fld1NsAName = "fld1NsAName";
	private static final String fld1NsBName = "fld1NsBName";
	private static final String fld2NsAName = "fld2NsAName";
	private static final String fld1NsADesc = "I";
	private static final String fld2NsADesc = "L" + cls1NsAName + ";";
	private static final String fld1Comment = "fld1Comment";
	private static final String mth1NsAName = "mth1NsAName";
	private static final String mth1NsBName = "mth1NsBName";
	private static final String mth2NsAName = "mth2NsAName";
	private static final String mth1NsADesc = "()I";
	private static final String mth2NsADesc = "()L" + cls1NsAName + ";";
	private static final String arg1NsAName = "arg1NsAName";
	private static final String arg2NsAName = "arg2NsAName";
	private static final int arg1ArgPos = 0;
	private static final int arg1LvIdx = 0;
	private static final int arg2ArgPos = 1;
	private static final int arg2LvIdx = 1;
	private static final String var1NsAName = "var1NsAName";
	private static final String var2NsAName = "var2NsAName";
	private static final int var1LvtIdx = 0;
	private static final int var1LvIdx = 0;
	private static final int var1StartOpIdx = 0;
	private static final int var1EndOpIdx = 1;
	private static final int var2LvtIdx = 1;
	private static final int var2LvIdx = 1;
	private static final int var2StartOpIdx = 2;
	private static final int var2EndOpIdx = 3;
	private MemoryMappingTree tree1, tree2, emptyTree;

	@BeforeEach
	public void setup() throws Exception {
		tree1 = new MemoryMappingTree();
		tree2 = new MemoryMappingTree();
		emptyTree = new MemoryMappingTree();

		for (int i = 1; i <= 3; i++) {
			VisitableMappingTree tree = i == 1 ? tree1 : i == 2 ? tree2 : emptyTree;
			MappingVisitor delegate = new VisitOrderVerifyingVisitor(tree);

			if (delegate.visitHeader()) {
				delegate.visitNamespaces(nsA, Collections.singletonList(nsB));
			}

			if (delegate.visitContent()) {
				if (tree != emptyTree && delegate.visitClass(cls1NsAName)) {
					delegate.visitDstName(MappedElementKind.CLASS, 0, cls1NsBName);

					if (delegate.visitElementContent(MappedElementKind.CLASS)) {
						if (delegate.visitField(fld1NsAName, fld1NsADesc)) {
							if (tree == tree2) {
								delegate.visitDstName(MappedElementKind.FIELD, 0, fld1NsBName);
							}

							if (delegate.visitElementContent(MappedElementKind.FIELD) && tree == tree2) {
								delegate.visitComment(MappedElementKind.FIELD, fld1Comment);
							}
						}

						if (tree == tree2 && delegate.visitField(fld2NsAName, fld2NsADesc)) {
							delegate.visitElementContent(MappedElementKind.FIELD);
						}

						if (delegate.visitMethod(mth1NsAName, mth1NsADesc)) {
							if (tree == tree2) {
								delegate.visitDstName(MappedElementKind.METHOD, 0, mth1NsBName);
							}

							if (delegate.visitElementContent(MappedElementKind.METHOD)) {
								if (delegate.visitMethodArg(arg1ArgPos, arg1LvIdx, arg1NsAName)) {
									delegate.visitElementContent(MappedElementKind.METHOD_ARG);
								}

								if (tree == tree2 && delegate.visitMethodArg(arg2ArgPos, arg2LvIdx, arg2NsAName)) {
									delegate.visitElementContent(MappedElementKind.METHOD_ARG);
								}

								if (delegate.visitMethodVar(var1LvtIdx, var1LvIdx, var1StartOpIdx, var1EndOpIdx, var1NsAName)) {
									delegate.visitElementContent(MappedElementKind.METHOD_VAR);
								}

								if (tree == tree2 && delegate.visitMethodVar(var2LvtIdx, var2LvIdx, var2StartOpIdx, var2EndOpIdx, var2NsAName)) {
									delegate.visitElementContent(MappedElementKind.METHOD_VAR);
								}
							}
						}

						if (tree == tree2 && delegate.visitMethod(mth2NsAName, mth2NsADesc)) {
							delegate.visitElementContent(MappedElementKind.METHOD);
						}
					}
				}
			}

			delegate.visitEnd();
		}
	}

	@Test
	public void isEmptyClear() {
		assertTrue(emptyTree.getClasses().isEmpty());
		emptyTree.getClasses().clear();

		cls1(tree1).getFields().clear();
		cls1(tree1).getMethods().clear();
		assertTrue(emptyTree.getClasses().add(cls1(tree1)));
		assertTrue(cls1(emptyTree).getFields().isEmpty());
		assertTrue(cls1(emptyTree).getMethods().isEmpty());

		mth1(cls1(tree2)).getArgs().clear();
		mth1(cls1(tree2)).getVars().clear();
		assertTrue(cls1(emptyTree).getMethods().add(mth1(cls1(tree2))));
		assertTrue(mth1(cls1(emptyTree)).getArgs().isEmpty());
		assertTrue(mth1(cls1(emptyTree)).getVars().isEmpty());
	}

	@Test
	public void size() {
		// Classes
		assertEquals(1, tree1.getClasses().size());
		assertEquals(1, tree2.getClasses().size());

		// Members
		assertEquals(1, cls1(tree1).getFields().size());
		assertEquals(2, cls1(tree2).getFields().size());
		assertEquals(1, cls1(tree1).getMethods().size());
		assertEquals(2, cls1(tree2).getMethods().size());

		// Locals
		assertEquals(1, mth1(cls1(tree1)).getArgs().size());
		assertEquals(2, mth1(cls1(tree2)).getArgs().size());
		assertEquals(1, mth1(cls1(tree1)).getVars().size());
		assertEquals(2, mth1(cls1(tree2)).getVars().size());
		assertEquals(0, mth2(cls1(tree2)).getArgs().size());
		assertEquals(0, mth2(cls1(tree2)).getVars().size());
	}

	@Test
	public void contains() {
		// Classes
		assertTrue(tree1.getClasses().contains(cls1(tree1)));
		assertTrue(tree2.getClasses().contains(cls1(tree2)));
		assertFalse(tree1.getClasses().contains(cls1(tree2)));
		assertTrue(tree1.getClasses().containsCompatible(cls1(tree2)));
		assertFalse(tree2.getClasses().contains(cls1(tree1)));
		assertTrue(tree2.getClasses().containsCompatible(cls1(tree1)));

		// Fields
		assertTrue(cls1(tree1).getFields().contains(fld1(cls1(tree1))));
		assertFalse(cls1(tree1).getFields().contains(fld2(cls1(tree2))));
		assertFalse(cls1(tree2).getFields().contains(fld1(cls1(tree1))));
		assertTrue(cls1(tree2).getFields().containsCompatible(fld1(cls1(tree1))));
		assertTrue(cls1(tree2).getFields().contains(fld2(cls1(tree2))));
		assertTrue(cls1(tree2).getFields().containsCompatible(fld2(cls1(tree2))));

		// Methods
		assertTrue(cls1(tree1).getMethods().contains(mth1(cls1(tree1))));
		assertFalse(cls1(tree1).getMethods().contains(mth2(cls1(tree2))));
		assertFalse(cls1(tree2).getMethods().contains(mth1(cls1(tree1))));
		assertTrue(cls1(tree2).getMethods().containsCompatible(mth1(cls1(tree1))));
		assertTrue(cls1(tree2).getMethods().contains(mth2(cls1(tree2))));
		assertTrue(cls1(tree2).getMethods().containsCompatible(mth2(cls1(tree2))));

		// Args
		assertTrue(mth1(cls1(tree1)).getArgs().contains(arg1(mth1(cls1(tree1)))));
		assertFalse(mth1(cls1(tree1)).getArgs().contains(arg2(mth1(cls1(tree2)))));
		assertFalse(mth1(cls1(tree2)).getArgs().contains(arg1(mth1(cls1(tree1)))));
		assertTrue(mth1(cls1(tree2)).getArgs().containsCompatible(arg1(mth1(cls1(tree1)))));
		assertTrue(mth1(cls1(tree2)).getArgs().contains(arg2(mth1(cls1(tree2)))));
		assertTrue(mth1(cls1(tree2)).getArgs().containsCompatible(arg2(mth1(cls1(tree2)))));

		// Vars
		assertTrue(mth1(cls1(tree1)).getVars().contains(var1(mth1(cls1(tree1)))));
		assertFalse(mth1(cls1(tree1)).getVars().contains(var2(mth1(cls1(tree2)))));
		assertFalse(mth1(cls1(tree2)).getVars().contains(var1(mth1(cls1(tree1)))));
		assertTrue(mth1(cls1(tree2)).getVars().containsCompatible(var1(mth1(cls1(tree1)))));
		assertTrue(mth1(cls1(tree2)).getVars().contains(var2(mth1(cls1(tree2)))));
		assertTrue(mth1(cls1(tree2)).getVars().containsCompatible(var2(mth1(cls1(tree2)))));
	}

	@Test
	public void merge() {
		assertNull(fld1(cls1(tree1)).getComment());
		cls1(tree1).getFields().add(fld1(cls1(tree2)));
		assertEquals(fld1Comment, fld1(cls1(tree1)).getComment());
	}

	@Test
	public void addRemoveRetain() {
		cls1(tree2).getFields().remove(fld1(cls1(tree2)));
		assertFalse(cls1(tree2).getFields().contains(fld1(cls1(tree2))));
		assertEquals(1, cls1(tree2).getFields().size());

		for (int i = 0; i <= 4; i++) {
			switch (i) {
			case 0:
				cls1(tree1).getFields().add(fld2(cls1(tree2)));
				break;
			case 1:
				cls1(tree1).getFields().addAllViews(cls1(tree2).getFields());
				break;
			case 2:
				cls1(tree1).getFields().addAllViews((FieldMappingCollectionView<? extends FieldMappingView>) cls1(tree2).getFields());
				break;
			case 3:
				cls1(tree1).getFields().addAllViews(Collections.singleton((FieldMapping) fld2(cls1(tree2))));
				break;
			case 4:
				cls1(tree1).getFields().addAllViews(Collections.singleton(fld2(cls1(tree2))));
				break;
			}

			assertFalse(cls1(tree1).getFields().contains(fld2(cls1(tree2))));
			assertTrue(cls1(tree1).getFields().containsCompatible(fld2(cls1(tree2))));
			assertEquals(2, cls1(tree1).getFields().size());

			switch (i) {
			case 0:
				assertFalse(cls1(tree1).getFields().remove(fld2(cls1(tree2))));
				assertTrue(cls1(tree1).getFields().removeCompatible(fld2(cls1(tree2))));
				break;
			case 1:
				assertFalse(cls1(tree1).getFields().removeAll(cls1(tree2).getFields()));
				assertTrue(cls1(tree1).getFields().removeAllCompatible(cls1(tree2).getFields()));
				break;
			case 2:
				assertFalse(cls1(tree1).getFields().removeAll(Collections.singleton(fld2(cls1(tree2)))));
				assertTrue(cls1(tree1).getFields().removeAllCompatible(Collections.singleton(fld2(cls1(tree2)))));
				break;
			case 3:
				assertTrue(cls1(tree1).getFields().retainAll(Collections.singleton(fld1(cls1(tree1)))));
				assertFalse(cls1(tree1).getFields().retainAll(Collections.singleton(fld1(cls1(tree1)))));
				assertFalse(cls1(tree1).getFields().retainAllCompatible(Collections.singleton(fld1(cls1(tree1)))));
				break;
			case 4:
				assertTrue(cls1(tree1).getFields().retainAllCompatible(Collections.singleton(fld1(cls1(tree1)))));
				assertFalse(cls1(tree1).getFields().retainAllCompatible(Collections.singleton(fld1(cls1(tree1)))));
				assertFalse(cls1(tree1).getFields().retainAll(Collections.singleton(fld1(cls1(tree1)))));
				break;
			}

			assertFalse(cls1(tree1).getFields().contains(fld2(cls1(tree2))));
			assertFalse(cls1(tree1).getFields().containsCompatible(fld2(cls1(tree2))));
			assertEquals(1, cls1(tree1).getFields().size());
		}
	}

	private ClassMapping cls1(MemoryMappingTree tree) {
		return tree.getClass(cls1NsAName);
	}

	private FieldMapping fld1(ClassMapping cls) {
		return cls.getField(fld1NsAName, fld1NsADesc);
	}

	private FieldMapping fld2(ClassMapping cls) {
		return cls.getField(fld2NsAName, fld2NsADesc);
	}

	private MethodMapping mth1(ClassMapping cls) {
		return cls.getMethod(mth1NsAName, mth1NsADesc);
	}

	private MethodMapping mth2(ClassMapping cls) {
		return cls.getMethod(mth2NsAName, mth2NsADesc);
	}

	private MethodArgMapping arg1(MethodMapping mth) {
		return mth.getArg(arg1ArgPos, arg1LvIdx, arg1NsAName);
	}

	private MethodArgMapping arg2(MethodMapping mth) {
		return mth.getArg(arg2ArgPos, arg2LvIdx, arg2NsAName);
	}

	private MethodVarMapping var1(MethodMapping mth) {
		return mth.getVar(var1LvtIdx, var1LvIdx, var1StartOpIdx, var1EndOpIdx, var1NsAName);
	}

	private MethodVarMapping var2(MethodMapping mth) {
		return mth.getVar(var2LvtIdx, var2LvIdx, var2StartOpIdx, var2EndOpIdx, var2NsAName);
	}
}

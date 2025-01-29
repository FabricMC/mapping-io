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

package net.fabricmc.mappingio.test.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.NopMappingVisitor;
import net.fabricmc.mappingio.adapter.OuterClassNamePropagator;
import net.fabricmc.mappingio.adapter.SubsetEnforcer;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.test.TestMappings.MappingDir;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class OuterClassNamePropagationTest {
	private static String srcNamespace;
	private static List<String> dstNamespaces;
	private static String invalidNs = "invalid";

	@BeforeAll
	public static void setup() throws IOException {
		MappingTreeView tree = acceptMappings(new MemoryMappingTree());
		srcNamespace = tree.getSrcNamespace();
		dstNamespaces = tree.getDstNamespaces();

		assert !srcNamespace.equals(invalidNs);
		assert !dstNamespaces.contains(invalidNs);
	}

	private static <T extends MappingVisitor> T acceptMappings(T visitor) throws IOException {
		return TestMappings.generateOuterClassNamePropagation(visitor);
	}

	@Test
	public void visitor() throws IOException {
		acceptMappings(new OuterClassNamePropagator(
				new OuterClassNameChecker(false, dstNamespaces, true)));

		for (int pass = 1; pass <= 2; pass++) {
			boolean processRemappedDstNames = pass == 1;

			acceptMappings(new OuterClassNamePropagator(
					new OuterClassNameChecker(false, dstNamespaces, processRemappedDstNames),
					dstNamespaces,
					processRemappedDstNames));
		}

		assertThrows(UnsupportedOperationException.class, () -> acceptMappings(new OuterClassNamePropagator(
				new NopMappingVisitor(false),
				Collections.singletonList(srcNamespace),
				false)));
		assertThrows(IllegalArgumentException.class, () -> acceptMappings(new OuterClassNamePropagator(
				new NopMappingVisitor(false),
				Collections.singletonList(invalidNs),
				false)));
	}

	@Test
	public void visitorThroughTree() throws IOException {
		for (int pass = 1; pass <= 2; pass++) {
			boolean processRemappedDstNames = pass == 1;

			VisitableMappingTree tree = new MemoryMappingTree();
			acceptMappings(new OuterClassNamePropagator(tree, dstNamespaces, processRemappedDstNames));
			tree.accept(new OuterClassNameChecker(true, dstNamespaces, processRemappedDstNames));

			checkDiskEquivalence(tree, processRemappedDstNames);
		}
	}

	@Test
	public void tree() throws IOException {
		for (int pass = 1; pass <= 2; pass++) {
			boolean processRemappedDstNames = pass == 1;

			VisitableMappingTree tree = acceptMappings(new MemoryMappingTree());
			tree.propagateOuterClassNames(processRemappedDstNames);
			tree.accept(new OuterClassNameChecker(true, dstNamespaces, processRemappedDstNames));

			checkDiskEquivalence(tree, processRemappedDstNames);
		}

		VisitableMappingTree tree = acceptMappings(new MemoryMappingTree());

		assertThrows(UnsupportedOperationException.class, () -> tree.propagateOuterClassNames(
				dstNamespaces.get(0),
				Collections.singletonList(srcNamespace),
				false));
		assertThrows(IllegalArgumentException.class, () -> tree.propagateOuterClassNames(
				invalidNs,
				tree.getDstNamespaces(),
				false));
		assertThrows(IllegalArgumentException.class, () -> tree.propagateOuterClassNames(
				tree.getSrcNamespace(),
				Collections.singletonList(invalidNs),
				false));
	}

	private void checkDiskEquivalence(VisitableMappingTree tree, boolean processRemappedDstNames) throws IOException {
		for (MappingFormat format : MappingFormat.values()) {
			MappingDir dir = processRemappedDstNames
					? TestMappings.PROPAGATION.PROPAGATED
					: TestMappings.PROPAGATION.PROPAGATED_EXCEPT_REMAPPED_DST;

			VisitableMappingTree diskTree = dir.read(format, new MemoryMappingTree());

			tree.accept(new FlatAsRegularMappingVisitor(new SubsetEnforcer(diskTree, format, null)));
			diskTree.accept(new FlatAsRegularMappingVisitor(new SubsetEnforcer(tree, null, format)));
		}
	}

	private static class OuterClassNameChecker extends NopMappingVisitor {
		OuterClassNameChecker(boolean mappingsPassedThroughTree, List<String> dstNamespaces, boolean processRemappedDstNames) {
			super(true);
			this.tree = mappingsPassedThroughTree;
			this.processRemappedDstNames = processRemappedDstNames;
		}

		@Override
		public Set<MappingFlag> getFlags() {
			return EnumSet.of(MappingFlag.NEEDS_DST_FIELD_DESC, MappingFlag.NEEDS_DST_METHOD_DESC);
		}

		@Override
		public boolean visitClass(String srcName) throws IOException {
			clsSrcName = srcName;
			return true;
		}

		@Override
		public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
			if (tree) return; // trees handle destination descriptor remapping themselves

			switch (clsSrcName) {
			case "class_1":
				if (!tree) {
					assertEquals("Lclass_1;", desc);
					break;
				}

				switch (namespace) {
				case 0:
					assertEquals("Lclass1Ns0Rename;", desc);
					break;
				case 1:
					assertEquals("Lclass1Ns1Rename;", desc);
					break;
				case 2:
					assertEquals("Lclass1Ns2Rename;", desc);
					break;
				case 4:
					assertEquals("Lclass1Ns4Rename;", desc);
					break;
				default:
					throw new IllegalStateException();
				}

				break;
			case "class_1$class_2":
				switch (namespace) {
				case 0:
					assertEquals("Lclass1Ns0Rename$class_2;", desc);
					break;
				case 1:
					assertEquals("Lclass1Ns1Rename$class_2;", desc);
					break;
				case 2:
					assertEquals("Lclass1Ns2Rename$class2Ns2Rename;", desc);
					break;
				case 3:
					assertEquals("Lclass_1$class2Ns3Rename;", desc);
					break;
				case 4:
					assertEquals("Lclass1Ns4Rename$class_2;", desc);
					break;
				case 5:
					assertEquals("Lclass_1$class2Ns5Rename;", desc);
					break;
				case 6:
					assertEquals("Lclass_1$class_2;", desc);
					break;
				default:
					throw new IllegalStateException();
				}

				break;
			case "class_1$class_2$class_3":
				switch (namespace) {
				case 0:
					assertEquals("Lclass1Ns0Rename$class_2$class_3;", desc);
					break;
				case 1:
					assertEquals("Lclass1Ns1Rename$class_2$class_3;", desc);
					break;
				case 2:
					assertEquals("Lclass1Ns2Rename$class2Ns2Rename$class_3;", desc);
					break;
				case 3:
					assertEquals("Lclass_1$class2Ns3Rename$class_3;", desc);
					break;
				case 4:
					assertEquals("Lclass1Ns4Rename$class_2$class_3;", desc);
					break;
				case 5:
					assertEquals(
							processRemappedDstNames
									? "Lclass_1$class2Ns5Rename$class3Ns5Rename;"
									: "Lclass_1$class_2$class3Ns5Rename;",
							desc);
					break;
				case 6:
					assertEquals("Lclass_1$class_2$class3Ns6Rename;", desc);
					break;
				default:
					throw new IllegalStateException();
				}

				break;
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
			if (targetKind != MappedElementKind.CLASS) return;

			switch (clsSrcName) {
			case "class_1":
				break;
			case "class_1$class_2":
				switch (namespace) {
				case 0:
					assertEquals("class1Ns0Rename$class_2", name);
					break;
				case 1:
					assertEquals("class1Ns1Rename$class_2", name);
					break;
				case 2:
					assertEquals("class1Ns2Rename$class2Ns2Rename", name);
					break;
				case 3:
					assertEquals("class_1$class2Ns3Rename", name);
					break;
				case 4:
					assertEquals("class1Ns4Rename$class_2", name);
					break;
				case 5:
					assertEquals("class_1$class2Ns5Rename", name);
					break;
				case 6:
					assertEquals("class_1$class_2", name);
					break;
				default:
					throw new IllegalStateException();
				}

				break;
			case "class_1$class_2$class_3":
				switch (namespace) {
				case 0:
					assertEquals("class1Ns0Rename$class_2$class_3", name);
					break;
				case 1:
					assertEquals("class1Ns1Rename$class_2$class_3", name);
					break;
				case 2:
					assertEquals("class1Ns2Rename$class2Ns2Rename$class_3", name);
					break;
				case 3:
					assertEquals("class_1$class2Ns3Rename$class_3", name);
					break;
				case 4:
					assertEquals("class1Ns4Rename$class_2$class_3", name);
					break;
				case 5:
					assertEquals(
							processRemappedDstNames
									? "class_1$class2Ns5Rename$class3Ns5Rename"
									: "class_1$class_2$class3Ns5Rename",
							name);
					break;
				case 6:
					assertEquals("class_1$class_2$class3Ns6Rename", name);
					break;
				default:
					throw new IllegalStateException();
				}

				break;
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		public boolean visitEnd() throws IOException {
			return ++passesDone == 2;
		}

		private final boolean tree;
		private final boolean processRemappedDstNames;
		private byte passesDone = 0;
		private String clsSrcName;
	}
}

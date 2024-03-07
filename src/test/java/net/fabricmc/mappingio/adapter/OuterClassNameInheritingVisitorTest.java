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

package net.fabricmc.mappingio.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.NopMappingVisitor;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class OuterClassNameInheritingVisitorTest {
	private static void accept(MappingVisitor visitor) throws IOException {
		do {
			if (visitor.visitHeader()) {
				visitor.visitNamespaces("source", Arrays.asList("dstNs0", "dstNs1", "dstNs2", "dstNs3", "dstNs4", "dstNs5", "dstNs6"));
			}

			if (visitor.visitContent()) {
				if (visitor.visitClass("class_1")) {
					visitor.visitDstName(MappedElementKind.CLASS, 0, "class1Ns0Rename");
					visitor.visitDstName(MappedElementKind.CLASS, 1, "class1Ns1Rename");
					visitor.visitDstName(MappedElementKind.CLASS, 2, "class1Ns2Rename");
					visitor.visitDstName(MappedElementKind.CLASS, 4, "class1Ns4Rename");

					if (visitor.visitElementContent(MappedElementKind.CLASS)) {
						if (visitor.visitField("field_1", "Lclass_1;")) {
							for (int i = 0; i <= 6; i++) {
								visitor.visitDstDesc(MappedElementKind.FIELD, i, "Lclass_1;");
								visitor.visitElementContent(MappedElementKind.FIELD);
							}
						}
					}
				}

				if (visitor.visitClass("class_1$class_2")) {
					visitor.visitDstName(MappedElementKind.CLASS, 2, "class1Ns2Rename$class2Ns2Rename");
					visitor.visitDstName(MappedElementKind.CLASS, 3, "class_1$class2Ns3Rename");
					visitor.visitDstName(MappedElementKind.CLASS, 4, "class_1$class_2");
					visitor.visitDstName(MappedElementKind.CLASS, 5, "class_1$class2Ns5Rename");

					if (visitor.visitElementContent(MappedElementKind.CLASS)) {
						if (visitor.visitField("field_2", "Lclass_1$class_2;")) {
							for (int i = 0; i <= 6; i++) {
								visitor.visitDstDesc(MappedElementKind.FIELD, i, "Lclass_1$class_2;");
								visitor.visitElementContent(MappedElementKind.FIELD);
							}
						}
					}
				}

				if (visitor.visitClass("class_1$class_2$class_3")) {
					visitor.visitDstName(MappedElementKind.CLASS, 5, "class_1$class2Ns5Rename$class3Ns5Rename");
					visitor.visitDstName(MappedElementKind.CLASS, 6, "class_1$class_2$class3Ns6Rename");

					if (visitor.visitElementContent(MappedElementKind.CLASS)) {
						if (visitor.visitField("field_2", "Lclass_1$class_2$class_3;")) {
							for (int i = 0; i <= 6; i++) {
								visitor.visitDstDesc(MappedElementKind.FIELD, i, "Lclass_1$class_2$class_3;");
								visitor.visitElementContent(MappedElementKind.FIELD);
							}
						}
					}
				}
			}
		} while (!visitor.visitEnd());
	}

	@Test
	public void directVisit() throws IOException {
		accept(new OuterClassNameInheritingVisitor(new CheckingVisitor(false)));
	}

	@Test
	public void tree() throws IOException {
		VisitableMappingTree tree = new MemoryMappingTree();
		accept(new OuterClassNameInheritingVisitor(tree));
		tree.accept(new CheckingVisitor(true));
	}

	private static class CheckingVisitor extends NopMappingVisitor {
		CheckingVisitor(boolean tree) {
			super(true);
			this.tree = tree;
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
				assertEquals("Lclass_1;", desc);
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
					assertEquals("Lclass_1$class_2;", desc);
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
					assertEquals("Lclass_1$class_2$class_3;", desc);
					break;
				case 5:
					assertEquals("Lclass_1$class2Ns5Rename$class3Ns5Rename;", desc);
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
					assertEquals("class_1$class_2", name);
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
					assertEquals("class_1$class_2$class_3", name);
					break;
				case 5:
					assertEquals("class_1$class2Ns5Rename$class3Ns5Rename", name);
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
		private byte passesDone = 0;
		private String clsSrcName;
	}
}

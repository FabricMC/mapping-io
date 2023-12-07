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

import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

public class OuterClassNameInheritingVisitorTest {
	@Test
	public void testOuterClassFixing() throws IOException {
		VisitableMappingTree tree = new MemoryMappingTree();
		tree.visitNamespaces("source", Arrays.asList("dstNs0", "dstNs1", "dstNs2", "dstNs3", "dstNs4", "dstNs5", "dstNs6"));

		tree.visitClass("class_1");
		tree.visitDstName(MappedElementKind.CLASS, 0, "class1Ns0Rename");
		tree.visitDstName(MappedElementKind.CLASS, 1, "class1Ns1Rename");
		tree.visitDstName(MappedElementKind.CLASS, 2, "class1Ns2Rename");
		tree.visitDstName(MappedElementKind.CLASS, 4, "class1Ns4Rename");

		tree.visitClass("class_1$class_2");
		tree.visitDstName(MappedElementKind.CLASS, 2, "class1Ns2Rename$class2Ns2Rename");
		tree.visitDstName(MappedElementKind.CLASS, 3, "class_1$class2Ns3Rename");
		tree.visitDstName(MappedElementKind.CLASS, 4, "class_1$class_2");
		tree.visitDstName(MappedElementKind.CLASS, 5, "class_1$class2Ns5Rename");

		tree.visitClass("class_1$class_2$class_3");
		tree.visitDstName(MappedElementKind.CLASS, 5, "class_1$class2Ns5Rename$class3Ns5Rename");
		tree.visitDstName(MappedElementKind.CLASS, 6, "class_1$class_2$class3Ns6Rename");

		VisitableMappingTree processedTree = new MemoryMappingTree();
		tree.accept(new OuterClassNameInheritingVisitor(processedTree));

		ClassMapping class2 = processedTree.getClass("class_1$class_2");
		ClassMapping class3 = processedTree.getClass("class_1$class_2$class_3");

		assertEquals("class1Ns0Rename$class_2", class2.getDstName(0));
		assertEquals("class1Ns0Rename$class_2$class_3", class3.getDstName(0));

		assertEquals("class1Ns1Rename$class_2", class2.getDstName(1));
		assertEquals("class1Ns1Rename$class_2$class_3", class3.getDstName(1));

		assertEquals("class1Ns2Rename$class2Ns2Rename", class2.getDstName(2));
		assertEquals("class1Ns2Rename$class2Ns2Rename$class_3", class3.getDstName(2));

		assertEquals("class_1$class2Ns3Rename", class2.getDstName(3));
		assertEquals("class_1$class2Ns3Rename$class_3", class3.getDstName(3));

		assertEquals("class_1$class_2", class2.getDstName(4));
		assertEquals("class_1$class_2$class_3", class3.getDstName(4));

		assertEquals("class_1$class2Ns5Rename", class2.getDstName(5));
		assertEquals("class_1$class2Ns5Rename$class3Ns5Rename", class3.getDstName(5));

		assertEquals("class_1$class_2$class3Ns6Rename", class3.getDstName(6));
	}
}

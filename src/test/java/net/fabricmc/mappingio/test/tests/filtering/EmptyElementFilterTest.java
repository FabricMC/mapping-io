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

package net.fabricmc.mappingio.test.tests.filtering;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.EmptyEntryFilter;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.test.TestMappings.MappingDir;
import net.fabricmc.mappingio.test.visitors.SubsetAsserter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class EmptyElementFilterTest {
	private static <T extends MappingVisitor> T acceptMappings(T visitor) throws IOException {
		return TestMappings.generateEmptyElementFiltering(visitor);
	}

	@Test
	public void visitorThroughTree() throws IOException {
		for (int pass = 1; pass <= 2; pass++) {
			boolean treatSrcOnDstAsEmpty = pass == 1;
			VisitableMappingTree tree = new MemoryMappingTree();

			acceptMappings(new EmptyEntryFilter(tree, treatSrcOnDstAsEmpty));
			checkDiskEquivalence(tree, treatSrcOnDstAsEmpty);
		}
	}

	private void checkDiskEquivalence(VisitableMappingTree tree, boolean treatSrcOnDstAsEmpty) throws IOException {
		MappingDir dir = treatSrcOnDstAsEmpty
				? TestMappings.FILTERING.FILTERED
				: TestMappings.FILTERING.FILTERED_EXCEPT_SRC_ON_DST;

		for (MappingFormat format : MappingFormat.values()) {
			assertDoesNotThrow(() -> checkDiskEquivalence(tree, dir, format), "Failed for " + dir + " with " + format);
		}
	}

	private void checkDiskEquivalence(VisitableMappingTree tree, MappingDir dir, MappingFormat format) throws IOException {
		VisitableMappingTree diskTree = dir.read(format, new MemoryMappingTree());

		tree.accept(new FlatAsRegularMappingVisitor(new SubsetAsserter(diskTree, format, null)));
		diskTree.accept(new FlatAsRegularMappingVisitor(new SubsetAsserter(tree, null, format)));
	}
}

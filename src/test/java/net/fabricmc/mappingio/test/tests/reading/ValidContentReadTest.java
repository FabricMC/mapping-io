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

package net.fabricmc.mappingio.test.tests.reading;

import java.nio.file.Files;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.adapter.VisitOrderVerifier;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.test.TestMappings.MappingDir;
import net.fabricmc.mappingio.test.visitors.SubsetAsserter;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class ValidContentReadTest {
	@Test
	public void run() throws Exception {
		for (MappingDir dir : TestMappings.values()) {
			for (MappingFormat format : MappingFormat.values()) {
				check(dir, format);
			}
		}
	}

	private void check(MappingDir dir, MappingFormat format) throws Exception {
		if (!dir.supportsGeneration() || !Files.exists(dir.pathFor(format))) {
			return;
		}

		if (dir == TestMappings.PROPAGATION.UNPROPAGATED && format == MappingFormat.ENIGMA_FILE) {
			// Enigma files cannot represent unpropagated outer class names,
			// they are always propagated automatically since inner classes are stored as children of outer classes.
			// The directory format on the other hand is able to do so by storing the classes in separate files.
			return;
		}

		// TODO: The Tiny v2 spec also disallows repeated elements, there should at least be warnings
		boolean allowConsecutiveDuplicateElementVisits = dir == TestMappings.READING.REPEATED_ELEMENTS;

		VisitableMappingTree referenceTree = dir.generate(new MemoryMappingTree());
		VisitableMappingTree tree = new MemoryMappingTree();

		dir.read(format, new VisitOrderVerifier(tree, allowConsecutiveDuplicateElementVisits));
		assertEqual(tree, format, referenceTree, allowConsecutiveDuplicateElementVisits);

		if (dir == TestMappings.READING.HOLES && !format.features().hasNamespaces()) {
			return;
		}

		tree = new MemoryMappingTree();
		String newSrcNs = format.features().hasNamespaces()
				? referenceTree.getDstNamespaces().get(0)
				: MappingUtil.NS_TARGET_FALLBACK;
		MappingVisitor target = new MappingSourceNsSwitch(
				new VisitOrderVerifier(
						new MappingSourceNsSwitch(
								new VisitOrderVerifier(tree, allowConsecutiveDuplicateElementVisits),
								referenceTree.getSrcNamespace()),
						allowConsecutiveDuplicateElementVisits),
				newSrcNs);

		dir.read(format, target);
		assertEqual(tree, format, referenceTree, allowConsecutiveDuplicateElementVisits);
	}

	private void assertEqual(MappingTreeView tree, MappingFormat format, MappingTreeView referenceTree, boolean allowConsecutiveDuplicateElementVisits) throws Exception {
		assertSubset(tree, format, referenceTree, null, allowConsecutiveDuplicateElementVisits);
		assertSubset(referenceTree, null, tree, format, allowConsecutiveDuplicateElementVisits);
	}

	private void assertSubset(MappingTreeView subTree, @Nullable MappingFormat subFormat, MappingTreeView supTree, @Nullable MappingFormat supFormat, boolean allowConsecutiveDuplicateElementVisits) throws Exception {
		subTree.accept(new VisitOrderVerifier(
				new FlatAsRegularMappingVisitor(
						new SubsetAsserter(supTree, supFormat, subFormat)),
				allowConsecutiveDuplicateElementVisits));
	}
}

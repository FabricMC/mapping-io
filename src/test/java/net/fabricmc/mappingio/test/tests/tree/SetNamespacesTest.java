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

package net.fabricmc.mappingio.test.tests.tree;

import static net.fabricmc.mappingio.test.TestUtil.createTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class SetNamespacesTest {
	private MemoryMappingTree tree;
	private String srcNs;
	private String dstNs0;
	private String dstNs1;

	@BeforeEach
	public void setup() throws IOException {
		tree = TestMappings.generateValid(createTree());

		srcNs = tree.getSrcNamespace();
		dstNs0 = tree.getDstNamespaces().get(0);
		dstNs1 = tree.getDstNamespaces().get(1);
	}

	@Test
	public void srcToNewSrc() {
		String srcNsNew = srcNs + "New";

		tree.setSrcNamespace(srcNsNew);
		assertSrc(srcNsNew);
		assertDst(dstNs0, dstNs1);

		tree.setSrcNamespace(srcNs);
		assertSrc(srcNs);
		assertDst(dstNs0, dstNs1);
	}

	@Test
	public void dstToNewDst() {
		String dstNs0New = dstNs0 + "New";
		String dstNs1New = dstNs1 + "New";

		tree.setDstNamespaces(Arrays.asList(dstNs0New, dstNs1New));
		assertSrc(srcNs);
		assertDst(dstNs0New, dstNs1New);

		tree.setDstNamespaces(Arrays.asList(dstNs0, dstNs1));
		assertSrc(srcNs);
		assertDst(dstNs0, dstNs1);
	}

	@Test
	public void srcToDst() {
		assertThrows(UnsupportedOperationException.class, () -> tree.setSrcNamespace(dstNs0));
	}

	@Test
	public void dstToSrc() {
		assertThrows(UnsupportedOperationException.class, () -> tree.setDstNamespaces(Arrays.asList(srcNs, dstNs1)));
	}

	@Test
	public void dstToDst() {
		tree.setDstNamespaces(Arrays.asList(dstNs1, dstNs0));
		assertSrc(srcNs);
		assertDst(dstNs1, dstNs0);

		tree.setDstNamespaces(Arrays.asList(dstNs0, dstNs1));
		assertSrc(srcNs);
		assertDst(dstNs0, dstNs1);
	}

	@Test
	public void duplicateDst() {
		assertThrows(IllegalArgumentException.class, () -> tree.setDstNamespaces(Arrays.asList(dstNs0, dstNs0)));
		assertThrows(IllegalArgumentException.class, () -> tree.setDstNamespaces(Arrays.asList(dstNs1, dstNs1)));
		assertThrows(IllegalArgumentException.class, () -> tree.setDstNamespaces(Arrays.asList(dstNs1 + "New", dstNs1 + "New")));
	}

	private void assertSrc(String srcNs) {
		assertEquals(tree.getSrcNamespace(), srcNs);
	}

	private void assertDst(String... dstNs) {
		assertEquals(tree.getDstNamespaces(), Arrays.asList(dstNs));
	}
}

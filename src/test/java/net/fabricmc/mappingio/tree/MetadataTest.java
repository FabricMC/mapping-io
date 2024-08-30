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

package net.fabricmc.mappingio.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.NopMappingVisitor;
import net.fabricmc.mappingio.TestHelper;

public class MetadataTest {
	private static final Random random = new Random();
	private static final List<String> keys = new ArrayList<>();
	private static final List<String> values = new ArrayList<>();
	private static VisitableMappingTree tree;

	@BeforeAll
	public static void setup() throws Exception {
		tree = TestHelper.createTestTree();

		for (int i = 0; i < 40; i++) {
			String key = "key" + random.nextInt(3);
			String value = "position" + i;

			keys.add(key);
			values.add(value);
			tree.visitMetadata(key, value);
		}
	}

	@Test
	public void testOrder() throws Exception {
		tree.accept(new NopMappingVisitor(true) {
			@Override
			public void visitMetadata(String key, @Nullable String value) {
				assertEquals(key, keys.get(visitCount));
				assertEquals(value, values.get(visitCount));
				visitCount++;
			}

			int visitCount;
		});
	}

	@Test
	public void testUniqueness() throws Exception {
		tree.accept(new NopMappingVisitor(true) {
			@Override
			public Set<MappingFlag> getFlags() {
				return EnumSet.of(MappingFlag.NEEDS_METADATA_UNIQUENESS);
			}

			@Override
			public void visitMetadata(String key, @Nullable String value) {
				assertFalse(visitedKeys.contains(key));
				visitedKeys.add(key);
			}

			Set<String> visitedKeys = new HashSet<>();
		});
	}
}

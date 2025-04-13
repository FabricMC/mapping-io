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

package net.fabricmc.mappingio.test.visitors;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import net.fabricmc.mappingio.adapter.SubsetChecker;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;

/**
 * {@link SubsetChecker} that throws {@link AssertionFailedError}s.
 */
public class SubsetAsserter extends SubsetChecker {
	/**
	 * @param supTree The superset tree.
	 * @param supFormat The superset format, or null if supTree has all the original data.
	 * @param subFormat The subset format, or null if lossless (i.e. if the visits are coming from a tree).
	 */
	public SubsetAsserter(MappingTreeView supTree, @Nullable MappingFormat supFormat, @Nullable MappingFormat subFormat) {
		super(supTree, supFormat, subFormat, message -> {
			throw new AssertionFailedError(message);
		});
	}

	@Override
	protected void assertTrue(boolean condition, String message) {
		Assertions.assertTrue(condition, message);
	}

	@Override
	protected void assertFalse(boolean condition, String message) {
		Assertions.assertFalse(condition, message);
	}

	@Override
	protected void assertEquals(Object expected, Object actual, String message) {
		Assertions.assertEquals(expected, actual, message);
	}

	@Override
	protected void assertNotNull(Object obj, String message) {
		Assertions.assertNotNull(obj, message);
	}
}

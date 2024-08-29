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

package net.fabricmc.mappingio;

import java.io.IOException;
import java.nio.file.Path;

import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;

public class TestFileUpdater {
	public static void main(String[] args) throws IOException {
		MappingTreeView defaultTree = TestHelper.createTestTree();
		MappingTreeView holesTree = TestHelper.createTestTreeWithHoles();

		for (MappingFormat format : MappingFormat.values()) {
			Path defaultPath = TestHelper.MappingDirs.VALID.resolve(TestHelper.getFileName(format));
			Path holesPath = TestHelper.MappingDirs.VALID_WITH_HOLES.resolve(TestHelper.getFileName(format));

			defaultTree.accept(MappingWriter.create(defaultPath, format));
			holesTree.accept(MappingWriter.create(holesPath, format));
		}
	}
}

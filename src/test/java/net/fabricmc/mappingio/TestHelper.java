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

package net.fabricmc.mappingio;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class TestHelper {
	public static Path getResource(String slashPrefixedResourcePath) {
		try {
			return Paths.get(TestHelper.class.getResource(slashPrefixedResourcePath).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static MemoryMappingTree createTestTree() {
		MemoryMappingTree tree = new MemoryMappingTree();
		tree.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, Arrays.asList(MappingUtil.NS_TARGET_FALLBACK));

		tree.visitClass("class_1");
		tree.visitDstName(MappedElementKind.CLASS, 0, "RenamedClass1");

		tree.visitField("field_1", "I");
		tree.visitDstName(MappedElementKind.FIELD, 0, "renamedField");

		tree.visitMethod("method_1", "(F)I");
		tree.visitDstName(MappedElementKind.METHOD, 0, "renamedMethod");

		tree.visitMethodArg(0, 0, "param_1");
		tree.visitDstName(MappedElementKind.METHOD_ARG, 0, "renamedParameter");

		tree.visitMethodVar(0, 0, 0, 0, "param_1");
		tree.visitDstName(MappedElementKind.METHOD_VAR, 0, "renamedVariable");

		tree.visitClass("class_1$class_2");
		tree.visitDstName(MappedElementKind.CLASS, 0, "RenamedClass1$RenamedInnerClass");

		tree.visitField("field_1", "I");
		tree.visitDstName(MappedElementKind.FIELD, 0, "renamedField2");

		tree.visitClass("class_3");
		tree.visitDstName(MappedElementKind.CLASS, 0, "RenamedClass2");

		return tree;
	}

	public static void writeToDir(MappingTree tree, MappingFormat format, Path dir) throws IOException {
		MappingWriter writer = MappingWriter.create(dir.resolve(format.name() + "." + format.fileExt), format);
		tree.accept(writer);
		writer.close();
	}
}

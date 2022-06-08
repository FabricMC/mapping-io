/*
 * Copyright (c) 2021 FabricMC
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

package net.fabricmc.mappingio.format;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;

public final class EnigmaDirReader {
	public static void read(Path dir, MappingVisitor visitor) throws IOException {
		read(dir, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor);
	}

	public static void read(Path dir, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("." + MappingFormat.ENIGMA.fileExt)) {
					EnigmaReader.read(Files.newBufferedReader(file), sourceNs, targetNs, visitor);
				}

				return FileVisitResult.CONTINUE;
			}
		});
		visitor.visitEnd();
	}
}

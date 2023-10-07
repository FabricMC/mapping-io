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

package net.fabricmc.mappingio.format.enigma;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.MappingDirReader;
import net.fabricmc.mappingio.format.MappingFormat;

public final class EnigmaDirReader implements MappingDirReader {
	private EnigmaDirReader() {
	}

	public static EnigmaDirReader getInstance() {
		return INSTANCE;
	}

	@Override
	public void read(Reader reader, Path dir, MappingVisitor visitor) throws IOException {
		Objects.requireNonNull(dir, "dir path must not be null");
		read(dir, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor);
	}

	public void read(Path dir, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("." + MappingFormat.ENIGMA_FILE.fileExt)) {
					try (BufferedReader reader = Files.newBufferedReader(file)) {
						EnigmaFileReader.getInstance().read(reader, sourceNs, targetNs, visitor);
					}
				}

				return FileVisitResult.CONTINUE;
			}
		});
		visitor.visitEnd();
	}

	private static final EnigmaDirReader INSTANCE = new EnigmaDirReader();
}

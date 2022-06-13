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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.format.MappingFormat;

public final class EnigmaDirWriter extends EnigmaWriterBase {
	public EnigmaDirWriter(Path dir, boolean deleteExistingFiles) throws IOException {
		super(null);
		this.dir = dir.toAbsolutePath().normalize();

		if (deleteExistingFiles && Files.exists(dir)) {
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.getFileName().toString().endsWith("." + MappingFormat.ENIGMA.fileExt)) {
						Files.delete(file);
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
					try {
						if (!dir.equals(file)) Files.delete(file);
					} catch (DirectoryNotEmptyException e) {
						// ignore
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	@Override
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
			writer = null;
			currentClass = null;
		}
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (targetKind == MappedElementKind.CLASS) {
			String name = dstName != null ? dstName : srcClassName;

			if (currentClass == null
					|| !name.startsWith(currentClass)
					|| name.length() > currentClass.length() && name.charAt(currentClass.length()) != '$') {
				int pos = getNextOuterEnd(name, 0);
				if (pos >= 0) name = name.substring(0, pos);

				// currentClass is not an outer class of srcName (or the same)
				Path file = dir.resolve(name + "." + MappingFormat.ENIGMA.fileExt).normalize();
				if (!file.startsWith(dir)) throw new RuntimeException("invalid name: " + name);

				if (writer != null) {
					writer.close();
				}

				currentClass = name;

				if (Files.exists(file)) {
					// initialize writtenClass with last CLASS entry

					List<String> writtenClassParts = new ArrayList<>();

					try (BufferedReader reader = Files.newBufferedReader(file)) {
						String line;

						while ((line = reader.readLine()) != null) {
							int offset = 0;

							while (offset < line.length() && line.charAt(offset) == '\t') {
								offset++;
							}

							if (line.startsWith("CLASS ", offset)) {
								int start = offset + 6;
								int end = line.indexOf(' ', start);
								if (end < 0) end = line.length();
								String part = line.substring(start, end);

								while (writtenClassParts.size() > offset) {
									writtenClassParts.remove(writtenClassParts.size() - 1);
								}

								writtenClassParts.add(part);
							}
						}
					}

					lastWrittenClass = String.join("$", writtenClassParts);
				} else {
					lastWrittenClass = "";
					Files.createDirectories(file.getParent());
				}

				writer = Files.newBufferedWriter(file, StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			}

			writeMismatchedOrMissingClasses();
		} else if (targetKind == MappedElementKind.FIELD || targetKind == MappedElementKind.METHOD) {
			writer.write(' ');
			writer.write(desc);
			writer.write('\n');
		} else {
			writer.write('\n');
		}

		return true;
	}

	private final Path dir;
}

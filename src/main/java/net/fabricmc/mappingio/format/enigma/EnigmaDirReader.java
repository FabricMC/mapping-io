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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Set;

import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class EnigmaDirReader {
	private EnigmaDirReader() {
	}

	public static void read(Path dir, MappingVisitor visitor) throws IOException {
		read(dir, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor);
	}

	public static void read(Path dir, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		Set<MappingFlag> flags = visitor.getFlags();
		MappingVisitor parentVisitor = null;

		if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS) || flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			parentVisitor = visitor;
			visitor = new MemoryMappingTree();
		}

		if (visitor.visitHeader()) {
			visitor.visitNamespaces(sourceNs, Collections.singletonList(targetNs));
		}

		MappingVisitor delegatingVisitor = new ForwardingMappingVisitor(visitor) {
			@Override
			public boolean visitHeader() throws IOException {
				return false; // Namespaces have already been visited above, and Enigma files don't have any metadata
			}

			@Override
			public boolean visitContent() throws IOException {
				if (!visitedContent) { // Don't call next's visitContent() more than once
					visitedContent = true;
					visitContent = super.visitContent();
				}

				return visitContent;
			}

			@Override
			public boolean visitEnd() throws IOException {
				return true; // Don't forward since we're not done yet, there are more files to come
			}

			private boolean visitedContent;
			private boolean visitContent;
		};

		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.getFileName().toString().endsWith("." + MappingFormat.ENIGMA_FILE.fileExt)) {
					EnigmaFileReader.read(Files.newBufferedReader(file), sourceNs, targetNs, delegatingVisitor);
				}

				return FileVisitResult.CONTINUE;
			}
		});

		visitor.visitEnd();

		if (parentVisitor != null) {
			((MappingTree) visitor).accept(parentVisitor);
		}
	}
}

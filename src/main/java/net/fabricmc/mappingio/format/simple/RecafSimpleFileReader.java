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

package net.fabricmc.mappingio.format.simple;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class RecafSimpleFileReader {
	private RecafSimpleFileReader() {
	}

	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(reader, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor);
	}

	public static void read(Reader reader, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		read(new ColumnFileReader(reader, ' '), sourceNs, targetNs, visitor);
	}

	private static void read(ColumnFileReader reader, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		Set<MappingFlag> flags = visitor.getFlags();
		MappingVisitor parentVisitor = null;

		if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS)) {
			parentVisitor = visitor;
			visitor = new MemoryMappingTree();
		} else if (flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			reader.mark();
		}

		for (;;) {
			if (visitor.visitHeader()) {
				visitor.visitNamespaces(sourceNs, Collections.singletonList(targetNs));
			}

			if (visitor.visitContent()) {
				String line;
				String lastClass = null;
				boolean visitClass = false;

				do {
					line = reader.nextCols(true);

					// Skip comments and empty lines
					if (line == null || line.trim().isEmpty() || line.trim().startsWith("#")) continue;

					String[] parts = line.split(" ");
					int dotPos = parts[0].lastIndexOf('.');
					String clsSrcName;
					String clsDstName = null;
					String memberSrcName = null;
					String memberSrcDesc = null;
					String memberDstName = null;
					boolean isMethod = false;

					if (dotPos < 0) { // class
						clsSrcName = parts[0];
						clsDstName = parts[1];
					} else { // member
						clsSrcName = parts[0].substring(0, dotPos);
						String memberIdentifier = parts[0].substring(dotPos + 1);
						memberDstName = parts[1];

						if (parts.length >= 3) { // field with descriptor
							memberSrcName = memberIdentifier;
							memberSrcDesc = parts[1];
							memberDstName = parts[2];
						} else if (parts.length == 2) { // field without descriptor or method
							int mthDescPos = memberIdentifier.lastIndexOf("(");

							if (mthDescPos < 0) { // field
								memberSrcName = memberIdentifier;
							} else { // method
								isMethod = true;
								memberSrcName = memberIdentifier.substring(0, mthDescPos);
								memberSrcDesc = memberIdentifier.substring(mthDescPos);
							}
						} else {
							throw new IOException("Invalid Recaf Simple line "+reader.getLineNumber()+": Insufficient column count!");
						}
					}

					if (!clsSrcName.equals(lastClass)) {
						visitClass = visitor.visitClass(clsSrcName);
						lastClass = clsSrcName;

						if (visitClass) {
							if (clsDstName != null) visitor.visitDstName(MappedElementKind.CLASS, 0, clsDstName);
							visitClass = visitor.visitElementContent(MappedElementKind.CLASS);
						}
					}

					if (visitClass && memberSrcName != null) {
						if (!isMethod && visitor.visitField(memberSrcName, memberSrcDesc)) {
							visitor.visitDstName(MappedElementKind.FIELD, 0, memberDstName);
						} else if (isMethod && visitor.visitMethod(memberSrcName, memberSrcDesc)) {
							visitor.visitDstName(MappedElementKind.METHOD, 0, memberDstName);
						}
					}
				} while (reader.nextLine(0));
			}

			if (visitor.visitEnd()) break;
			reader.reset();
		}

		if (parentVisitor != null) {
			((MappingTree) visitor).accept(parentVisitor);
		}
	}
}

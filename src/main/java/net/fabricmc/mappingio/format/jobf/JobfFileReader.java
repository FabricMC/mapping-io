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

package net.fabricmc.mappingio.format.jobf;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/**
 * {@linkplain MappingFormat#JOBF_FILE JOBF file} reader.
 */
public class JobfFileReader {
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
				String lastClass = null;
				boolean visitLastClass = false;

				do {
					boolean isField;

					if (reader.nextCol("c")) { // class: c <name-a> = <name-b>
						String srcName = reader.nextCol();
						if (srcName == null || srcName.isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());
						srcName = srcName.replace('.', '/');

						if (!srcName.equals(lastClass)) {
							lastClass = srcName;
							visitLastClass = visitor.visitClass(srcName);

							if (visitLastClass) {
								readSeparator(reader);

								String dstName = reader.nextCol();
								if (dstName == null || dstName.isEmpty()) throw new IOException("missing class-name-b in line "+reader.getLineNumber());

								visitor.visitDstName(MappedElementKind.CLASS, 0, dstName);
								visitLastClass = visitor.visitElementContent(MappedElementKind.CLASS);
							}
						}
					} else if ((isField = reader.nextCol("f")) || reader.nextCol("m")) {
						// field: f <cls-a>.<name-a>:<desc-a> = <name-b>
						// method: m <cls-a>.<name-a><desc-a> = <name-b>
						String src = reader.nextCol();
						if (src == null || src.isEmpty()) throw new IOException("missing class/name/desc a in line "+reader.getLineNumber());

						int nameSepPos = src.lastIndexOf('.');
						if (nameSepPos <= 0 || nameSepPos == src.length() - 1) throw new IOException("invalid class/name/desc a in line "+reader.getLineNumber());

						int descSepPos = src.lastIndexOf(isField ? ':' : '(');
						if (descSepPos <= 0 || descSepPos == src.length() - 1) throw new IOException("invalid name/desc a in line "+reader.getLineNumber());

						readSeparator(reader);

						String dstName = reader.nextCol();
						if (dstName == null || dstName.isEmpty()) throw new IOException("missing name-b in line "+reader.getLineNumber());

						String srcOwner = src.substring(0, nameSepPos).replace('.', '/');

						if (!srcOwner.equals(lastClass)) {
							lastClass = srcOwner;
							visitLastClass = visitor.visitClass(srcOwner);

							if (visitLastClass) {
								visitLastClass = visitor.visitElementContent(MappedElementKind.CLASS);
							}
						}

						if (visitLastClass) {
							String srcName = src.substring(nameSepPos + 1, descSepPos);
							String srcDesc = src.substring(descSepPos + (isField ? 1 : 0));

							if (isField && visitor.visitField(srcName, srcDesc)
									|| !isField && visitor.visitMethod(srcName, srcDesc)) {
								MappedElementKind kind = isField ? MappedElementKind.FIELD : MappedElementKind.METHOD;
								visitor.visitDstName(kind, 0, dstName);
								visitor.visitElementContent(kind);
							}
						}
					} else if (reader.nextCol("p")) { // package: p <name-a> = <name-b>
						// TODO
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

	private static void readSeparator(ColumnFileReader reader) throws IOException {
		if (!reader.nextCol("=")) {
			throw new IOException("missing separator in line "+reader.getLineNumber()+" (expected \" = \")");
		}
	}
}

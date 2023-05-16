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

package net.fabricmc.mappingio.format.match;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;

public class MatchFileReader {
	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(new ColumnFileReader(reader, '\t'), visitor);
	}

	private static void read(ColumnFileReader reader, MappingVisitor visitor) throws IOException {
		if (!reader.nextCol().startsWith("Matches saved")) {
			throw new IOException("invalid/unsupported match file: incorrect header");
		}

		if (visitor.getFlags().contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			reader.mark();
		}

		boolean escapeNames = false;

		for (;;) {
			boolean visitHeader = visitor.visitHeader();

			if (visitHeader) {
				visitor.visitNamespaces("a", Arrays.asList("b"));
			}

			if (visitor.visitContent()) {
				while (reader.nextLine(0)) {
					if (reader.nextCol("c")) { // class: c <name-a> <name-b>
						String srcName = reader.nextCol(escapeNames);
						if (srcName == null || srcName.isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());

						if (visitor.visitClass(srcName)) {
							readClass(reader, escapeNames, visitor);
						}
					}
				}
			}

			if (visitor.visitEnd()) break;

			reader.reset();
		}
	}

	private static void readClass(ColumnFileReader reader, boolean escapeNames, MappingVisitor visitor) throws IOException {
		visitor.visitDstName(MappedElementKind.CLASS, 0, reader.nextCol(escapeNames));
		if (!visitor.visitElementContent(MappedElementKind.CLASS)) return;

		while (reader.nextLine(1)) {
			boolean field = false;
			boolean method = false;

			if ((field = reader.nextCol("f")) || (method = reader.nextCol("m"))) {
				// field:  f <name-a;;desc-a> <name-b;;desc->
				// method: m <name-a desc-a> <name-b desc-b> (but no spaces between name and desc)

				String[] from;
				String[] to;

				if (field) {
					from = reader.nextCol(escapeNames).split(";;");
					to = reader.nextCol(escapeNames).split(";;");
				} else {
					from = toMethodArray(reader.nextCol(escapeNames));
					to = toMethodArray(reader.nextCol(escapeNames));
				}

				if (from.length != 2 || to.length != 2) throw new IOException("invalid member mapping in line "+reader.getLineNumber());

				String srcName = from[0];
				String srcDesc = from[1];
				String dstName = to[0];
				String dstDesc = to[1];

				if (field && visitor.visitField(srcName, srcDesc)) {
					visitor.visitDstName(MappedElementKind.FIELD, 0, dstName);
					visitor.visitDstDesc(MappedElementKind.FIELD, 0, dstDesc);
				} else if (method && visitor.visitMethod(srcName, srcDesc)) {
					visitor.visitDstName(MappedElementKind.METHOD, 0, dstName);
					visitor.visitDstDesc(MappedElementKind.METHOD, 0, dstDesc);
					readMethodContent(reader, escapeNames, visitor);
				}
			} else if (reader.nextCol("c")) { // comment: c <comment>
				readComment(reader, MappedElementKind.CLASS, visitor);
			}
		}
	}

	private static String[] toMethodArray(String nameWithDescriptor) {
		int parenPos = nameWithDescriptor.indexOf('(');
		return new String[] {
				nameWithDescriptor.substring(0, parenPos),
				nameWithDescriptor.substring(parenPos)
		};
	}

	private static void readMethodContent(ColumnFileReader reader, boolean escapeNames, MappingVisitor visitor) throws IOException {
		if (!visitor.visitElementContent(MappedElementKind.METHOD)) return;

		while (reader.nextLine(2)) {
			if (reader.nextCol("ma")) {
				// method arg: ma <arg-pos-a> <arg-pos-b>

				int srcArgPos = reader.nextIntCol();
				int dstArgPos = reader.nextIntCol();
				if (srcArgPos < 0 || dstArgPos < 0) throw new IOException("missing/invalid method arg position in line "+reader.getLineNumber());

				if (visitor.visitMethodArg(srcArgPos, -1, null)) {
					// TODO: Implement once per-element metadata is supported
				}
			} else if (reader.nextCol("c")) { // comment: c <comment>
				readComment(reader, MappedElementKind.METHOD, visitor);
			}
		}
	}

	private static void readComment(ColumnFileReader reader, MappedElementKind subjectKind, MappingVisitor visitor) throws IOException {
		String comment = reader.nextEscapedCol();
		if (comment == null) throw new IOException("missing comment in line "+reader.getLineNumber());

		visitor.visitComment(subjectKind, comment);
	}
}

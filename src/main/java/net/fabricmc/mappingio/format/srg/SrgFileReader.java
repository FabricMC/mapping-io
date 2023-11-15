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

package net.fabricmc.mappingio.format.srg;

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

public final class SrgFileReader {
	private SrgFileReader() {
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
		MappingFormat format = MappingFormat.SRG_FILE;

		if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS)) {
			parentVisitor = visitor;
			visitor = new MemoryMappingTree();
		} else if (flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			reader.mark();
		}

		for (;;) {
			boolean visitHeader = visitor.visitHeader();

			if (visitHeader) {
				visitor.visitNamespaces(sourceNs, Collections.singletonList(targetNs));
			}

			if (visitor.visitContent()) {
				String lastClass = null;
				boolean visitLastClass = false;

				do {
					boolean isMethod;

					if (reader.nextCol("CL:")) { // class: CL: <src> <dst>
						String srcName = reader.nextCol();
						if (srcName == null || srcName.isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());

						if (!srcName.equals(lastClass)) {
							lastClass = srcName;
							visitLastClass = visitor.visitClass(srcName);

							if (visitLastClass) {
								String dstName = reader.nextCol();
								if (dstName == null || dstName.isEmpty()) throw new IOException("missing class-name-b in line "+reader.getLineNumber());

								visitor.visitDstName(MappedElementKind.CLASS, 0, dstName);
								visitLastClass = visitor.visitElementContent(MappedElementKind.CLASS);
							}
						}
					} else if ((isMethod = reader.nextCol("MD:")) || reader.nextCol("FD:")) { // method: MD: <cls-a><name-a> <desc-a> <cls-b><name-b> <desc-b> or field: FD: <cls-a><name-a> <cls-b><name-b>
						String src = reader.nextCol();
						if (src == null) throw new IOException("missing class/name a in line "+reader.getLineNumber());

						int srcSepPos = src.lastIndexOf('/');
						if (srcSepPos <= 0 || srcSepPos == src.length() - 1) throw new IOException("invalid class/name a in line "+reader.getLineNumber());

						String[] cols = new String[3];

						for (int i = 0; i < 3; i++) {
							cols[i] = reader.nextCol();
						}

						if (!isMethod && cols[1] != null && cols[2] != null) format = MappingFormat.XSRG_FILE;
						String srcDesc;
						String dstName;
						String dstDesc;

						if (isMethod || format == MappingFormat.XSRG_FILE) {
							srcDesc = cols[0];
							if (srcDesc == null || srcDesc.isEmpty()) throw new IOException("missing desc a in line "+reader.getLineNumber());
							dstName = cols[1];
							dstDesc = cols[2];
							if (dstDesc == null || dstDesc.isEmpty()) throw new IOException("missing desc b in line "+reader.getLineNumber());
						} else {
							srcDesc = null;
							dstName = cols[0];
							dstDesc = null;
						}

						if (dstName == null) throw new IOException("missing class/name b in line "+reader.getLineNumber());

						int dstSepPos = dstName.lastIndexOf('/');
						if (dstSepPos <= 0 || dstSepPos == dstName.length() - 1) throw new IOException("invalid class/name b in line "+reader.getLineNumber());

						String srcOwner = src.substring(0, srcSepPos);

						if (!srcOwner.equals(lastClass)) {
							lastClass = srcOwner;
							visitLastClass = visitor.visitClass(srcOwner);

							if (visitLastClass) {
								visitor.visitDstName(MappedElementKind.CLASS, 0, dstName.substring(0, dstSepPos));
								visitLastClass = visitor.visitElementContent(MappedElementKind.CLASS);
							}
						}

						if (visitLastClass) {
							String srcName = src.substring(srcSepPos + 1);

							if (isMethod && visitor.visitMethod(srcName, srcDesc)
									|| !isMethod && visitor.visitField(srcName, srcDesc)) {
								MappedElementKind kind = isMethod ? MappedElementKind.METHOD : MappedElementKind.FIELD;
								visitor.visitDstName(kind, 0, dstName.substring(dstSepPos + 1));
								visitor.visitDstDesc(kind, 0, dstDesc);
								visitor.visitElementContent(kind);
							}
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

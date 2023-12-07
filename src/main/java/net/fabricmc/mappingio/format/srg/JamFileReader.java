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

/**
 * {@linkplain MappingFormat#JAM_FILE JAM file} reader.
 *
 * <p>Crashes if a second visit pass is requested without
 * {@link MappingFlag#NEEDS_MULTIPLE_PASSES} having been passed beforehand.
 */
public final class JamFileReader {
	private JamFileReader() {
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
			boolean visitHeader = visitor.visitHeader();

			if (visitHeader) {
				visitor.visitNamespaces(sourceNs, Collections.singletonList(targetNs));
			}

			if (visitor.visitContent()) {
				String lastClass = null;
				boolean visitLastClass = false;

				do {
					boolean isMethod;
					boolean isArg = false;

					if (reader.nextCol("CL")) { // class: CL <src> <dst>
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
					} else if ((isMethod = reader.nextCol("MD")) || reader.nextCol("FD") // method/field: MD/FD <cls-a> <name-a> <desc-a> <name-b>
							|| (isArg = reader.nextCol("MP"))) { // parameter: MP <cls-a> <mth-name-a> <mth-desc-a> <arg-pos> [<arg-desc-a>] <name-b>
						String clsSrcClsName = reader.nextCol();
						if (clsSrcClsName == null) throw new IOException("missing class-name-a in line "+reader.getLineNumber());

						String memberSrcName = reader.nextCol();
						if (memberSrcName == null || memberSrcName.isEmpty()) throw new IOException("missing member-name-a in line "+reader.getLineNumber());

						String memberSrcDesc = reader.nextCol();
						if (memberSrcDesc == null || memberSrcDesc.isEmpty()) throw new IOException("missing member-desc-a in line "+reader.getLineNumber());

						String col5 = reader.nextCol();
						String col6 = reader.nextCol();
						String col7 = reader.nextCol();

						int argSrcPos = -1;
						String dstName;
						String argSrcDesc;

						if (!isArg) {
							dstName = col5;
						} else {
							argSrcPos = Integer.parseInt(col5);

							if (col7 == null || col7.isEmpty()) {
								dstName = col6;
							} else {
								argSrcDesc = col6;
								if (argSrcDesc == null || argSrcDesc.isEmpty()) throw new IOException("missing parameter-desc-a in line "+reader.getLineNumber());

								dstName = col7;
							}
						}

						if (dstName == null || dstName.isEmpty()) throw new IOException("missing name-b in line "+reader.getLineNumber());

						if (!clsSrcClsName.equals(lastClass)) {
							lastClass = clsSrcClsName;
							visitLastClass = visitor.visitClass(clsSrcClsName);

							if (visitLastClass) {
								visitLastClass = visitor.visitElementContent(MappedElementKind.CLASS);
							}
						}

						if (!visitLastClass) continue;
						boolean visitMethod = false;

						if (isMethod || isArg) {
							visitMethod = visitor.visitMethod(memberSrcName, memberSrcDesc);
						}

						if (visitMethod) {
							if (isMethod) {
								visitor.visitDstName(MappedElementKind.METHOD, 0, dstName);
								visitor.visitElementContent(MappedElementKind.METHOD);
							} else {
								visitor.visitMethodArg(argSrcPos, -1, null);
								visitor.visitDstName(MappedElementKind.METHOD_ARG, 0, dstName);
								visitor.visitElementContent(MappedElementKind.METHOD_ARG);
							}
						} else if (!isMethod && !isArg && visitor.visitField(memberSrcName, memberSrcDesc)) {
							visitor.visitDstName(MappedElementKind.FIELD, 0, dstName);
							visitor.visitElementContent(MappedElementKind.FIELD);
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

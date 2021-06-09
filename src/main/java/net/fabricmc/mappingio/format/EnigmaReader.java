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
import java.util.Collections;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class EnigmaReader {
	public static void read(Path dir, MappingVisitor visitor) throws IOException {
		Set<MappingFlag> flags = visitor.getFlags();
		MappingVisitor parentVisitor = null;

		if (flags.contains(MappingFlag.NEEDS_UNIQUENESS) || flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			parentVisitor = visitor;
			visitor = new MemoryMappingTree();
		}

		boolean visitHeader = visitor.visitHeader();

		if (visitHeader) {
			visitor.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, Collections.singletonList(MappingUtil.NS_TARGET_FALLBACK));
		}

		if (visitor.visitContent()) {
			StringBuilder commentSb = new StringBuilder(200);
			final MappingVisitor finalVisitor = visitor;

			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.getFileName().toString().endsWith(".mapping")) {
						try (ColumnFileReader reader = new ColumnFileReader(Files.newBufferedReader(file), ' ')) {
							do {
								if (reader.nextCol("CLASS")) { // class: CLASS <name-a> [<name-b>]
									readClass(reader, 0, null, null, commentSb, finalVisitor);
								}
							} while (reader.nextLine(0));
						}
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}

		visitor.visitEnd();

		if (parentVisitor != null) {
			((MappingTree) visitor).accept(parentVisitor);
		}
	}

	private static void readClass(ColumnFileReader reader, int indent, String outerSrcClass, String outerDstClass, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		String srcName = reader.nextCol();
		if (srcName == null || srcName.isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());

		if (outerSrcClass != null && srcName.indexOf('$') < 0) {
			srcName = String.format("%s$%s", outerSrcClass, srcName);
		}

		String dstName = reader.nextCol();

		if (dstName != null && outerDstClass != null && dstName.indexOf('$') < 0) {
			dstName = String.format("%s$%s", outerDstClass, dstName);
		}

		readClassBody(reader, indent, srcName, dstName, commentSb, visitor);
	}

	private static void readClassBody(ColumnFileReader reader, int indent, String srcClass, String dstClass, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		boolean visited = false;
		int state = 0; // 0=invalid 1=visit -1=skip

		while (reader.nextLine(indent + 1)) {
			boolean isMethod;

			if (reader.nextCol("CLASS")) { // nested class: CLASS <name-a> [<name-b>]
				if (commentSb.length() > 0) {
					visitClass(srcClass, dstClass, state, commentSb, visitor);
					visited = true;
				}

				readClass(reader, indent + 1, srcClass, dstClass != null ? dstClass : srcClass, commentSb, visitor);
				state = 0;
			} else if (reader.nextCol("COMMENT")) { // comment: COMMENT <comment>
				readComment(reader, commentSb);
			} else if ((isMethod = reader.nextCol("METHOD")) || reader.nextCol("FIELD")) { // method: METHOD <name-a> [<name-b>] <desc-a> or field: FIELD <name-a> [<name-b>] <desc-a>
				state = visitClass(srcClass, dstClass, state, commentSb, visitor);
				visited = true;
				if (state < 0) continue;

				String srcName = reader.nextCol();
				if (srcName == null || srcName.isEmpty()) throw new IOException("missing field-name-a in line "+reader.getLineNumber());

				String dstNameOrSrcDesc = reader.nextCol();
				if (dstNameOrSrcDesc == null || dstNameOrSrcDesc.isEmpty()) throw new IOException("missing field-desc-b in line "+reader.getLineNumber());

				String srcDesc = reader.nextCol();
				String dstName;

				if (srcDesc == null) {
					dstName = null;
					srcDesc = dstNameOrSrcDesc;
				} else {
					dstName = dstNameOrSrcDesc;
				}

				if (isMethod && visitor.visitMethod(srcName, srcDesc)) {
					if (dstName != null) visitor.visitDstName(MappedElementKind.METHOD, 0, dstName);
					readMethod(reader, indent, commentSb, visitor);
				} else if (!isMethod && visitor.visitField(srcName, srcDesc)) {
					if (dstName != null) visitor.visitDstName(MappedElementKind.FIELD, 0, dstName);
					readElement(reader, MappedElementKind.FIELD, indent, commentSb, visitor);
				}
			}
		}

		if (!visited || commentSb.length() > 0) {
			visitClass(srcClass, dstClass, state, commentSb, visitor);
		}
	}

	/**
	 * Re-visit a class if necessary and visit its comment if available.
	 */
	private static int visitClass(String srcClass, String dstClass, int state, StringBuilder commentSb, MappingVisitor visitor) {
		// state: 0=invalid 1=visit -1=skip

		if (state == 0) {
			boolean visitContent = visitor.visitClass(srcClass);

			if (visitContent) {
				if (dstClass != null) visitor.visitDstName(MappedElementKind.CLASS, 0, dstClass);
				visitContent = visitor.visitElementContent(MappedElementKind.CLASS);
			}

			state = visitContent ? 1 : -1;

			if (commentSb.length() > 0) {
				if (state > 0) visitor.visitComment(MappedElementKind.CLASS, commentSb.toString());

				commentSb.setLength(0);
			}
		}

		return state;
	}

	private static void readMethod(ColumnFileReader reader, int indent, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		if (!visitor.visitElementContent(MappedElementKind.METHOD)) return;

		while (reader.nextLine(indent + 2)) {
			if (reader.nextCol("COMMENT")) { // comment: COMMENT <comment>
				readComment(reader, commentSb);
			} else {
				submitComment(MappedElementKind.METHOD, commentSb, visitor);

				if (reader.nextCol("ARG")) { // method parameter: ARG <lv-index> <name-b>
					int lvIndex = reader.nextIntCol();
					if (lvIndex < 0) throw new IOException("missing/invalid parameter lv-index in line "+reader.getLineNumber());

					if (visitor.visitMethodArg(-1, lvIndex, null)) {
						String dstName = reader.nextCol();
						if (dstName == null) throw new IOException("missing var-name-b column in line "+reader.getLineNumber());
						visitor.visitDstName(MappedElementKind.METHOD_ARG, 0, dstName);

						readElement(reader, MappedElementKind.METHOD_ARG, indent, commentSb, visitor);
					}
				}
			}
		}

		submitComment(MappedElementKind.METHOD, commentSb, visitor);
	}

	private static void readElement(ColumnFileReader reader, MappedElementKind kind, int indent, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		if (!visitor.visitElementContent(kind)) return;

		while (reader.nextLine(indent + kind.level + 1)) {
			if (reader.nextCol("COMMENT")) { // comment: COMMENT <comment>
				readComment(reader, commentSb);
			}
		}

		submitComment(kind, commentSb, visitor);
	}

	private static void readComment(ColumnFileReader reader, StringBuilder commentSb) throws IOException {
		if (commentSb.length() > 0) commentSb.append('\n');

		String comment = reader.nextCols(true);

		if (comment != null) {
			commentSb.append(comment);
		}
	}

	private static void submitComment(MappedElementKind kind, StringBuilder commentSb, MappingVisitor visitor) {
		if (commentSb.length() == 0) return;

		visitor.visitComment(kind, commentSb.toString());
		commentSb.setLength(0);
	}
}

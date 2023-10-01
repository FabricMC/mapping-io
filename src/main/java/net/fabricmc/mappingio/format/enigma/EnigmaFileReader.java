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
import java.io.Reader;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.format.StandardProperties;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class EnigmaFileReader {
	private EnigmaFileReader() {
	}

	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(reader, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor);
	}

	public static void read(Reader reader, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		read(new ColumnFileReader(reader, ' '), sourceNs, targetNs, visitor);
	}

	public static void read(ColumnFileReader reader, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
		Set<MappingFlag> flags = visitor.getFlags();
		MappingVisitor parentVisitor = null;

		if (flags.contains(MappingFlag.NEEDS_UNIQUENESS) || flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			parentVisitor = visitor;
			visitor = new MemoryMappingTree();
		}

		boolean visitHeader = visitor.visitHeader();

		if (visitHeader) {
			visitor.visitNamespaces(sourceNs, Collections.singletonList(targetNs));
		}

		if (visitor.visitContent()) {
			StringBuilder commentSb = new StringBuilder(200);
			final MappingVisitor finalVisitor = visitor;

			do {
				if (reader.nextCol("CLASS")) { // class: CLASS <name-a> [<name-b>] [<access-modifier>]
					readClass(reader, 0, null, null, commentSb, finalVisitor);
				}
			} while (reader.nextLine(0));
		}

		visitor.visitEnd();

		if (parentVisitor != null) {
			((MappingTree) visitor).accept(parentVisitor);
		}
	}

	private static void readClass(ColumnFileReader reader, int indent, String outerSrcClass, String outerDstClass, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		String line = reader.nextCols(false);
		String[] parts = line.split(" ");

		if (parts.length == 0 || parts[0].isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());
		String srcInnerName = parts[0];

		String srcName = srcInnerName;

		if (outerSrcClass != null && srcInnerName.indexOf('$') < 0) {
			srcName = String.format("%s$%s", outerSrcClass, srcInnerName);
		}

		String dstInnerName = null;
		String accessModifier = null;

		if (parts.length == 2) { // <name-b> | <access-modifier>
			String parsedModifier = parseModifier(parts[1]);

			if (parsedModifier == null) {
				dstInnerName = parts[1];
			} else {
				accessModifier = parsedModifier;
			}
		} else {
			dstInnerName = parts[1];
			accessModifier = parts[2];
		}

		String dstName = dstInnerName;

		// merge with outer name if available
		if (outerDstClass != null
				|| dstInnerName != null && outerSrcClass != null) {
			if (dstInnerName == null) dstInnerName = srcInnerName; // inner name is not mapped
			if (outerDstClass == null) outerDstClass = outerSrcClass; // outer name is not mapped

			dstName = String.format("%s$%s", outerDstClass, dstInnerName);
		}

		readClassBody(reader, indent, srcName, dstName, accessModifier, commentSb, visitor);
	}

	private static void readClassBody(ColumnFileReader reader, int indent, String srcClass, String dstClass,
			String classAccess, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		boolean visited = false;
		int state = 0; // 0=invalid 1=visit -1=skip

		while (reader.nextLine(indent + 1)) {
			boolean isMethod;

			if (reader.nextCol("CLASS")) { // nested class: CLASS <name-a> [<name-b>] [<access-modifier>]
				if (!visited || commentSb.length() > 0) {
					visitClass(srcClass, dstClass, state, classAccess, commentSb, visitor);
					visited = true;
				}

				readClass(reader, indent + 1, srcClass, dstClass, commentSb, visitor);
				state = 0;
			} else if (reader.nextCol("COMMENT")) { // comment: COMMENT <comment>
				readComment(reader, commentSb);
			} else if ((isMethod = reader.nextCol("METHOD")) || reader.nextCol("FIELD")) { // METHOD|FIELD <name-a> [<name-b>] [<access-modifier>] <desc-a>
				state = visitClass(srcClass, dstClass, state, classAccess, commentSb, visitor);
				visited = true;
				if (state < 0) continue;

				String line = reader.nextCols(false);
				String[] parts = line.split(" ");

				if (parts.length == 0 || parts[0].isEmpty()) throw new IOException("missing member-name-a in line "+reader.getLineNumber());
				if (parts.length == 1 || parts[1].isEmpty()) throw new IOException("missing member-desc-a in line "+reader.getLineNumber());
				String srcName = parts[0];
				String dstName = null;
				String modifier = null;
				String srcDesc;

				if (parts.length == 2) { // <name-a> <desc-a>
					srcDesc = parts[1];
				} else if (parts.length == 3) { // <name-a> <name-b> <desc-a> | <name-a> <desc-a> <access-modifier>
					String parsedModifier = parseModifier(parts[2]);

					if (parsedModifier == null) {
						dstName = parts[1];
						srcDesc = parts[2];
					} else {
						srcDesc = parts[1];
						modifier = parsedModifier;
					}
				} else { // <name-a> <name-b> <desc-a> <access-modifier>
					dstName = parts[1];
					srcDesc = parts[2];
					modifier = parts[3];
				}

				MappedElementKind targetKind = isMethod && visitor.visitMethod(srcName, srcDesc) ? MappedElementKind.METHOD
						: !isMethod && visitor.visitField(srcName, srcDesc) ? MappedElementKind.FIELD : null;

				if (targetKind != null) {
					if (dstName != null && !dstName.isEmpty()) visitor.visitDstName(targetKind, 0, dstName);
					if (modifier != null) visitAccessModifier(targetKind, modifier, visitor);

					if (targetKind == MappedElementKind.METHOD) {
						readMethod(reader, indent, commentSb, visitor);
					} else {
						readElement(reader, targetKind, indent, commentSb, visitor);
					}
				}
			}
		}

		if (!visited || commentSb.length() > 0) {
			visitClass(srcClass, dstClass, state, classAccess, commentSb, visitor);
		}
	}

	/**
	 * Re-visit a class if necessary and visit its comment if available.
	 */
	private static int visitClass(String srcClass, String dstClass, int state, String accessModifier, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		// state: 0=invalid 1=visit -1=skip

		if (state == 0) {
			boolean visitContent = visitor.visitClass(srcClass);

			if (visitContent) {
				if (dstClass != null && !dstClass.isEmpty()) visitor.visitDstName(MappedElementKind.CLASS, 0, dstClass);
				visitContent = visitor.visitElementContent(MappedElementKind.CLASS);
			}

			state = visitContent ? 1 : -1;

			if (accessModifier != null) {
				visitAccessModifier(MappedElementKind.CLASS, accessModifier, visitor);
			}

			if (commentSb.length() > 0) {
				if (state > 0) visitor.visitComment(MappedElementKind.CLASS, commentSb.toString());

				commentSb.setLength(0);
			}
		}

		return state;
	}

	private static void visitAccessModifier(MappedElementKind targetKind, String modifier, MappingVisitor visitor) throws IOException {
		visitor.visitElementMetadata(targetKind, StandardProperties.MODIFIED_ACCESS.getId(), 0, modifier);
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
						if (!dstName.isEmpty()) visitor.visitDstName(MappedElementKind.METHOD_ARG, 0, dstName);

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

	private static void submitComment(MappedElementKind kind, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
		if (commentSb.length() == 0) return;

		visitor.visitComment(kind, commentSb.toString());
		commentSb.setLength(0);
	}

	@Nullable
	private static String parseModifier(String token) {
		if (!token.startsWith("ACC:")) {
			return null;
		}

		return token.substring(4).toLowerCase(Locale.ROOT);
	}
}

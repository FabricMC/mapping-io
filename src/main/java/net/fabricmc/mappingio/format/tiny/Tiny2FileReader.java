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

package net.fabricmc.mappingio.format.tiny;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.format.ErrorSink;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.ParsingError.Severity;

/**
 * {@linkplain MappingFormat#TINY_2_FILE Tiny v2 file} reader.
 *
 * <p>Crashes if a second visit pass is requested without
 * {@link MappingFlag#NEEDS_MULTIPLE_PASSES} having been passed beforehand.
 */
public final class Tiny2FileReader {
	private Tiny2FileReader() {
	}

	public static List<String> getNamespaces(Reader reader) throws IOException {
		return getNamespaces(new ColumnFileReader(reader, '\t', '\t'));
	}

	private static List<String> getNamespaces(ColumnFileReader reader) throws IOException {
		if (!reader.nextCol("tiny") // magic
				|| reader.nextIntCol(true) != 2 // major version
				|| reader.nextIntCol(true) < 0) { // minor version
			throw new IOException("invalid/unsupported tiny file: no tiny 2 header");
		}

		List<String> ret = new ArrayList<>();
		String ns;

		while ((ns = reader.nextCol()) != null) {
			ret.add(ns);
		}

		return ret;
	}

	@Deprecated
	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(new ColumnFileReader(reader, '\t', '\t'), visitor, ErrorSink.throwingOnSeverity(Severity.WARNING));
	}

	public static void read(Reader reader, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		read(new ColumnFileReader(reader, '\t', '\t'), visitor, errorSink);
	}

	private static void read(ColumnFileReader reader, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		if (!reader.nextCol("tiny") // magic
				|| reader.nextIntCol(true) != 2 // major version
				|| reader.nextIntCol(true) < 0) { // minor version
			throw new IOException("invalid/unsupported tiny file: no tiny 2 header");
		}

		String srcNamespace = reader.nextCol();
		List<String> dstNamespaces = new ArrayList<>();
		String dstNamespace;

		while ((dstNamespace = reader.nextCol()) != null) {
			dstNamespaces.add(dstNamespace);
		}

		int dstNsCount = dstNamespaces.size();
		boolean readerMarked = false;

		if (visitor.getFlags().contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			reader.mark();
			readerMarked = true;
		}

		boolean firstIteration = true;
		boolean escapeNames = false;

		for (;;) {
			boolean visitHeader = visitor.visitHeader();

			if (visitHeader) {
				visitor.visitNamespaces(srcNamespace, dstNamespaces);
			}

			if (visitHeader || firstIteration) {
				while (reader.nextLine(1)) {
					if (!visitHeader) {
						if (!escapeNames && reader.nextCol(Tiny2Util.escapedNamesProperty)) {
							escapeNames = true;
						}
					} else {
						String key = reader.nextCol();

						if (key == null) {
							errorSink.addError("missing property key in line "+reader.getLineNumber());
							continue;
						}

						String value = reader.nextCol(true); // may be missing -> null

						if (key.equals(Tiny2Util.escapedNamesProperty)) {
							escapeNames = true;
						}

						visitor.visitMetadata(key, value);
					}
				}
			}

			if (visitor.visitContent()) {
				while (reader.nextLine(0)) {
					if (reader.nextCol("c")) { // class: c <names>...
						String srcName = reader.nextCol(escapeNames);

						if (srcName == null || srcName.isEmpty()) {
							errorSink.addError("missing class-name-a in line "+reader.getLineNumber());
							continue;
						}

						if (visitor.visitClass(srcName)) {
							readClass(reader, dstNsCount, escapeNames, visitor, errorSink);
						}
					}
				}
			}

			if (visitor.visitEnd()) break;

			if (!readerMarked) {
				throw new IllegalStateException("repeated visitation requested without NEEDS_MULTIPLE_PASSES");
			}

			firstIteration = false;
			int markIdx = reader.reset();
			assert markIdx == 1;
		}
	}

	private static void readClass(ColumnFileReader reader, int dstNsCount, boolean escapeNames, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		readDstNames(reader, MappedElementKind.CLASS, dstNsCount, escapeNames, visitor, errorSink);
		if (!visitor.visitElementContent(MappedElementKind.CLASS)) return;

		while (reader.nextLine(1)) {
			if (reader.nextCol("f")) { // field: f <descA> <names>...
				String srcDesc = reader.nextCol(escapeNames);

				if (srcDesc == null || srcDesc.isEmpty()) {
					errorSink.addError("missing field-desc-a in line "+reader.getLineNumber());
					continue;
				}

				String srcName = reader.nextCol(escapeNames);

				if (srcName == null || srcName.isEmpty()) {
					errorSink.addError("missing field-name-a in line "+reader.getLineNumber());
					continue;
				}

				if (visitor.visitField(srcName, srcDesc)) {
					readElement(reader, MappedElementKind.FIELD, dstNsCount, escapeNames, visitor, errorSink);
				}
			} else if (reader.nextCol("m")) { // method: m <descA> <names>...
				String srcDesc = reader.nextCol(escapeNames);

				if (srcDesc == null || srcDesc.isEmpty()) {
					errorSink.addError("missing method-desc-a in line "+reader.getLineNumber());
					continue;
				}

				String srcName = reader.nextCol(escapeNames);

				if (srcName == null || srcName.isEmpty()) {
					errorSink.addError("missing method-name-a in line "+reader.getLineNumber());
					continue;
				}

				if (visitor.visitMethod(srcName, srcDesc)) {
					readMethod(reader, dstNsCount, escapeNames, visitor, errorSink);
				}
			} else if (reader.nextCol("c")) { // comment: c <comment>
				readComment(reader, MappedElementKind.CLASS, visitor, errorSink);
			}
		}
	}

	private static void readMethod(ColumnFileReader reader, int dstNsCount, boolean escapeNames, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		readDstNames(reader, MappedElementKind.METHOD, dstNsCount, escapeNames, visitor, errorSink);
		if (!visitor.visitElementContent(MappedElementKind.METHOD)) return;

		while (reader.nextLine(2)) {
			if (reader.nextCol("p")) { // method parameter: p <lv-index> <names>...
				int lvIndex = -1;

				try {
					lvIndex = reader.nextIntCol(false);
				} catch (NumberFormatException e) {
					errorSink.addError("invalid parameter lv-index in line "+reader.getLineNumber());
					continue;
				}

				if (lvIndex < 0) {
					errorSink.addWarning("missing/invalid parameter lv-index in line "+reader.getLineNumber());
					lvIndex = -1;
				}

				String srcName = reader.nextCol(escapeNames);

				if (srcName == null) {
					errorSink.addWarning("missing arg-name-a column in line "+reader.getLineNumber());
				} else if (srcName.isEmpty()) {
					srcName = null;
				}

				if (lvIndex == -1 && srcName == null) continue;

				if (visitor.visitMethodArg(-1, lvIndex, srcName)) {
					readElement(reader, MappedElementKind.METHOD_ARG, dstNsCount, escapeNames, visitor, errorSink);
				}
			} else if (reader.nextCol("v")) { // method variable: v <lv-index> <lv-start-offset> <optional-lvt-index> <names>...
				int lvIndex = -1;

				try {
					lvIndex = reader.nextIntCol(false);
				} catch (NumberFormatException e) {
					errorSink.addError("invalid variable lv-index in line "+reader.getLineNumber());
					continue;
				}

				if (lvIndex < 0) {
					errorSink.addWarning("missing/invalid variable lv-index in line "+reader.getLineNumber());
					lvIndex = -1;
				}

				int startOpIdx = -1;

				try {
					startOpIdx = reader.nextIntCol(false);
				} catch (NumberFormatException e) {
					errorSink.addError("invalid variable lv-start-offset in line "+reader.getLineNumber());
					continue;
				}

				if (startOpIdx < 0) {
					errorSink.addWarning("missing/invalid variable lv-start-offset in line "+reader.getLineNumber());
					startOpIdx = -1;
				}

				int lvtRowIndex = -1;

				try {
					lvtRowIndex = reader.nextIntCol(false);
				} catch (NumberFormatException e) {
					errorSink.addError("invalid variable lvt-index in line "+reader.getLineNumber());
					continue;
				}

				if (lvtRowIndex < -1) {
					errorSink.addWarning("invalid variable lvt-index in line "+reader.getLineNumber());
					lvtRowIndex = -1;
				}

				String srcName = null;

				if (reader.isAtEol()) {
					errorSink.addWarning("missing var-name columns in line "+reader.getLineNumber());
				} else if ((srcName = reader.nextCol(escapeNames)).isEmpty()) {
					srcName = null;
				}

				if (lvIndex == -1 && startOpIdx == -1 && srcName == null) continue;

				if (visitor.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, -1, srcName)) {
					readElement(reader, MappedElementKind.METHOD_VAR, dstNsCount, escapeNames, visitor, errorSink);
				}
			} else if (reader.nextCol("c")) { // comment: c <comment>
				readComment(reader, MappedElementKind.METHOD, visitor, errorSink);
			}
		}
	}

	private static void readElement(ColumnFileReader reader, MappedElementKind kind, int dstNsCount, boolean escapeNames, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		readDstNames(reader, kind, dstNsCount, escapeNames, visitor, errorSink);
		if (!visitor.visitElementContent(kind)) return;

		while (reader.nextLine(kind.level + 1)) {
			if (reader.nextCol("c")) { // comment: c <comment>
				readComment(reader, kind, visitor, errorSink);
			}
		}
	}

	private static void readComment(ColumnFileReader reader, MappedElementKind subjectKind, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		String comment = reader.nextCol(true);

		if (comment == null) {
			errorSink.addWarning("missing comment in line "+reader.getLineNumber());
			return;
		}

		visitor.visitComment(subjectKind, comment);
	}

	private static void readDstNames(ColumnFileReader reader, MappedElementKind subjectKind, int dstNsCount, boolean escapeNames, MappingVisitor visitor, ErrorSink errorSink) throws IOException {
		for (int dstNs = 0; dstNs < dstNsCount; dstNs++) {
			String name = reader.nextCol(escapeNames);

			if (name == null) {
				errorSink.addWarning("missing name columns in line "+reader.getLineNumber());
				break;
			}

			if (!name.isEmpty()) visitor.visitDstName(subjectKind, dstNs, name);
		}
	}
}

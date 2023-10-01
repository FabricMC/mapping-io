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
import java.io.Writer;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.StandardProperties;

abstract class EnigmaWriterBase implements MappingWriter {
	EnigmaWriterBase(Writer writer) throws IOException {
		this.writer = writer;
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return flags;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) { }

	@Override
	public boolean visitClass(String srcName) throws IOException {
		writePendingElementMetadata();
		srcClassName = srcName;
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		writePendingElementMetadata();
		writeIndent(0);
		writer.write("FIELD ");
		writer.write(srcName);

		desc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		writePendingElementMetadata();
		writeIndent(0);
		writer.write("METHOD ");
		writer.write(srcName);

		desc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException {
		writePendingElementMetadata();
		writeIndent(1);
		writer.write("ARG ");
		writer.write(Integer.toString(lvIndex));

		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
		writePendingElementMetadata();
		return false;
	}

	@Override
	public boolean visitEnd() throws IOException {
		writePendingElementMetadata();
		close();

		return true;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (namespace != 0) return;

		if (targetKind == MappedElementKind.CLASS) {
			dstName = name;
		} else {
			writer.write(' ');
			writer.write(name);
		}
	}

	@Override
	public abstract boolean visitElementContent(MappedElementKind targetKind) throws IOException;

	@Override
	public void visitElementMetadata(MappedElementKind target, String key, int namespace, String value) {
		if (namespace == 0) elementMetadata.put(key, value);
	}

	protected static int getNextOuterEnd(String name, int startPos) {
		int pos;

		while ((pos = name.indexOf('$', startPos + 1)) > 0) {
			if (name.charAt(pos - 1) != '/') return pos;
			startPos = pos + 1;
		}

		return -1;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		writePendingElementMetadata();
		int start = 0;
		int pos;

		do {
			if (start >= comment.length()) break;
			pos = comment.indexOf('\n', start);
			int end = pos >= 0 ? pos : comment.length();

			writeIndent(targetKind.level);
			writer.write("COMMENT");

			if (end > start) {
				writer.write(' ');

				for (int i = start; i < end; i++) {
					char c = comment.charAt(i);
					int idx = toEscape.indexOf(c);

					if (idx >= 0) {
						if (i > start) writer.write(comment, start, i - start);
						writer.write('\\');
						writer.write(escaped.charAt(idx));
						start = i + 1;
					}
				}

				if (start < end) writer.write(comment, start, end - start);
			}

			writer.write('\n');

			start = end + 1;
		} while (pos >= 0);
	}

	protected void writeMismatchedOrMissingClasses() throws IOException {
		indent = 0;
		int srcStart = 0;

		do {
			int srcEnd = getNextOuterEnd(srcClassName, srcStart);
			if (srcEnd < 0) srcEnd = srcClassName.length();
			int srcLen = srcEnd - srcStart;

			if (!lastWrittenClass.regionMatches(srcStart, srcClassName, srcStart, srcLen) // writtenPart.startsWith(srcPart)
					|| srcEnd < lastWrittenClass.length() && lastWrittenClass.charAt(srcEnd) != '$') { // no trailing characters in writtenPart -> startsWith = equals
				writeIndent(0);
				writer.write("CLASS ");
				writer.write(srcClassName, srcStart, srcLen);

				if (dstName != null) {
					int dstStart = 0;

					for (int i = 0; i < indent; i++) {
						dstStart = getNextOuterEnd(dstName, dstStart);
						if (dstStart < 0) break;
						dstStart++;
					}

					if (dstStart >= 0) {
						int dstEnd = getNextOuterEnd(dstName, dstStart);
						if (dstEnd < 0) dstEnd = dstName.length();
						int dstLen = dstEnd - dstStart;

						if (dstLen != srcLen || !srcClassName.regionMatches(srcStart, dstName, dstStart, srcLen)) { // src != dst
							writer.write(' ');
							writer.write(dstName, dstStart, dstLen);
						}
					}
				}

				writer.write('\n');
			}

			indent++;
			srcStart = srcEnd + 1;
		} while (srcStart < srcClassName.length());

		lastWrittenClass = srcClassName;
		dstName = null;
	}

	protected void writePendingElementMetadata() throws IOException {
		if (!elementMetadata.isEmpty()) {
			for (Map.Entry<String, String> entry : elementMetadata.entrySet()) {
				String key = entry.getKey();
				if (StandardProperties.getById(key) != StandardProperties.MODIFIED_ACCESS) continue;

				writer.write(key);
				writer.write(" ACC:");
				writer.write(entry.getValue().toUpperCase(Locale.ROOT));
				break;
			}

			elementMetadata.clear();
		}

		writer.write('\n');
	}

	protected void writeIndent(int extra) throws IOException {
		for (int i = 0; i < indent + extra; i++) {
			writer.write('\t');
		}
	}

	protected static final Set<MappingFlag> flags = EnumSet.of(
			MappingFlag.NEEDS_HEADER_METADATA,
			MappingFlag.NEEDS_UNIQUENESS,
			MappingFlag.NEEDS_SRC_FIELD_DESC,
			MappingFlag.NEEDS_SRC_METHOD_DESC);
	protected static final String toEscape = "\\\n\r\0\t";
	protected static final String escaped = "\\nr0t";
	protected static final LinkedHashMap<String, String> elementMetadata = new LinkedHashMap<>();

	protected Writer writer;
	protected int indent;

	protected String srcClassName;
	protected String currentClass;
	protected String lastWrittenClass = "";
	protected String dstName;

	protected String desc;
}

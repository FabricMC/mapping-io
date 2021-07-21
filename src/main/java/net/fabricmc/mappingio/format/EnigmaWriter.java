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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;

public final class EnigmaWriter implements MappingWriter {
	public EnigmaWriter(Path dir, boolean deleteExistingFiles) throws IOException {
		this.dir = dir.toAbsolutePath().normalize();

		if (deleteExistingFiles && Files.exists(dir)) {
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.getFileName().toString().endsWith(".mapping")) {
						Files.delete(file);
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path file, IOException exc) throws IOException {
					try {
						if (!dir.equals(file)) Files.delete(file);
					} catch (DirectoryNotEmptyException e) {
						// ignore
					}

					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	@Override
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
			writer = null;
			writerClass = null;
		}
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return flags;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) { }

	@Override
	public boolean visitClass(String srcName) {
		srcClassName = srcName;
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		writeIndent(0);
		writer.write("FIELD ");
		writer.write(srcName);

		desc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		writeIndent(0);
		writer.write("METHOD ");
		writer.write(srcName);

		desc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException {
		writeIndent(1);
		writer.write("ARG ");
		writer.write(Integer.toString(lvIndex));

		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName) {
		return false;
	}

	@Override
	public boolean visitEnd() throws IOException {
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
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (targetKind == MappedElementKind.CLASS) {
			String name = dstName != null ? dstName : srcClassName;

			if (writerClass == null
					|| !name.startsWith(writerClass)
					|| name.length() > writerClass.length() && name.charAt(writerClass.length()) != '$') {
				int pos = getNextOuterEnd(name, 0);
				if (pos >= 0) name = name.substring(0, pos);

				// currentClass is not an outer class of srcName (or the same)
				Path file = dir.resolve(name+".mapping").normalize();
				if (!file.startsWith(dir)) throw new RuntimeException("invalid name: "+name);

				if (writer != null) {
					writer.close();
				}

				writerClass = name;

				if (Files.exists(file)) {
					// initialize writtenClass with last CLASS entry

					List<String> writtenClassParts = new ArrayList<>();

					try (BufferedReader reader = Files.newBufferedReader(file)) {
						String line;

						while ((line = reader.readLine()) != null) {
							int offset = 0;

							while (offset < line.length() && line.charAt(offset) == '\t') {
								offset++;
							}

							if (line.startsWith("CLASS ", offset)) {
								int start = offset + 6;
								int end = line.indexOf(' ', start);
								if (end < 0) end = line.length();
								String part = line.substring(start, end);

								while (writtenClassParts.size() > offset) {
									writtenClassParts.remove(writtenClassParts.size() - 1);
								}

								writtenClassParts.add(part);
							}
						}
					}

					writtenClass = String.join("$", writtenClassParts);
				} else {
					writtenClass = "";
					Files.createDirectories(file.getParent());
				}

				writer = Files.newBufferedWriter(file, StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			}

			// write mismatched/missing class parts

			indent = 0;
			int srcStart = 0;

			do {
				int srcEnd = getNextOuterEnd(srcClassName, srcStart);
				if (srcEnd < 0) srcEnd = srcClassName.length();
				int srcLen = srcEnd - srcStart;

				if (!writtenClass.regionMatches(srcStart, srcClassName, srcStart, srcLen) // writtenPart.startsWith(srcPart)
						|| srcEnd < writtenClass.length() && writtenClass.charAt(srcEnd) != '$') { // no trailing characters in writtenPart -> startsWith = equals
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

			writtenClass = srcClassName;
			dstName = null;
		} else if (targetKind == MappedElementKind.FIELD || targetKind == MappedElementKind.METHOD) {
			writer.write(' ');
			writer.write(desc);
			writer.write('\n');
		} else {
			writer.write('\n');
		}

		return true;
	}

	private static int getNextOuterEnd(String name, int startPos) {
		int pos;

		while ((pos = name.indexOf('$', startPos + 1)) > 0) {
			if (name.charAt(pos - 1) != '/') return pos;
			startPos = pos + 1;
		}

		return -1;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
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

	private void writeIndent(int extra) throws IOException {
		for (int i = 0; i < indent + extra; i++) {
			writer.write('\t');
		}
	}

	private static final Set<MappingFlag> flags = EnumSet.of(MappingFlag.NEEDS_UNIQUENESS, MappingFlag.NEEDS_SRC_FIELD_DESC, MappingFlag.NEEDS_SRC_METHOD_DESC);
	private static final String toEscape = "\\\n\r\0\t";
	private static final String escaped = "\\nr0t";

	private final Path dir;

	private Writer writer;
	private String writerClass;
	private String writtenClass;
	private int indent;

	private String srcClassName;
	private String dstName;

	private String desc;
}

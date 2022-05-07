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
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;

public final class JsonWriter implements MappingWriter {
	public JsonWriter(Writer writer) {
		this.writer = writer;
		this.firstMetadata = true;
		this.firstClass = true;
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return flags;
	}

	@Override
	public boolean visitHeader() throws IOException {
		// open root
		write("{");

		writeLn();
		writeTab();
		writeKey("version");
		write("1,");

		return true;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		names = new String[dstNamespaces.size() + 1];

		writeLn();
		writeTab();
		writeKey("namespaces");
		write("[");

		writeJsonString(srcNamespace);

		for (String dstNamespace : dstNamespaces) {
			write(", ");
			writeJsonString(dstNamespace);
		}

		write("],");
	}

	@Override
	public void visitMetadata(String key, String value) throws IOException {
		if (firstMetadata) {
			// open metadata
			writeLn();
			writeTab();
			writeKey("meta");
			write("{");

			firstMetadata = false;
		} else {
			write(",");
		}

		writeLn();
		writeTabs(2);
		writeKey(key);
		writeJsonString(value);
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		names[0] = srcName;

		if (firstClass) {
			if (!firstMetadata) {
				// close metadata
				writeLn();
				writeTab();
				write("},");
			}

			// open classes array
			writeLn();
			writeTab();
			writeKey("classes");
			write("[");

			firstClass = false;
		} else {
			if (!firstField || !firstMethod) {
				if (!firstMethod && (!firstMethodArg || !firstMethodVar)) {
					// close last method arg/var entry
					writeLn();
					writeTabs(6);
					write("}");

					// close args/vars array
					writeLn();
					writeTabs(5);
					write("]");
				}

				// close last field/method entry
				writeLn();
				writeTabs(4);
				write("}");

				// close fields/methods array
				writeLn();
				writeTabs(3);
				write("]");
			}

			// close class entry
			writeLn();
			writeTabs(2);
			write("},");
		}

		// open class entry
		writeLn();
		writeTabs(2);
		write("{");

		firstField = true;
		firstMethod = true;

		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		names[0] = srcName;

		if (firstField) {
			write(",");

			// open fields array
			writeLn();
			writeTabs(3);
			writeKey("fields");
			write("[");

			firstField = false;
		} else {
			// close field entry
			writeLn();
			writeTabs(4);
			write("},");
		}

		// open field entry
		writeLn();
		writeTabs(4);
		write("{");

		writeLn();
		writeTabs(5);
		writeKey("desc");
		writeJsonString(srcDesc);
		write(",");

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		names[0] = srcName;

		if (firstMethod) {
			if (!firstField) {
				// close last field entry
				writeLn();
				writeTabs(4);
				write("}");

				// close fields array
				writeLn();
				writeTabs(3);
				write("],");
			} else {
				write(",");
			}

			// open methods array
			writeLn();
			writeTabs(3);
			writeKey("methods");
			write("[");

			firstMethod = false;
		} else {
			if (!firstMethodArg || !firstMethodVar) {
				// close last method arg/var entry
				writeLn();
				writeTabs(6);
				write("}");

				// close args/vars array
				writeLn();
				writeTabs(5);
				write("]");
			}

			// close method entry
			writeLn();
			writeTabs(4);
			write("},");
		}

		// open method entry
		writeLn();
		writeTabs(4);
		write("{");

		writeLn();
		writeTabs(5);
		writeKey("desc");
		writeJsonString(srcDesc);
		write(",");

		firstMethodArg = true;
		firstMethodVar = true;

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException {
		names[0] = srcName;

		if (firstMethodArg) {
			// close method args array
			write(",");
			writeLn();
			writeTabs(5);
			writeKey("parameters");
			write("[");

			firstMethodArg = false;
		} else {
			// close method arg entry
			writeLn();
			writeTabs(6);
			write("},");
		}

		// open method arg entry
		writeLn();
		writeTabs(6);
		write("{");

		writeLn();
		writeTabs(7);
		writeKey("lvIndex");
		write(lvIndex);
		write(",");

		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName) throws IOException {
		names[0] = srcName;

		if (firstMethodVar) {
			// close method vars array
			write(",");
			writeLn();
			writeTabs(5);
			writeKey("variables");
			write("[");

			firstMethodVar = false;
		} else {
			// close method var entry
			writeLn();
			writeTabs(6);
			write("},");
		}

		// open method var entry
		writeLn();
		writeTabs(6);
		write("{");

		writeLn();
		writeTabs(7);
		writeKey("lvIndex");
		write(lvIndex);
		write(",");

		writeLn();
		writeTabs(7);
		writeKey("lvStartOffset");
		write(startOpIdx);
		write(",");

		writeLn();
		writeTabs(7);
		writeKey("lvtIndex");
		write(lvtRowIndex);

		return true;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		names[namespace + 1] = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		writeLn();
		writeTabs(targetKind.level * 2 + 3);
		writeKey("name");

		write("[");

		boolean firstName = true;

		for (String name : names) {
			if (!firstName) {
				write(", ");
			}

			firstName = false;

			if (name == null) {
				write("null");
			} else {
				writeJsonString(name);
			}
		}

		write("]");

		Arrays.fill(names, null);

		return true;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		write(",");
		writeLn();
		writeTabs(targetKind.level * 2 + 3);
		writeKey("comment");
		writeJsonString(comment);
	}

	@Override
	public void close() throws IOException {
		if (!firstMethod && (!firstMethodArg || !firstMethodVar)) {
			// close last method arg/var entry
			writeLn();
			writeTabs(6);
			write("}");

			// close args/vars array
			writeLn();
			writeTabs(5);
			write("]");
		}

		if (!firstField || !firstMethod) {
			// close last field/method entry
			writeLn();
			writeTabs(4);
			write("}");

			// close field/method array
			writeLn();
			writeTabs(3);
			write("]");
		}

		if (!firstClass) {
			// close last class entry
			writeLn();
			writeTabs(2);
			write("}");

			// close classes array
			writeLn();
			writeTab();
			write("]");
		}

		// close root
		writeLn();
		write("}");

		writer.close();
	}

	private void write(String str) throws IOException {
		writer.write(str);
	}

	private void write(int i) throws IOException {
		write(Integer.toString(i));
	}

	private void writeJsonString(String str) throws IOException {
		writer.write("\"");
		JsonUtil.writeEscaped(str, writer);
		writer.write("\"");
	}

	private void writeKey(String key) throws IOException {
		writeJsonString(key);
		write(": ");
	}

	private void writeLn() throws IOException {
		writer.write('\n');
	}

	private void writeTab() throws IOException {
		writer.write('\t');
	}

	private void writeTabs(int count) throws IOException {
		for (int i = 0; i < count; i++) {
			writer.write('\t');
		}
	}

	private static final Set<MappingFlag> flags = EnumSet.of(MappingFlag.NEEDS_HEADER_METADATA, MappingFlag.NEEDS_UNIQUENESS, MappingFlag.NEEDS_SRC_FIELD_DESC, MappingFlag.NEEDS_SRC_METHOD_DESC);

	private final Writer writer;
	private String[] names;
	private boolean firstMetadata;
	private boolean firstClass;
	private boolean firstField;
	private boolean firstMethod;
	private boolean firstMethodArg;
	private boolean firstMethodVar;
}

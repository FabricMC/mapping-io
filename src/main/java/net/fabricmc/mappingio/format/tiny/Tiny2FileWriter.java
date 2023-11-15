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
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;

public final class Tiny2FileWriter implements MappingWriter {
	public Tiny2FileWriter(Writer writer, boolean escapeNames) {
		this.writer = writer;
		this.escapeNames = escapeNames;
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
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		dstNames = new String[dstNamespaces.size()];

		write("tiny\t2\t0\t");
		write(srcNamespace);

		for (String dstNamespace : dstNamespaces) {
			writeTab();
			write(dstNamespace);
		}

		writeLn();
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		if (key.equals(Tiny2Util.escapedNamesProperty)) {
			escapeNames = true;
			wroteEscapedNamesProperty = true;
		}

		writeTab();
		write(key);

		if (value != null) {
			writeTab();
			write(value);
		}

		writeLn();
	}

	@Override
	public boolean visitContent() throws IOException {
		if (escapeNames && !wroteEscapedNamesProperty) {
			write("\t");
			write(Tiny2Util.escapedNamesProperty);
			writeLn();
		}

		return true;
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		write("c\t");
		writeName(srcName);

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		write("\tf\t");
		writeName(srcDesc);
		writeTab();
		writeName(srcName);

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		write("\tm\t");
		writeName(srcDesc);
		writeTab();
		writeName(srcName);

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		write("\t\tp\t");
		write(lvIndex);
		writeTab();
		if (srcName != null) writeName(srcName);

		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		write("\t\tv\t");
		write(lvIndex);
		writeTab();
		write(startOpIdx);
		writeTab();
		write(Math.max(lvtRowIndex, -1));
		writeTab();
		if (srcName != null) writeName(srcName);

		return true;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		dstNames[namespace] = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		for (String dstName : dstNames) {
			writeTab();
			if (dstName != null) writeName(dstName);
		}

		writeLn();

		Arrays.fill(dstNames, null);

		return true;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		writeTabs(targetKind.level);
		write("\tc\t");
		writeEscaped(comment);
		writeLn();
	}

	private void write(String str) throws IOException {
		writer.write(str);
	}

	private void write(int i) throws IOException {
		write(Integer.toString(i));
	}

	private void writeEscaped(String str) throws IOException {
		Tiny2Util.writeEscaped(str, writer);
	}

	private void writeName(String str) throws IOException {
		if (escapeNames) {
			writeEscaped(str);
		} else {
			write(str);
		}
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

	private static final Set<MappingFlag> flags = EnumSet.of(
			MappingFlag.NEEDS_HEADER_METADATA,
			MappingFlag.NEEDS_METADATA_UNIQUENESS,
			MappingFlag.NEEDS_ELEMENT_UNIQUENESS,
			MappingFlag.NEEDS_SRC_FIELD_DESC,
			MappingFlag.NEEDS_SRC_METHOD_DESC);

	private final Writer writer;
	private boolean escapeNames;
	private boolean wroteEscapedNamesProperty;
	private String[] dstNames;
}

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
import java.io.Writer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;

/**
 * {@linkplain MappingFormat#TSRG_FILE TSRG file} and
 * {@linkplain MappingFormat#TSRG_2_FILE TSRG2 file} writer.
 */
public final class TsrgFileWriter implements MappingWriter {
	public TsrgFileWriter(Writer writer, boolean tsrg2) {
		this.writer = writer;
		this.tsrg2 = tsrg2;
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return tsrg2 ? tsrg2Flags : tsrgFlags;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		dstNames = new String[dstNamespaces.size()];

		if (tsrg2) {
			write("tsrg2 ");
			write(srcNamespace);

			for (String dstNamespace : dstNamespaces) {
				writeSpace();
				write(dstNamespace);
			}

			writeLn();
		}
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		// TODO: Support the static method marker once https://github.com/FabricMC/mapping-io/pull/41 is merged
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		this.srcName = srcName;

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		this.srcName = srcName;
		this.srcDesc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		this.srcName = srcName;
		this.srcDesc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		if (tsrg2) {
			this.srcName = srcName;
			this.lvIndex = lvIndex;
			return true;
		}

		return false;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		return false; // not supported, skip
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		if (!tsrg2 && namespace != 0) return;

		dstNames[namespace] = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		switch (targetKind) {
		case CLASS:
			break;
		case FIELD:
		case METHOD:
			writeTab();
			break;
		case METHOD_ARG:
			assert tsrg2;
			writeTab();
			writeTab();
			write(Integer.toString(lvIndex));
			writeSpace();
		case METHOD_VAR:
			assert tsrg2;
			break;
		}

		write(srcName);

		if (targetKind == MappedElementKind.METHOD
				|| (targetKind == MappedElementKind.FIELD && tsrg2)) {
			writeSpace();
			write(srcDesc);
		}

		int dstNsCount = tsrg2 ? dstNames.length : 1;

		for (int i = 0; i < dstNsCount; i++) {
			String dstName = dstNames[i];
			writeSpace();
			write(dstName != null ? dstName : srcName);
		}

		writeLn();

		srcName = srcDesc = null;
		Arrays.fill(dstNames, null);
		lvIndex = -1;

		return targetKind == MappedElementKind.CLASS
				|| (tsrg2 && targetKind == MappedElementKind.METHOD);
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		// not supported, skip
	}

	private void write(String str) throws IOException {
		writer.write(str);
	}

	private void writeTab() throws IOException {
		writer.write('\t');
	}

	private void writeSpace() throws IOException {
		writer.write(' ');
	}

	private void writeLn() throws IOException {
		writer.write('\n');
	}

	private static final Set<MappingFlag> tsrgFlags = EnumSet.of(MappingFlag.NEEDS_ELEMENT_UNIQUENESS, MappingFlag.NEEDS_SRC_METHOD_DESC);
	private static final Set<MappingFlag> tsrg2Flags;

	static {
		tsrg2Flags = EnumSet.copyOf(tsrgFlags);
		tsrg2Flags.add(MappingFlag.NEEDS_SRC_FIELD_DESC);
	}

	private final Writer writer;
	private final boolean tsrg2;
	private String srcName;
	private String srcDesc;
	private String[] dstNames;
	private int lvIndex = -1;
}

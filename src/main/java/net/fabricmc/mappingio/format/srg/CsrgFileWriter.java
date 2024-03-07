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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;

/**
 * {@linkplain MappingFormat#CSRG_FILE CSRG file} writer.
 */
public final class CsrgFileWriter implements MappingWriter {
	public CsrgFileWriter(Writer writer) {
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
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		// not supported, skip
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		classSrcName = srcName;

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		memberSrcName = srcName;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		memberSrcName = srcName;
		methodSrcDesc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		return false; // not supported, skip
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		return false; // not supported, skip
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		if (namespace != 0) return;

		dstName = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (dstName == null) return false;

		write(classSrcName);

		if (targetKind != MappedElementKind.CLASS) {
			writeSpace();
			write(memberSrcName);

			if (targetKind == MappedElementKind.METHOD) {
				writeSpace();
				write(methodSrcDesc);
			}

			memberSrcName = methodSrcDesc = null;
		}

		writeSpace();
		write(dstName);
		writeLn();

		dstName = null;

		return targetKind == MappedElementKind.CLASS; // only members are supported, skip anything but class contents
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		// not supported, skip
	}

	private void write(String str) throws IOException {
		writer.write(str);
	}

	private void writeSpace() throws IOException {
		writer.write(' ');
	}

	private void writeLn() throws IOException {
		writer.write('\n');
	}

	private static final Set<MappingFlag> flags = EnumSet.of(MappingFlag.NEEDS_SRC_METHOD_DESC);

	private final Writer writer;
	private String classSrcName;
	private String memberSrcName;
	private String methodSrcDesc;
	private String dstName;
}

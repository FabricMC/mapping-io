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

package net.fabricmc.mappingio.format.simple;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;

/**
 * {@linkplain MappingFormat#RECAF_SIMPLE Recaf Simple file} writer.
 */
public final class RecafSimpleFileWriter implements MappingWriter {
	public RecafSimpleFileWriter(Writer writer) {
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
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		classSrcName = srcName;

		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		memberSrcName = srcName;
		memberSrcDesc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		memberSrcName = srcName;
		memberSrcDesc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException {
		return false; // not supported, skip
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
		return false; // not supported, skip
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		if (namespace != 0) return;
		dstName = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (dstName == null) return true;
		write(classSrcName);

		if (targetKind != MappedElementKind.CLASS) {
			if (memberSrcName == null) throw new IllegalArgumentException("member source name cannot be null!");
			writer.write('.');
			write(memberSrcName);

			if (memberSrcDesc != null) {
				if (targetKind == MappedElementKind.FIELD) writeSpace();
				write(memberSrcDesc);
			}
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

	private void writeLn() throws IOException {
		writer.write('\n');
	}

	private void writeSpace() throws IOException {
		writer.write(' ');
	}

	private static final Set<MappingFlag> flags = EnumSet.of(MappingFlag.NEEDS_SRC_FIELD_DESC, MappingFlag.NEEDS_SRC_METHOD_DESC);

	private final Writer writer;
	private String classSrcName;
	private String memberSrcName;
	private String memberSrcDesc;
	private String dstName;
}

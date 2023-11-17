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

public final class SrgFileWriter implements MappingWriter {
	public SrgFileWriter(Writer writer, boolean xsrg) {
		this.writer = writer;
		this.xsrg = xsrg;
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return xsrg ? xsrgFlags : srgFlags;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		classSrcName = srcName;
		classDstName = null;

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		memberSrcName = srcName;
		memberSrcDesc = srcDesc;
		memberDstName = null;
		memberDstDesc = null;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		memberSrcName = srcName;
		memberSrcDesc = srcDesc;
		memberDstName = null;
		memberDstDesc = null;

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

		switch (targetKind) {
		case CLASS:
			classDstName = name;
			break;
		case FIELD:
		case METHOD:
			memberDstName = name;
			break;
		default:
			break;
		}
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		if (namespace != 0) return;

		memberDstDesc = desc;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		switch (targetKind) {
		case CLASS:
			if (classDstName == null) return true;
			write("CL: ");
			break;
		case FIELD:
			if (memberDstName == null) return false;
			write("FD: ");
			break;
		case METHOD:
			if (memberDstName == null || memberDstDesc == null) return false;
			write("MD: ");
			break;
		default:
			throw new IllegalStateException("unexpected invocation for "+targetKind);
		}

		write(classSrcName);

		if (targetKind != MappedElementKind.CLASS) {
			write("/");
			write(memberSrcName);

			if (targetKind == MappedElementKind.METHOD || xsrg) {
				write(" ");
				write(memberSrcDesc);
			}
		}

		write(" ");
		if (classDstName == null) classDstName = classSrcName;
		write(classDstName);

		if (targetKind != MappedElementKind.CLASS) {
			write("/");
			write(memberDstName);

			if (targetKind == MappedElementKind.METHOD || xsrg) {
				write(" ");
				write(memberDstDesc);
			}
		}

		writeLn();

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

	private static final Set<MappingFlag> srgFlags = EnumSet.of(MappingFlag.NEEDS_SRC_METHOD_DESC, MappingFlag.NEEDS_DST_METHOD_DESC);
	private static final Set<MappingFlag> xsrgFlags;

	static {
		xsrgFlags = EnumSet.copyOf(srgFlags);
		xsrgFlags.add(MappingFlag.NEEDS_SRC_FIELD_DESC);
		xsrgFlags.add(MappingFlag.NEEDS_DST_FIELD_DESC);
	}

	private final Writer writer;
	private final boolean xsrg;
	private String classSrcName;
	private String memberSrcName;
	private String memberSrcDesc;
	private String classDstName;
	private String memberDstName;
	private String memberDstDesc;
}

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

package net.fabricmc.mappingio.format.jobf;

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
 * {@linkplain MappingFormat#JOBF_FILE JOBF file} writer.
 */
public final class JobfFileWriter implements MappingWriter {
	public JobfFileWriter(Writer writer) {
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
	public boolean visitPackage(String srcName) throws IOException {
		packageSrcName = srcName;
		dstName = null;

		return true;
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		classSrcName = srcName;
		dstName = null;

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		if (srcDesc == null) {
			return false;
		}

		memberSrcName = srcName;
		memberSrcDesc = srcDesc;
		dstName = null;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		if (srcDesc == null) {
			return false;
		}

		memberSrcName = srcName;
		memberSrcDesc = srcDesc;
		dstName = null;

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
		boolean isPackage = targetKind == MappedElementKind.PACKAGE;
		boolean isClass = targetKind == MappedElementKind.CLASS;
		boolean isField = false;

		if (dstName == null) {
			return isPackage || isClass;
		}

		if (isPackage) {
			if (countOccurrences(packageSrcName, '/') != countOccurrences(dstName, '/')) {
				return false; // JOBF doesn't allow changes to the package structure
			}

			packageSrcName = packageSrcName.replace('/', '.');
			dstName = dstName.replace('/', '.');
			write("p ");
		} else if (isClass) {
			int srcNameLastSep = classSrcName.lastIndexOf('/');
			int dstNameLastSep = dstName.lastIndexOf('/');
			String srcPkg = classSrcName.substring(0, srcNameLastSep + 1);
			String dstPkg = dstName.substring(0, dstNameLastSep + 1);

			classSrcName = classSrcName.replace('/', '.');
			if (!srcPkg.equals(dstPkg)) return true; // JOBF doesn't support renaming into a different package

			dstName = dstName.substring(dstNameLastSep + 1);
			write("c ");
		} else if ((isField = targetKind == MappedElementKind.FIELD)
				|| targetKind == MappedElementKind.METHOD) {
			write(isField ? "f " : "m ");
		} else {
			throw new IllegalStateException("unexpected invocation for "+targetKind);
		}

		write(isPackage ? packageSrcName : classSrcName);

		if (!isPackage && !isClass) {
			write(".");
			write(memberSrcName);

			if (isField) write(":");
			write(memberSrcDesc);
		}

		write(" = ");
		write(dstName);

		writeLn();

		return isClass; // only members are supported, skip anything but class contents
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

	private int countOccurrences(String str, char c) {
		int count = 0;

		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) count++;
		}

		return count;
	}

	private static final Set<MappingFlag> flags = EnumSet.of(MappingFlag.NEEDS_SRC_FIELD_DESC, MappingFlag.NEEDS_SRC_METHOD_DESC);

	private final Writer writer;
	private String packageSrcName;
	private String classSrcName;
	private String memberSrcName;
	private String memberSrcDesc;
	private String dstName;
}

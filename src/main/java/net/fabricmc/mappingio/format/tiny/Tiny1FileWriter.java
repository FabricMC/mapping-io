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

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.ProgressListener;
import net.fabricmc.mappingio.format.AbstractMappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;

public final class Tiny1FileWriter extends AbstractMappingWriter {
	public Tiny1FileWriter(Writer writer, ProgressListener progressListener) {
		super(MappingFormat.TINY_FILE, writer, progressListener, "Writing Tiny v1 file");
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return flags;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		super.visitNamespaces(srcNamespace, dstNamespaces);
		dstNames = new String[dstNamespaces.size()];

		write("v1\t");
		write(srcNamespace);

		for (String dstNamespace : dstNamespaces) {
			writeTab();
			write(dstNamespace);
		}

		writeLn();
	}

	@Override
	public void visitMetadata(String key, String value) throws IOException {
		super.visitMetadata(key, value);

		switch (key) {
		case Tiny1FileReader.nextIntermediaryClassProperty:
		case Tiny1FileReader.nextIntermediaryFieldProperty:
		case Tiny1FileReader.nextIntermediaryMethodProperty:
			write("# INTERMEDIARY-COUNTER ");

			switch (key) {
			case Tiny1FileReader.nextIntermediaryClassProperty:
				write("class");
				break;
			case Tiny1FileReader.nextIntermediaryFieldProperty:
				write("field");
				break;
			case Tiny1FileReader.nextIntermediaryMethodProperty:
				write("method");
				break;
			default:
				throw new IllegalStateException();
			}

			write(" ");
			write(value);
			writeLn();
		}
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		super.visitClass(srcName);
		classSrcName = srcName;
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		super.visitField(srcName, srcDesc);

		memberSrcName = srcName;
		memberSrcDesc = srcDesc;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		super.visitMethod(srcName, srcDesc);

		memberSrcName = srcName;
		memberSrcDesc = srcDesc;

		return true;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		dstNames[namespace] = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		// determine if there is any useful data to emit
		boolean found = false;

		for (String dstName : dstNames) {
			if (dstName != null) {
				found = true;
				break;
			}
		}

		if (!found) return true;

		switch (targetKind) {
		case CLASS:
			write("CLASS");
			break;
		case FIELD:
			write("FIELD");
			break;
		case METHOD:
			write("METHOD");
			break;
		default:
			throw new IllegalStateException("unexpected invocation for "+targetKind);
		}

		writeTab();
		write(classSrcName);

		if (targetKind != MappedElementKind.CLASS) {
			writeTab();
			write(memberSrcDesc);
			writeTab();
			write(memberSrcName);
		}

		for (String dstName : dstNames) {
			writeTab();
			if (dstName != null) write(dstName);
		}

		writeLn();

		Arrays.fill(dstNames, null);

		return targetKind == MappedElementKind.CLASS; // only members are supported, skip anything but class contents
	}

	private void write(String str) throws IOException {
		writer.write(str);
	}

	private void writeLn() throws IOException {
		writer.write('\n');
	}

	private void writeTab() throws IOException {
		writer.write('\t');
	}

	private static final Set<MappingFlag> flags = EnumSet.of(MappingFlag.NEEDS_SRC_FIELD_DESC, MappingFlag.NEEDS_SRC_METHOD_DESC);

	private String classSrcName;
	private String memberSrcName;
	private String memberSrcDesc;
	private String[] dstNames;
}

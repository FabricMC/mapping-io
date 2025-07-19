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
package net.fabricmc.mappingio.format.pdme;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * {@linkplain MappingFormat#PDME_FILE Paragraph Delimited Mappings Extended file} writer.
 */
public final class PDMEFileWriter implements MappingWriter {

	private static final char DELIM = '\u00B6';

	private final Writer out;

	private String currentClassSlash;
	private String currentMethodName;
	private String currentMethodDesc;

	private String dstClassFullSlash; // mapped class (slash) if provided
	private String dstMemberName; // mapped field or method simple name

	private String fieldSrcName;
	private String fieldSrcDesc;

	private StringBuilder stagedRow;
	private MappedElementKind stagedKind;

	public PDMEFileWriter(Writer out) {
		this.out = out;
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return EnumSet.of(MappingFlag.NEEDS_HEADER_METADATA, MappingFlag.NEEDS_SRC_FIELD_DESC,
				MappingFlag.NEEDS_SRC_METHOD_DESC);
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		out.write("tipo" + DELIM + "original" + DELIM + "nuevo" + DELIM + "def" + DELIM + "pos" + DELIM + "desc\n");
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		flushStaged();
		currentClassSlash = srcName;
		currentMethodName = null;
		currentMethodDesc = null;
		dstClassFullSlash = null;
		dstMemberName = null;
		fieldSrcName = null;
		fieldSrcDesc = null;
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		flushStaged();
		fieldSrcName = srcName;
		fieldSrcDesc = srcDesc;
		dstMemberName = null;
		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		flushStaged();
		if (srcDesc == null) {
			return false; // skip methods without descriptor
		}
		currentMethodName = srcName;
		currentMethodDesc = srcDesc;
		dstMemberName = null;
		return true;
	}

	@Override
	public void visitDstName(MappedElementKind kind, int namespace, String name) {
		if (namespace != 0 || name == null)
			return;
		switch (kind) {
		case CLASS:
			dstClassFullSlash = name;
			break;
		case FIELD:
		case METHOD:
			dstMemberName = name;
			break;
		default:
			break; // Param kinds ignored (not supported here)
		}
	}

	@Override
	public boolean visitElementContent(MappedElementKind kind) throws IOException {
		switch (kind) {
		case CLASS:
			stageClass();
			return true;
		case FIELD:
			stageField();
			return true;
		case METHOD:
			stageMethod();
			return true;
		default:
			return false; // ignore args/locals/others
		}
	}

	private void stageClass() {
		String origFull = currentClassSlash.replace('/', '.');
		String mappedFull = (dstClassFullSlash != null ? dstClassFullSlash : currentClassSlash).replace('/', '.');

		stagedRow = begin("Class").append(origFull).append(DELIM).append(mappedFull).append(DELIM).append("nil")
				.append(DELIM).append("nil").append(DELIM);
		stagedKind = MappedElementKind.CLASS;
		dstClassFullSlash = null;
	}

	private void stageField() {
		String cls = currentClassSlash.replace('/', '.');
		String mapped = (dstMemberName != null ? dstMemberName : fieldSrcName);

		stagedRow = begin("Var").append(cls).append('.').append(fieldSrcName).append(':').append(fieldSrcDesc)
				.append(DELIM).append(mapped).append(DELIM).append("nil").append(DELIM).append("nil").append(DELIM);
		stagedKind = MappedElementKind.FIELD;
		dstMemberName = null;
	}

	private void stageMethod() {
		String cls = currentClassSlash.replace('/', '.');
		String mapped = (dstMemberName != null ? dstMemberName : currentMethodName);

		stagedRow = begin("Def").append(cls).append('.').append(currentMethodName).append(currentMethodDesc)
				.append(DELIM).append(mapped).append(DELIM).append("nil").append(DELIM).append("nil").append(DELIM);
		stagedKind = MappedElementKind.METHOD;
		dstMemberName = null;
	}

	private StringBuilder begin(String tipo) {
		return new StringBuilder().append(tipo).append(DELIM);
	}

	@Override
	public void visitComment(MappedElementKind kind, String comment) throws IOException {
	    if (stagedRow != null && stagedKind == kind) {
	        if (comment != null && !comment.isEmpty()) {
	            String esc = comment
	                .replaceAll("\\.+$", "")
	                .replace("\r\n", "\\n")
	                .replace("\n", "\\n")
	                .replace("\r", "\\n");
	            
	            stagedRow.append(esc);
	        }
	        out.write(stagedRow.toString());
	        out.write('\n');
	        stagedRow = null;
	        stagedKind = null;
	    }
	}

	private void flushStaged() throws IOException {
		if (stagedRow != null) {
			out.write(stagedRow.toString());
			out.write('\n');
			stagedRow = null;
			stagedKind = null;
		}
	}

	@Override
	public void close() throws IOException {
		flushStaged();
		out.close();
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		return false;// I could not figure out how to get this to work with the Unit Tests. Should be
						// Param¶nil¶name¶featurecreep.example.ExampleClass$SubClass.TEST_METH(ILjava/lang/String;Ljava/lang/String;)V¶1¶JavaDocsOrComment
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName)
			throws IOException {
		return false;// I could not figure out how to get these to work with the Unit Tests. Should
						// be
						// Param¶nil¶name¶featurecreep.example.ExampleClass$SubClass.TEST_METH(ILjava/lang/String;Ljava/lang/String;)V¶1¶JavaDocsOrComment
	}
}

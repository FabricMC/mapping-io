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

package net.fabricmc.mappingio.format.pdme;

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
 * {@linkplain MappingFormat#PDME_FILE Paragraph Delimited Mappings Extended
 * file} writer.
 */
public final class PDMEFileWriter implements MappingWriter {
	private static final char DELIM = '\u00B6';

	private final Writer out;

	private String currentClassSlash;
	private String currentMethodName;
	private String currentMethodDesc;

	private int currentMethodParamCount;
	private int nextLocalPos1;

	private String dstClassFullSlash;
	private String dstMemberName;
	private String dstParamName;

	private String fieldSrcName;
	private String fieldSrcDesc;

	private String paramSrcName;
	private int paramOrLocalPos1;
	private boolean stagingParam;
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
		currentMethodParamCount = 0;
		nextLocalPos1 = 0;
		dstClassFullSlash = null;
		dstMemberName = null;
		dstParamName = null;
		fieldSrcName = null;
		fieldSrcDesc = null;
		paramSrcName = null;
		stagingParam = false;
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		flushStaged();

		if (srcDesc == null) {
			return false;
		}

		fieldSrcName = srcName;
		fieldSrcDesc = srcDesc;
		dstMemberName = null;
		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		flushStaged();

		if (srcDesc == null) {
			return false;
		}

		currentMethodName = srcName;
		currentMethodDesc = srcDesc;
		currentMethodParamCount = countParams(srcDesc);
		nextLocalPos1 = currentMethodParamCount + 1;
		dstMemberName = null;
		dstParamName = null;
		paramSrcName = null;
		stagingParam = false;
		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		flushStaged();
		paramSrcName = srcName;
		dstParamName = null;
		paramOrLocalPos1 = argPosition + 1;
		stagingParam = true;
		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName)
			throws IOException {
		flushStaged();
		paramSrcName = srcName;
		dstParamName = null;

		if (nextLocalPos1 == 0) {
			nextLocalPos1 = currentMethodParamCount + 1;
		}

		paramOrLocalPos1 = nextLocalPos1++;
		stagingParam = true;
		return true;
	}

	@Override
	public void visitDstName(MappedElementKind kind, int namespace, String name) {
		if (namespace != 0 || name == null) {
			return;
		}

		switch (kind) {
		case CLASS:
			dstClassFullSlash = name;
			break;
		case FIELD:
		case METHOD:
			dstMemberName = name;
			break;
		case METHOD_ARG:
		case METHOD_VAR:
			dstParamName = name;
			break;
		default:
			break;
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
		case METHOD_ARG:
		case METHOD_VAR:
			return stageParam();
		default:
			return false;
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

	private boolean stageParam() {
		if (!stagingParam) {
			return false;
		}

		boolean hasSrc = paramSrcName != null && paramSrcName.length() > 0;
		boolean hasDst = dstParamName != null && dstParamName.length() > 0;

		if (!hasSrc && !hasDst) {
			dstParamName = null;
			paramSrcName = null;
			return false;
		}

		String originalCol = hasSrc ? paramSrcName : "nil";
		String chosen = hasDst ? dstParamName : paramSrcName;

		String clsDotted = currentClassSlash.replace('/', '.');
		String def = clsDotted + '.' + currentMethodName + currentMethodDesc;

		stagedRow = begin("Param").append(originalCol).append(DELIM).append(chosen).append(DELIM).append(def)
				.append(DELIM).append(paramOrLocalPos1).append(DELIM);

		stagedKind = MappedElementKind.METHOD_ARG;
		dstParamName = null;
		paramSrcName = null;
		stagingParam = false;
		return true;
	}

	private StringBuilder begin(String tipo) {
		return new StringBuilder().append(tipo).append(DELIM);
	}

	@Override
	public void visitComment(MappedElementKind kind, String comment) throws IOException {
		if (stagedRow == null) {
			return;
		}

		if (stagedKind == kind || (stagedKind == MappedElementKind.METHOD_ARG
				&& (kind == MappedElementKind.METHOD_ARG || kind == MappedElementKind.METHOD_VAR))) {
			if (comment != null && !comment.isEmpty()) {
				String esc = comment.replaceAll("\\.+$", "").replace("\r\n", "\\n").replace("\n", "\\n").replace("\r",
						"\\n");
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

		stagingParam = false;
	}

	@Override
	public void close() throws IOException {
		flushStaged();
		out.close();
	}

	private static int countParams(String desc) {
		if (desc == null || desc.length() == 0) {
			return 0;
		}

		int count = 0;
		int i = 1;

		while (i < desc.length()) {
			char c = desc.charAt(i);

			if (c == ')') {
				break;
			}

			if (c == 'L') {
				int semi = desc.indexOf(';', i);

				if (semi < 0) {
					break;
				}

				i = semi + 1;
				count++;
			} else if (c == '[') {
				i++;

				while (i < desc.length() && desc.charAt(i) == '[') {
					i++;
				}

				if (i < desc.length() && desc.charAt(i) == 'L') {
					int semi = desc.indexOf(';', i);

					if (semi < 0) {
						break;
					}

					i = semi + 1;
				} else {
					i++;
				}

				count++;
			} else {
				i++;
				count++;
			}
		}

		return count;
	}
}

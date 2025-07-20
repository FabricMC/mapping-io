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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/**
 * {@linkplain MappingFormat#PDME_FILE Paragraph Delimited Mappings Extended
 * file} reader.
 *
 * <p>
 * Crashes if a second visit pass is requested without
 * {@link MappingFlag#NEEDS_MULTIPLE_PASSES} having been passed beforehand.
 */
public final class PDMEFileReader {
	private static final char DELIM = '\u00B6';
	private static final Pattern LOCAL_META_PATTERN = Pattern.compile("^-?\\d+:-?\\d+:-?\\d+:-?\\d+$");

	private PDMEFileReader() {
	}

	public static void read(Reader in, MappingVisitor visitor) throws IOException {
		Set<MappingFlag> flags = visitor.getFlags();

		MappingVisitor downstream = visitor;
		MemoryMappingTree buffering = null;

		if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS)) {
			buffering = new MemoryMappingTree();
			visitor = buffering;
		}

		BufferedReader br = new BufferedReader(in);
		String header = br.readLine();

		if (header == null) {
			return;
		}

		List<String> lines = new ArrayList<String>();

		for (String l; (l = br.readLine()) != null;) {
			lines.add(l);
		}

		boolean multi = flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES);

		while (true) {
			boolean h = visitor.visitHeader();

			if (h) {
				visitor.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK,
						Collections.singletonList(MappingUtil.NS_TARGET_FALLBACK));
			}

			if (visitor.visitContent()) {
				parseContent(lines, visitor);
			}

			if (visitor.visitEnd()) {
				break;
			}

			if (!multi) {
				throw new IllegalStateException("Repeated visitation requested without NEEDS_MULTIPLE_PASSES");
			}
		}

		if (buffering != null) {
			buffering.accept(downstream);
		}
	}

	private abstract static class Row {
		abstract void emit(MappingVisitor v) throws IOException;
	}

	private static final class ClassRow extends Row {
		final String original;
		final String mappedCol;
		final String comment;
		String finalMapped;
		boolean resolved;

		ClassRow(String o, String m, String c) {
			this.original = o;
			this.mappedCol = m;
			this.comment = c;
		}

		void resolve(Map<String, ClassRow> byOrig) {
			if (resolved) {
				return;
			}

			resolved = true;

			if (original == null) {
				return;
			}

			int lastDollar = original.lastIndexOf('$');

			if (lastDollar < 0) {
				finalMapped = (mappedCol != null ? mappedCol : original);
				return;
			}

			String outerOrig = original.substring(0, lastDollar);
			String tailOrig = original.substring(lastDollar + 1);

			ClassRow outer = byOrig.get(outerOrig);

			if (outer != null) {
				outer.resolve(byOrig);
			}

			String outerFinal = (outer != null && outer.finalMapped != null) ? outer.finalMapped : outerOrig;

			if (mappedCol == null) {
				finalMapped = outerFinal + '$' + tailOrig;
			} else {
				if (isSimpleTail(mappedCol)) {
					finalMapped = outerFinal + '$' + mappedCol;
				} else {
					finalMapped = mappedCol;
				}
			}
		}

		private static boolean isSimpleTail(String s) {
			return s.indexOf('$') < 0 && s.indexOf('.') < 0;
		}

		@Override
		void emit(MappingVisitor v) throws IOException {
			if (original == null) {
				return;
			}

			if (finalMapped == null) {
				finalMapped = original;
			}

			String slashOrig = original.replace('.', '/');
			String slashMapped = original.equals(finalMapped) ? null : finalMapped.replace('.', '/');

			if (v.visitClass(slashOrig)) {
				if (slashMapped != null) {
					v.visitDstName(MappedElementKind.CLASS, 0, slashMapped);
				}

				if (v.visitElementContent(MappedElementKind.CLASS) && comment != null) {
					v.visitComment(MappedElementKind.CLASS, comment);
				}
			}
		}
	}

	private static final class FieldRow extends Row {
		final String original;
		final String mapped;
		final String comment;

		FieldRow(String o, String m, String c) {
			this.original = o;
			this.mapped = m;
			this.comment = c;
		}

		@Override
		void emit(MappingVisitor v) throws IOException {
			if (original == null) {
				return;
			}

			int colon = original.lastIndexOf(':');

			if (colon < 0) {
				return;
			}

			String desc = original.substring(colon + 1);
			String ownerAndName = original.substring(0, colon);
			int lastDot = ownerAndName.lastIndexOf('.');

			if (lastDot < 0) {
				return;
			}

			String clsDotted = ownerAndName.substring(0, lastDot);
			String fieldName = ownerAndName.substring(lastDot + 1);
			String slashClass = clsDotted.replace('.', '/');

			if (v.visitClass(slashClass)) {
				v.visitElementContent(MappedElementKind.CLASS);
			}

			if (v.visitField(fieldName, desc)) {
				if (mapped != null) {
					v.visitDstName(MappedElementKind.FIELD, 0, mapped);
				}

				if (v.visitElementContent(MappedElementKind.FIELD) && comment != null) {
					v.visitComment(MappedElementKind.FIELD, comment);
				}
			}
		}
	}

	private static final class MethodRow extends Row {
		final String original;
		final String mapped;
		final String comment;

		MethodRow(String o, String m, String c) {
			this.original = o;
			this.mapped = m;
			this.comment = c;
		}

		@Override
		void emit(MappingVisitor v) throws IOException {
			if (original == null) {
				return;
			}

			int paren = original.indexOf('(');

			if (paren < 0) {
				return;
			}

			int lastDot = original.lastIndexOf('.', paren);

			if (lastDot < 0) {
				return;
			}

			String clsDotted = original.substring(0, lastDot);
			String methodName = original.substring(lastDot + 1, paren);
			String methodDesc = original.substring(paren);
			String slashClass = clsDotted.replace('.', '/');

			if (v.visitClass(slashClass)) {
				v.visitElementContent(MappedElementKind.CLASS);
			}

			if (v.visitMethod(methodName, methodDesc)) {
				if (mapped != null) {
					v.visitDstName(MappedElementKind.METHOD, 0, mapped);
				}

				if (v.visitElementContent(MappedElementKind.METHOD) && comment != null) {
					v.visitComment(MappedElementKind.METHOD, comment);
				}
			}
		}
	}

	private static final class ParamRow extends Row {
		final String def;
		final String posStr;
		final String mappedName;
		final String comment;
		final String sourceName;
		final String localMeta;

		ParamRow(String d, String p, String mapped, String c, String sourceName, String localMeta) {
			this.def = d;
			this.posStr = p;
			this.mappedName = mapped;
			this.comment = c;
			this.sourceName = sourceName;
			this.localMeta = localMeta;
		}

		@Override
		void emit(MappingVisitor v) throws IOException {
			if (def == null || posStr == null || mappedName == null) {
				return;
			}

			int paren = def.indexOf('(');

			if (paren < 0) {
				return;
			}

			int lastDot = def.lastIndexOf('.', paren);

			if (lastDot < 0) {
				return;
			}

			String clsDotted = def.substring(0, lastDot);
			String methodName = def.substring(lastDot + 1, paren);
			String methodDesc = def.substring(paren);

			int pos;

			try {
				pos = Integer.parseInt(posStr);
			} catch (NumberFormatException e) {
				return;
			}

			if (pos <= 0) {
				return;
			}

			String slashClass = clsDotted.replace('.', '/');

			if (v.visitClass(slashClass)) {
				v.visitElementContent(MappedElementKind.CLASS);
			}

			if (v.visitMethod(methodName, methodDesc)) {
				v.visitElementContent(MappedElementKind.METHOD);
			}

			int paramCount = countParams(methodDesc);

			if (pos <= paramCount) {
				int zeroIdx = pos - 1;
				String src = (sourceName != null ? sourceName : mappedName);

				if (v.visitMethodArg(zeroIdx, -1, src)) {
					boolean needsDst = (mappedName != null) && (sourceName == null || !mappedName.equals(sourceName));

					if (needsDst) {
						v.visitDstName(MappedElementKind.METHOD_ARG, 0, mappedName);
					}

					if (v.visitElementContent(MappedElementKind.METHOD_ARG) && comment != null) {
						v.visitComment(MappedElementKind.METHOD_ARG, comment);
					}
				}
			} else {
				int lvtRowIndex = 0;
				int lvIndex = 0;
				int start = 0;
				int end = -1;

				if (localMeta != null) {
					String[] parts = localMeta.split(":");

					if (parts.length == 4) {
						try {
							lvtRowIndex = Integer.parseInt(parts[0]);
							lvIndex = Integer.parseInt(parts[1]);
							start = Integer.parseInt(parts[2]);
							end = Integer.parseInt(parts[3]);
						} catch (NumberFormatException e) {
							return;
						}
					} else {
						return;
					}
				} else {
					return;
				}

				String src = (sourceName != null ? sourceName : mappedName);

				if (v.visitMethodVar(lvtRowIndex, lvIndex, start, end, src)) {
					boolean needsDst = (mappedName != null) && (sourceName == null || !mappedName.equals(sourceName));

					if (needsDst) {
						v.visitDstName(MappedElementKind.METHOD_VAR, 0, mappedName);
					}

					if (v.visitElementContent(MappedElementKind.METHOD_VAR) && comment != null) {
						v.visitComment(MappedElementKind.METHOD_VAR, comment);
					}
				}
			}
		}
	}

	private static void parseContent(List<String> lines, MappingVisitor v) throws IOException {
		Map<String, ClassRow> classRowsByOrig = new LinkedHashMap<String, ClassRow>();
		List<Row> otherRows = new ArrayList<Row>();

		for (int i = 0; i < lines.size(); i++) {
			String raw = lines.get(i);

			if (raw == null) {
				continue;
			}

			String trimmed = raw.trim();

			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}

			String[] cols = splitToSix(trimmed);
			String tipo = cols[0];
			String originalCol = normaliseEmpty(cols[1]);
			String nuevo = normaliseEmpty(cols[2]);
			String def = normaliseEmpty(cols[3]);
			String pos = normaliseEmpty(cols[4]);
			String desc = normaliseEmpty(cols[5]);

			if ("Class".equals(tipo)) {
				classRowsByOrig.put(originalCol, new ClassRow(originalCol, nuevo, desc));
			} else if ("Var".equals(tipo)) {
				otherRows.add(new FieldRow(originalCol, nuevo, desc));
			} else if ("Def".equals(tipo)) {
				otherRows.add(new MethodRow(originalCol, nuevo, desc));
			} else if ("Param".equals(tipo)) {
				String sourceName = null;

				String localMeta = null;

				if (originalCol != null) {
					if (LOCAL_META_PATTERN.matcher(originalCol).matches()) {
						localMeta = originalCol;
					} else {
						sourceName = originalCol;
					}
				}

				otherRows.add(new ParamRow(def, pos, nuevo, desc, sourceName, localMeta));
			}
		}

		for (ClassRow cr : classRowsByOrig.values()) {
			cr.resolve(classRowsByOrig);
		}

		for (ClassRow cr : classRowsByOrig.values()) {
			cr.emit(v);
		}

		for (Row r : otherRows) {
			r.emit(v);
		}
	}

	private static String[] splitToSix(String line) {
		String[] arr = line.split(Pattern.quote(String.valueOf(DELIM)), -1);

		if (arr.length == 6) {
			return arr;
		}

		if (arr.length < 6) {
			String[] six = new String[6];

			System.arraycopy(arr, 0, six, 0, arr.length);

			for (int i = arr.length; i < 6; i++) {
				six[i] = "";
			}

			return six;
		}

		StringBuilder desc = new StringBuilder(arr[5]);

		for (int i = 6; i < arr.length; i++) {
			if (desc.length() > 0) {
				desc.append(DELIM);
			}

			desc.append(arr[i]);
		}

		String[] six = new String[6];
		System.arraycopy(arr, 0, six, 0, 5);
		six[5] = desc.toString();
		return six;
	}

	private static String normaliseEmpty(String s) {
		return (s == null || s.isEmpty() || "nil".equals(s)) ? null : s;
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

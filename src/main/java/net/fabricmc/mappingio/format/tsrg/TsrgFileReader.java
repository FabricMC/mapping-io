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

package net.fabricmc.mappingio.format.tsrg;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.ProgressListener;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.format.MappingReaderProgressListenerHelper;

public final class TsrgFileReader {
	public static List<String> getNamespaces(Reader reader) throws IOException {
		return getNamespaces(new ColumnFileReader(reader, ' '));
	}

	private static List<String> getNamespaces(ColumnFileReader reader) throws IOException {
		if (reader.nextCol("tsrg2")) { // tsrg2 magic
			List<String> ret = new ArrayList<>();
			String ns;

			while ((ns = reader.nextCol()) != null) {
				ret.add(ns);
			}

			return ret;
		} else { // assume tsrg1
			return Arrays.asList(MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK);
		}
	}

	public static void read(Reader reader, MappingVisitor visitor, ProgressListener progressListener) throws IOException {
		read(reader, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor, progressListener);
	}

	public static void read(Reader reader, String sourceNs, String targetNs, MappingVisitor visitor, ProgressListener progressListener) throws IOException {
		read(new ColumnFileReader(reader, ' '), sourceNs, targetNs, visitor, new MappingReaderProgressListenerHelper(progressListener));
	}

	private static void read(ColumnFileReader reader, String sourceNs, String targetNs,
			MappingVisitor visitor, MappingReaderProgressListenerHelper progressHelper) throws IOException {
		boolean isTsrg2 = reader.nextCol("tsrg2");
		String srcNamespace;
		List<String> dstNamespaces;

		if (isTsrg2) {
			progressHelper.init(-1, "Reading TSRG2 file");
			srcNamespace = reader.nextCol();
			dstNamespaces = new ArrayList<>();
			String dstNamespace;

			while ((dstNamespace = reader.nextCol()) != null) {
				dstNamespaces.add(dstNamespace);
			}

			reader.nextLine(0);
		} else {
			progressHelper.init(-1, "Reading TSRG file");
			srcNamespace = sourceNs;
			dstNamespaces = Collections.singletonList(targetNs);
		}

		int dstNsCount = dstNamespaces.size();

		if (visitor.getFlags().contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			reader.mark();
		}

		List<String> nameTmp = dstNamespaces.size() > 1 ? new ArrayList<>(dstNamespaces.size() - 1) : null;

		for (;;) {
			boolean visitHeader = visitor.visitHeader();

			if (visitHeader) {
				visitor.visitNamespaces(srcNamespace, dstNamespaces);
			}

			if (visitor.visitContent(-1, -1, -1, -1, -1, -1, -1)) {
				do {
					if (reader.hasExtraIndents()) continue;

					String srcName = reader.nextCol();
					if (srcName == null || srcName.endsWith("/")) continue;
					if (srcName.isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());
					progressHelper.readClass(srcName);

					if (visitor.visitClass(srcName)) {
						readClass(reader, isTsrg2, dstNsCount, nameTmp, visitor, progressHelper);
					}
				} while (reader.nextLine(0));
			}

			if (visitor.visitEnd()) break;

			reader.reset();
		}

		progressHelper.finish();
	}

	private static void readClass(ColumnFileReader reader, boolean isTsrg2, int dstNsCount, List<String> nameTmp,
			MappingVisitor visitor, MappingReaderProgressListenerHelper progressHelper) throws IOException {
		readDstNames(reader, MappedElementKind.CLASS, 0, dstNsCount, visitor);
		if (!visitor.visitElementContent(MappedElementKind.CLASS)) return;

		while (reader.nextLine(1)) {
			if (reader.hasExtraIndents()) continue;

			String srcName = reader.nextCol();
			if (srcName == null || srcName.isEmpty()) throw new IOException("missing name-a in line "+reader.getLineNumber());

			String arg = reader.nextCol();
			if (arg == null) throw new IOException("missing desc/name-b in line "+reader.getLineNumber());

			if (arg.startsWith("(")) { // method: <nameA> <descA> <names>...
				progressHelper.readMethod(srcName);

				if (visitor.visitMethod(srcName, arg)) {
					readMethod(reader, dstNsCount, visitor, progressHelper);
				}
			} else if (!isTsrg2) { // tsrg1 field, never has a desc: <nameA> <names>...
				progressHelper.readField(srcName);

				if (visitor.visitField(srcName, null)) {
					if (!arg.isEmpty()) visitor.visitDstName(MappedElementKind.FIELD, 0, arg);
					readElement(reader, MappedElementKind.FIELD, 1, dstNsCount, visitor);
				}
			} else { // tsrg2 field, may have desc
				for (int i = 0; i < dstNsCount - 1; i++) {
					String name = reader.nextCol();
					if (name == null) throw new IOException("missing name columns in line "+reader.getLineNumber());
					nameTmp.add(name);
				}

				String lastName = reader.nextCol();
				int offset;
				String desc;

				if (lastName == null) { // no desc, arg is first dst name, nameTmp starts with 2nd dst name: <nameA> <names>...
					offset = 1;
					desc = null;
				} else { // arg is desc, nameTmp starts with 1st dst name: <nameA> <descA> <names>...
					offset = 0;
					desc = arg;
					if (desc.isEmpty()) throw new IOException("empty field desc in line "+reader.getLineNumber());
				}

				if (visitor.visitField(srcName, desc)) {
					// first name without desc
					if (lastName == null && !arg.isEmpty()) visitor.visitDstName(MappedElementKind.FIELD, 0, arg);

					// middle names
					for (int i = 0; i < dstNsCount - 1; i++) {
						String name = nameTmp.get(i);
						if (!name.isEmpty()) visitor.visitDstName(MappedElementKind.FIELD, i + offset, name);
					}

					// last name with desc
					if (lastName != null && !lastName.isEmpty()) visitor.visitDstName(MappedElementKind.FIELD, dstNsCount - 1, lastName);

					visitor.visitElementContent(MappedElementKind.FIELD);
				}

				if (nameTmp != null) nameTmp.clear();
			}
		}
	}

	private static void readMethod(ColumnFileReader reader, int dstNsCount, MappingVisitor visitor, MappingReaderProgressListenerHelper progressHelper) throws IOException {
		readDstNames(reader, MappedElementKind.METHOD, 0, dstNsCount, visitor);
		if (!visitor.visitElementContent(MappedElementKind.METHOD)) return;

		while (reader.nextLine(2)) {
			if (reader.hasExtraIndents()) continue;

			if (reader.nextCol("static")) {
				// method is static
			} else {
				int lvIndex = reader.nextIntCol();
				if (lvIndex < 0) throw new IOException("missing/invalid parameter lv-index in line "+reader.getLineNumber());

				String srcName = reader.nextCol();
				if (srcName == null) throw new IOException("missing var-name-a column in line "+reader.getLineNumber());
				if (srcName.isEmpty()) srcName = null;

				if (visitor.visitMethodArg(-1, lvIndex, srcName)) {
					readElement(reader, MappedElementKind.METHOD_ARG, 0, dstNsCount, visitor);
				}
			}
		}
	}

	private static void readElement(ColumnFileReader reader, MappedElementKind kind, int dstNsOffset, int dstNsCount, MappingVisitor visitor) throws IOException {
		readDstNames(reader, kind, dstNsOffset, dstNsCount, visitor);
		visitor.visitElementContent(kind);
	}

	private static void readDstNames(ColumnFileReader reader, MappedElementKind subjectKind, int dstNsOffset, int dstNsCount, MappingVisitor visitor) throws IOException {
		for (int dstNs = dstNsOffset; dstNs < dstNsCount; dstNs++) {
			String name = reader.nextCol();
			if (name == null) throw new IOException("missing name columns in line "+reader.getLineNumber());

			if (!name.isEmpty()) visitor.visitDstName(subjectKind, dstNs, name);
		}
	}
}

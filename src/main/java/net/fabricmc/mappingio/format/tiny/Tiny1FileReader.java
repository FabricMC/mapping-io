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
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.ColumnFileReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.StandardProperties;
import net.fabricmc.mappingio.format.StandardProperty;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

/**
 * {@linkplain MappingFormat#TINY_1 Tiny v1 file} reader.
 *
 * <p>Crashes if a second visit pass is requested without
 * {@link MappingFlag#NEEDS_MULTIPLE_PASSES} having been passed beforehand.
 */
public final class Tiny1FileReader {
	private Tiny1FileReader() {
	}

	public static List<String> getNamespaces(Reader reader) throws IOException {
		return getNamespaces(new ColumnFileReader(reader, '\t'));
	}

	private static List<String> getNamespaces(ColumnFileReader reader) throws IOException {
		if (!reader.nextCol("v1")) { // magic/version
			throw new IOException("invalid/unsupported tiny file: no tiny 1 header");
		}

		List<String> ret = new ArrayList<>();
		String ns;

		while ((ns = reader.nextCol()) != null) {
			ret.add(ns);
		}

		return ret;
	}

	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(new ColumnFileReader(reader, '\t'), visitor);
	}

	private static void read(ColumnFileReader reader, MappingVisitor visitor) throws IOException {
		if (!reader.nextCol("v1")) { // magic/version
			throw new IOException("invalid/unsupported tiny file: no tiny 1 header");
		}

		String srcNamespace = reader.nextCol();
		List<String> dstNamespaces = new ArrayList<>();
		String dstNamespace;

		while ((dstNamespace = reader.nextCol()) != null) {
			dstNamespaces.add(dstNamespace);
		}

		int dstNsCount = dstNamespaces.size();
		Set<MappingFlag> flags = visitor.getFlags();
		MappingVisitor parentVisitor = null;

		if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS) || flags.contains(MappingFlag.NEEDS_HEADER_METADATA)) {
			parentVisitor = visitor;
			visitor = new MemoryMappingTree();
		} else if (flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
			reader.mark();
		}

		for (;;) {
			boolean visitHeader = visitor.visitHeader();

			if (visitHeader) {
				visitor.visitNamespaces(srcNamespace, dstNamespaces);
			}

			if (visitor.visitContent()) {
				String lastClass = null;
				boolean lastClassDstNamed = false;;
				boolean visitLastClass = false;
				Map<String, String> potentialFooterMetadata = new LinkedHashMap<>();

				while (reader.nextLine(0)) {
					boolean isClass = false;
					boolean isMethod = false;
					boolean isField = false;

					if ((isClass = reader.nextCol("CLASS"))
							|| (isMethod = reader.nextCol("METHOD"))
							|| (isField = reader.nextCol("FIELD"))) {
						// Footer metadata can't be followed by a new class/method/field, so any stored data was comments
						potentialFooterMetadata.clear();
					}

					if (isClass) { // class: CLASS <names>...
						String srcName = reader.nextCol();
						if (srcName == null || srcName.isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());

						if (!lastClassDstNamed || !srcName.equals(lastClass)) {
							lastClass = srcName;
							lastClassDstNamed = true;
							visitLastClass = visitor.visitClass(srcName);

							if (visitLastClass) {
								readDstNames(reader, MappedElementKind.CLASS, dstNsCount, visitor);
								visitLastClass = visitor.visitElementContent(MappedElementKind.CLASS);
							}
						}
					} else if (isMethod || isField) { // method: METHOD cls-a desc-a <names>... or field: FIELD cls-a desc-a <names>...
						String srcOwner = reader.nextCol();
						if (srcOwner == null || srcOwner.isEmpty()) throw new IOException("missing class-name-a in line "+reader.getLineNumber());

						if (!srcOwner.equals(lastClass)) {
							lastClass = srcOwner;
							lastClassDstNamed = false;
							visitLastClass = visitor.visitClass(srcOwner) && visitor.visitElementContent(MappedElementKind.CLASS);
						}

						if (visitLastClass) {
							String srcDesc = reader.nextCol();
							if (srcDesc == null || srcDesc.isEmpty()) throw new IOException("missing desc-a in line "+reader.getLineNumber());
							String srcName = reader.nextCol();
							if (srcName == null || srcName.isEmpty()) throw new IOException("missing name-a in line "+reader.getLineNumber());

							if (isMethod && visitor.visitMethod(srcName, srcDesc)
									|| !isMethod && visitor.visitField(srcName, srcDesc)) {
								MappedElementKind kind = isMethod ? MappedElementKind.METHOD : MappedElementKind.FIELD;
								readDstNames(reader, kind, dstNsCount, visitor);
								visitor.visitElementContent(kind);
							}
						}
					} else {
						String line = reader.nextCol();

						if (line.startsWith("# ") && line.length() >= 3 && line.charAt(3) != ' ') { // Potentially metadata
							line = line.substring(2);
							String[] parts = line.split(" ");
							String value = parts[parts.length - 1];
							String key = line.substring(0, line.lastIndexOf(value) - 1);

							if (key.isEmpty()) {
								String oldValue = value;
								value = key;
								key = oldValue;
							}

							StandardProperty property = StandardProperties.getByName(key);

							if (property != null) {
								if (!property.isApplicableTo(format)) continue; // How did it get there?
								key = property.getId();
							}

							if (lastClass == null) { // Header metadata
								visitor.visitMetadata(key, value);
							} else {
								potentialFooterMetadata.put(key, value);
							}
						}
					}
				}

				for (Map.Entry<String, String> entry : potentialFooterMetadata.entrySet()) {
					visitor.visitMetadata(entry.getKey(), entry.getValue());
				}
			}

			if (visitor.visitEnd()) break;

			reader.reset();
		}

		if (parentVisitor != null) {
			((MappingTree) visitor).accept(parentVisitor);
		}
	}

	private static void readDstNames(ColumnFileReader reader, MappedElementKind subjectKind, int dstNsCount, MappingVisitor visitor) throws IOException {
		for (int dstNs = 0; dstNs < dstNsCount; dstNs++) {
			String name = reader.nextCol();
			if (name == null) throw new IOException("missing name columns in line "+reader.getLineNumber());

			if (!name.isEmpty()) visitor.visitDstName(subjectKind, dstNs, name);
		}
	}

	private static final MappingFormat format = MappingFormat.TINY_FILE;
}

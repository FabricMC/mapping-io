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

package net.fabricmc.mappingio.format.intellij;

import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;

/**
 * {@linkplain MappingFormat#INTELLIJ_MIGRATION_MAP_FILE IntelliJ migration map} writer.
 */
public final class MigrationMapFileWriter implements MappingWriter {
	public MigrationMapFileWriter(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void close() throws IOException {
		try {
			if (xmlWriter != null) {
				xmlWriter.writeEndDocument();
				xmlWriter.close();
			}
		} catch (XMLStreamException e) {
			throw new IOException(e);
		} finally {
			writer.close();
		}
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return flags;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		// TODO: Support once https://github.com/FabricMC/mapping-io/pull/29 is merged
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		this.srcName = srcName;
		this.dstName = null;

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		return false;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		return false;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		return false;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		return false;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		if (namespace != 0) return;

		dstName = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		if (dstName == null) return false;

		try {
			if (xmlWriter == null) {
				xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);

				xmlWriter.writeStartDocument("UTF-8", "1.0");
				xmlWriter.writeStartElement("migrationMap");
			}

			xmlWriter.writeStartElement("entry");
			xmlWriter.writeAttribute("oldName", srcName);
			xmlWriter.writeAttribute("newName", dstName);
			xmlWriter.writeAttribute("type", "class");
			xmlWriter.writeEndElement();

			return false;
		} catch (XMLStreamException | FactoryConfigurationError e) {
			throw new IOException(e);
		}
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		// not supported, skip
	}

	private static final Set<MappingFlag> flags = EnumSet.of(MappingFlag.NEEDS_ELEMENT_UNIQUENESS);

	private final Writer writer;
	private XMLStreamWriter xmlWriter;
	private String srcName;
	private String dstName;
}

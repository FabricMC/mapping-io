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

package net.fabricmc.mappingio;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.format.MappingFormat;

@ApiStatus.NonExtendable
public interface MappingReader {
	static MappingFormat detectFormat(Path file) throws IOException {
		if (Files.isDirectory(file)) {
			return MappingFormat.ENIGMA_DIR;
		} else {
			try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
				return detectFormat(reader);
			}
		}
	}

	static MappingFormat detectFormat(Reader reader) throws IOException {
		int DETECT_HEADER_LEN = 4096;
		char[] buffer = new char[DETECT_HEADER_LEN];
		int pos = 0;
		int len;

		while (pos < buffer.length
				&& (len = reader.read(buffer, pos, buffer.length - pos)) >= 0) {
			pos += len;
		}

		if (pos < 3) return null;

		switch (String.valueOf(buffer, 0, 3)) {
		case "v1\t":
			return MappingFormat.TINY_FILE;
		case "tin":
			return MappingFormat.TINY_2_FILE;
		case "tsr": // tsrg2 <nsA> <nsB> ..<nsN>
			return MappingFormat.TSRG_2_FILE;
		case "CLA":
			return MappingFormat.ENIGMA_FILE;
		case "PK:":
		case "CL:":
		case "MD:":
		case "FD:":
			return MappingFormat.SRG_FILE;
		}

		String headerStr = String.valueOf(buffer, 0, pos);

		if (headerStr.contains(" -> ")) {
			return MappingFormat.PROGUARD_FILE;
		} else if (headerStr.contains("\n\t")) {
			return MappingFormat.TSRG_FILE;
		}

		return null; // unknown format or corrupted
	}

	/**
	 * Read mappings from a file or directory.
	 * @param path the file or directory to read from
	 * @param visitor the visitor receiving the mappings
	 */
	default void read(Path path, MappingVisitor visitor) throws IOException {
		if (!path.toFile().isDirectory()) {
			try (Reader reader = Files.newBufferedReader(path)) {
				read(reader, path, visitor);
				return;
			}
		}

		throw new UnsupportedOperationException("format doesn't support directories");
	}

	/**
	 * Read mappings from the reader if the format supports it, otherwise from the path.
	 * @param reader the reader to read from, given the format supports it
	 * @param path the file or directory to read from alternatively
	 * @param visitor the visitor receiving the mappings
	 * @throws IOException if an I/O error occurs or none of the given sources could be used
	 */
	void read(Reader reader, @Nullable Path path, MappingVisitor visitor) throws IOException;

	default List<String> getNamespaces(Reader reader) throws IOException {
		return Arrays.asList(MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK);
	}
}

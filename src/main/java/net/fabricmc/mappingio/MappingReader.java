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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.mappingio.format.EnigmaReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.ProGuardReader;
import net.fabricmc.mappingio.format.SimpleReader;
import net.fabricmc.mappingio.format.SrgReader;
import net.fabricmc.mappingio.format.Tiny1Reader;
import net.fabricmc.mappingio.format.Tiny2Reader;
import net.fabricmc.mappingio.format.TsrgReader;

public final class MappingReader {
	public static MappingFormat detectFormat(Path file) throws IOException {
		if (Files.isDirectory(file)) {
			return MappingFormat.ENIGMA;
		} else {
			try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
				return detectFormat(reader);
			}
		}
	}

	public static MappingFormat detectFormat(Reader reader) throws IOException {
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
			return MappingFormat.TINY;
		case "tin":
			return MappingFormat.TINY_2;
		case "tsr": // tsrg2 <nsA> <nsB> ..<nsN>
			return MappingFormat.TSRG2;
		case "PK:":
		case "CL:":
		case "MD:":
		case "FD:":
			return MappingFormat.SRG;
		}

		String headerStr = String.valueOf(buffer, 0, pos);

		if (headerStr.contains(" -> ")) {
			return MappingFormat.PROGUARD;
		} else if (headerStr.contains("\n\t")) {
			return MappingFormat.TSRG;
		}

		return null; // unknown format or corrupted
	}

	public static List<String> getNamespaces(Path file) throws IOException {
		return getNamespaces(file, null);
	}

	public static List<String> getNamespaces(Path file, MappingFormat format) throws IOException {
		if (format == null) {
			format = detectFormat(file);
			if (format == null) throw new IOException("invalid/unsupported mapping format");
		}

		if (format.hasNamespaces) {
			try (Reader reader = Files.newBufferedReader(file)) {
				return getNamespaces(reader, format);
			}
		} else {
			return Arrays.asList(MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK);
		}
	}

	public static List<String> getNamespaces(Reader reader) throws IOException {
		return getNamespaces(reader, null);
	}

	public static List<String> getNamespaces(Reader reader, MappingFormat format) throws IOException {
		if (format == null) {
			if (!reader.markSupported()) reader = new BufferedReader(reader);
			reader.mark(DETECT_HEADER_LEN);
			format = detectFormat(reader);
			reader.reset();
			if (format == null) throw new IOException("invalid/unsupported mapping format");
		}

		if (format.hasNamespaces) {
			switch (format) {
			case TINY:
				return Tiny1Reader.getNamespaces(reader);
			case TINY_2:
				return Tiny2Reader.getNamespaces(reader);
			case TSRG2:
				return TsrgReader.getNamespaces(reader);
			default:
				throw new IllegalStateException();
			}
		} else {
			return Arrays.asList(MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK);
		}
	}

	public static void read(Path file, MappingVisitor visitor) throws IOException {
		read(file, null, visitor);
	}

	public static void read(Path file, MappingFormat format, MappingVisitor visitor) throws IOException {
		if (format == null) {
			format = detectFormat(file);
			if (format == null) throw new IOException("invalid/unsupported mapping format");
		}

		if (format.hasSingleFile()) {
			try (Reader reader = Files.newBufferedReader(file)) {
				read(reader, format, visitor);
			}
		} else {
			switch (format) {
			case ENIGMA:
				EnigmaReader.read(file, visitor);
				break;
			case MCP:
				throw new UnsupportedOperationException(); // TODO: implement
			default:
				throw new IllegalStateException();
			}
		}
	}

	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(reader, null, visitor);
	}

	public static void read(Reader reader, MappingFormat format, MappingVisitor visitor) throws IOException {
		if (format == null) {
			if (!reader.markSupported()) reader = new BufferedReader(reader);
			reader.mark(DETECT_HEADER_LEN);
			format = detectFormat(reader);
			reader.reset();
			if (format == null) throw new IOException("invalid/unsupported mapping format");
		}

		switch (format) {
		case TINY:
			Tiny1Reader.read(reader, visitor);
			break;
		case TINY_2:
			Tiny2Reader.read(reader, visitor);
			break;
		case SRG:
			SrgReader.read(reader, visitor);
			break;
		case TSRG:
		case TSRG2:
			TsrgReader.read(reader, visitor);
			break;
		case PROGUARD:
			ProGuardReader.read(reader, visitor);
			break;
		case SIMPLE:
			SimpleReader.read(reader, visitor);
			break;
		default:
			throw new IllegalStateException();
		}
	}

	private static final int DETECT_HEADER_LEN = 4096;
}

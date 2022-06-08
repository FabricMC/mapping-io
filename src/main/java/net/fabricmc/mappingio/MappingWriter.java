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

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.mappingio.format.EnigmaDirWriter;
import net.fabricmc.mappingio.format.EnigmaWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.Tiny1Writer;
import net.fabricmc.mappingio.format.Tiny2Writer;

public interface MappingWriter extends Closeable, MappingVisitor {
	static MappingWriter create(Path file, MappingFormat format) throws IOException {
		if (format.hasSingleFile()) {
			return create(Files.newBufferedWriter(file), format);
		} else {
			switch (format) {
			case ENIGMA_DIR: return new EnigmaDirWriter(file, true);
			default: throw new UnsupportedOperationException("format "+format+" is not implemented");
			}
		}
	}

	static MappingWriter create(Writer writer, MappingFormat format) throws IOException {
		if (!format.hasSingleFile()) throw new IllegalArgumentException("format "+format+" is not applicable to a single writer");

		switch (format) {
		case TINY: return new Tiny1Writer(writer);
		case TINY_2: return new Tiny2Writer(writer, false);
		case ENIGMA: return new EnigmaWriter(writer);
		default: throw new UnsupportedOperationException("format "+format+" is not implemented");
		}
	}
}

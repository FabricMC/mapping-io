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

package net.fabricmc.mappingio.format;

import java.io.IOException;
import java.io.Reader;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;

@ApiStatus.Internal
public interface MappingFileReader extends MappingReader {
	/**
	 * Read mappings from the passed reader.
	 * @param reader the reader to read from
	 * @param visitor the visitor receiving the mappings
	 * @throws IOException if an I/O error occurs
	 */
	default void read(Reader reader, MappingVisitor visitor) throws IOException {
		read(reader, null, visitor);
	}
}

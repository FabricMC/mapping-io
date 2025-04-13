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

package net.fabricmc.mappingio.test.tests.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.NopMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.test.TestMappings.MappingDir;

public class DetectionTest {
	@Test
	public void run() throws Exception {
		for (MappingDir dir : TestMappings.values()) {
			for (MappingFormat format : MappingFormat.values()) {
				if (format == MappingFormat.RECAF_SIMPLE_FILE) {
					assertThrows(AssertionFailedError.class, () -> check(dir, format));
				} else {
					check(dir, format);
				}
			}
		}

		assertNull(MappingReader.detectFormat(TestMappings.DETECTION.path().resolve("non-mapping-dir")));
	}

	private void check(MappingDir dir, MappingFormat format) throws Exception {
		Path path = dir.pathFor(format);

		if (!Files.exists(path)) {
			return;
		}

		assertEquals(format, MappingReader.detectFormat(path));

		if (!format.hasSingleFile()) return;
		if (format == MappingFormat.CSRG_FILE) return;

		try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			assertEquals(format, MappingReader.detectFormat(reader));
		}

		// Make sure that the passed reader still works after implicit format detection (see https://github.com/FabricMC/mapping-io/pull/71).
		try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			MappingReader.read(reader, new NopMappingVisitor(true));
		}
	}
}

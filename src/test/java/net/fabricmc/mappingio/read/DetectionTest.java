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

package net.fabricmc.mappingio.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.format.MappingFormat;

public class DetectionTest {
	private static Path dir;

	@BeforeAll
	public static void setup() throws Exception {
		dir = TestHelper.getResource("/detection/");
	}

	@Test
	public void enigmaFile() throws Exception {
		check("enigma.mappings", MappingFormat.ENIGMA_FILE);
	}

	@Test
	public void enigmaDirectory() throws Exception {
		check("enigma-dir", MappingFormat.ENIGMA_DIR);
	}

	@Test
	public void tinyFile() throws Exception {
		check("tiny.tiny", MappingFormat.TINY_FILE);
	}

	@Test
	public void tinyV2File() throws Exception {
		check("tinyV2.tiny", MappingFormat.TINY_2_FILE);
	}

	@Test
	public void srgFile() throws Exception {
		check("srg.srg", MappingFormat.SRG_FILE);
	}

	@Test
	public void xrgFile() throws Exception {
		check("xsrg.xsrg", MappingFormat.XSRG_FILE);
	}

	@Test
	public void csrgFile() throws Exception {
		assertThrows(AssertionFailedError.class, () -> check("csrg.csrg", MappingFormat.CSRG_FILE));
	}

	@Test
	public void tsrgFile() throws Exception {
		check("tsrg.tsrg", MappingFormat.TSRG_FILE);
	}

	@Test
	public void tsrg2File() throws Exception {
		check("tsrg2.tsrg", MappingFormat.TSRG_2_FILE);
	}

	private void check(String file, MappingFormat format) throws Exception {
		Path path = dir.resolve(file);
		assertEquals(format, MappingReader.detectFormat(path));

		if (!format.hasSingleFile()) return;

		try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			assertEquals(format, MappingReader.detectFormat(reader));
		}
	}
}

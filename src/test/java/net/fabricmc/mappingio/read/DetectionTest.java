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

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.NopMappingVisitor;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.format.MappingFormat;

public class DetectionTest {
	private static final Path dir = TestHelper.MappingDirs.DETECTION;

	@Test
	public void enigmaFile() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;
		check(format);
	}

	@Test
	public void enigmaDirectory() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_DIR;
		check(format);
	}

	@Test
	public void tinyFile() throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;
		check(format);
	}

	@Test
	public void tinyV2File() throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;
		check(format);
	}

	@Test
	public void srgFile() throws Exception {
		MappingFormat format = MappingFormat.SRG_FILE;
		check(format);
	}

	@Test
	public void xrgFile() throws Exception {
		MappingFormat format = MappingFormat.XSRG_FILE;
		check(format);
	}

	@Test
	public void jamFile() throws Exception {
		MappingFormat format = MappingFormat.JAM_FILE;
		check(format);
	}

	@Test
	public void csrgFile() throws Exception {
		MappingFormat format = MappingFormat.CSRG_FILE;
		assertThrows(AssertionFailedError.class, () -> check(format));
	}

	@Test
	public void tsrgFile() throws Exception {
		MappingFormat format = MappingFormat.TSRG_FILE;
		check(format);
	}

	@Test
	public void tsrg2File() throws Exception {
		MappingFormat format = MappingFormat.TSRG_2_FILE;
		check(format);
	}

	@Test
	public void proguardFile() throws Exception {
		MappingFormat format = MappingFormat.PROGUARD_FILE;
		check(format);
	}

	@Test
	public void recafSimpleFile() throws Exception {
		MappingFormat format = MappingFormat.RECAF_SIMPLE_FILE;
		assertThrows(AssertionFailedError.class, () -> check(format));
	}

	@Test
	public void jobfFile() throws Exception {
		MappingFormat format = MappingFormat.JOBF_FILE;
		check(format);
	}

	private void check(MappingFormat format) throws Exception {
		Path path = dir.resolve(TestHelper.getFileName(format));
		assertEquals(format, MappingReader.detectFormat(path));

		if (!format.hasSingleFile()) return;

		try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			assertEquals(format, MappingReader.detectFormat(reader));
		}

		// Make sure that the passed reader still works after implicit format detection (see https://github.com/FabricMC/mapping-io/pull/71).
		try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
			MappingReader.read(reader, new NopMappingVisitor(true));
		}
	}
}

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

package net.fabricmc.mappingio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.format.MappingFormat;

public class DetectionTest {
	private static Path dir;

	@BeforeAll
	public static void setup() throws Exception {
		dir = TestHelper.getResource("/detection/");
	}

	@Test
	public void enigma() throws Exception {
		assertEquals(MappingFormat.ENIGMA_FILE, MappingReader.detectFormat(dir.resolve("enigma.mappings")));
	}

	@Test
	public void enigmaDirectory() throws Exception {
		assertEquals(MappingFormat.ENIGMA_DIR, MappingReader.detectFormat(dir.resolve("enigma-dir")));
	}

	@Test
	public void tiny() throws Exception {
		assertEquals(MappingFormat.TINY_FILE, MappingReader.detectFormat(dir.resolve("tiny.tiny")));
	}

	@Test
	public void tinyV2() throws Exception {
		assertEquals(MappingFormat.TINY_2_FILE, MappingReader.detectFormat(dir.resolve("tinyV2.tiny")));
	}
}

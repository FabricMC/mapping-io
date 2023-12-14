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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.format.ErrorCollector;
import net.fabricmc.mappingio.format.ErrorCollector.Severity;
import net.fabricmc.mappingio.format.ErrorCollector.ThrowingErrorCollector;
import net.fabricmc.mappingio.format.enigma.EnigmaFileReader;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.srg.SrgFileReader;
import net.fabricmc.mappingio.format.srg.TsrgFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class EmptyContentReadTest {
	private static final VisitableMappingTree tree = new MemoryMappingTree();
	private static final ErrorCollector errorCollector = new ThrowingErrorCollector(Severity.INFO);

	@Test
	public void emptyEnigmaFile() throws Exception {
		EnigmaFileReader.read(new StringReader(""), tree, errorCollector);
	}

	@Test
	public void emptyTinyFile() throws Exception {
		assertThrows(IOException.class, () -> Tiny1FileReader.read(new StringReader(""), tree, errorCollector));
	}

	@Test
	public void emptyTinyV2File() throws Exception {
		assertThrows(IOException.class, () -> Tiny2FileReader.read(new StringReader(""), tree, errorCollector));
	}

	@Test
	public void emptyProguardFile() throws Exception {
		ProGuardFileReader.read(new StringReader(""), tree, errorCollector);
	}

	@Test
	public void emptySrgFile() throws Exception {
		SrgFileReader.read(new StringReader(""), tree, errorCollector);
	}

	@Test
	public void emptyTsrgFile() throws Exception {
		TsrgFileReader.read(new StringReader(""), tree, errorCollector);
	}
}

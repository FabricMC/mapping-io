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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.VisitOrderVerifyingVisitor;
import net.fabricmc.mappingio.format.enigma.EnigmaFileReader;
import net.fabricmc.mappingio.format.intellij.MigrationMapFileReader;
import net.fabricmc.mappingio.format.jobf.JobfFileReader;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.simple.RecafSimpleFileReader;
import net.fabricmc.mappingio.format.srg.JamFileReader;
import net.fabricmc.mappingio.format.srg.SrgFileReader;
import net.fabricmc.mappingio.format.srg.TsrgFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny1FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class EmptyContentReadTest {
	private MappingVisitor target;

	@BeforeEach
	public void instantiateTree() {
		target = new VisitOrderVerifyingVisitor(new MemoryMappingTree());
	}

	@Test
	public void emptyEnigmaFile() throws Exception {
		EnigmaFileReader.read(new StringReader(""), target);
	}

	@Test
	public void emptyTinyFile() throws Exception {
		String header0 = "";
		String header1 = "v1";
		String header2 = header1 + "\t";
		String header3 = header2 + "srcNs";
		String header4 = header3 + "\t";
		String header5 = header4 + "dstNs";

		assertThrows(IOException.class, () -> Tiny1FileReader.read(new StringReader(header0), target));
		assertThrows(IOException.class, () -> Tiny1FileReader.read(new StringReader(header1), target));
		assertThrows(IOException.class, () -> Tiny1FileReader.read(new StringReader(header2), target));
		assertThrows(IOException.class, () -> Tiny1FileReader.read(new StringReader(header3), target));
		assertThrows(IOException.class, () -> Tiny1FileReader.read(new StringReader(header4), target));
		Tiny1FileReader.read(new StringReader(header5), target);
	}

	@Test
	public void emptyTinyV2File() throws Exception {
		String header0 = "";
		String header1 = "tiny\t2\t0";
		String header2 = header1 + "\t";
		String header3 = header2 + "srcNs";
		String header4 = header3 + "\t";
		String header5 = header4 + "dstNs";

		assertThrows(IOException.class, () -> Tiny2FileReader.read(new StringReader(header0), target));
		assertThrows(IOException.class, () -> Tiny2FileReader.read(new StringReader(header1), target));
		assertThrows(IOException.class, () -> Tiny2FileReader.read(new StringReader(header2), target));
		assertThrows(IOException.class, () -> Tiny2FileReader.read(new StringReader(header3), target));
		assertThrows(IOException.class, () -> Tiny2FileReader.read(new StringReader(header4), target));
		Tiny2FileReader.read(new StringReader(header5), target);
	}

	@Test
	public void emptyProguardFile() throws Exception {
		ProGuardFileReader.read(new StringReader(""), target);
	}

	@Test
	public void emptySrgFile() throws Exception {
		SrgFileReader.read(new StringReader(""), target);
	}

	@Test
	public void emptyJamFile() throws Exception {
		JamFileReader.read(new StringReader(""), target);
	}

	@Test
	public void emptyTsrgFile() throws Exception {
		String header0 = "";
		String header1 = "tsrg2";
		String header2 = header1 + " ";
		String header3 = header2 + "srcNs";
		String header4 = header3 + " ";
		String header5 = header4 + "dstNs";

		TsrgFileReader.read(new StringReader(header0), target); // interpreted as TSRG v1
		assertThrows(IOException.class, () -> TsrgFileReader.read(new StringReader(header1), target));
		assertThrows(IOException.class, () -> TsrgFileReader.read(new StringReader(header2), target));
		assertThrows(IOException.class, () -> TsrgFileReader.read(new StringReader(header3), target));
		assertThrows(IOException.class, () -> TsrgFileReader.read(new StringReader(header4), target));
		instantiateTree();
		TsrgFileReader.read(new StringReader(header5), target);
	}

	@Test
	public void emptyMigrationMapFile() throws Exception {
		assertThrows(IOException.class, () -> MigrationMapFileReader.read(new StringReader(""), target));

		instantiateTree();
		assertThrows(IOException.class, () -> MigrationMapFileReader.read(new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), target));

		instantiateTree();
		MigrationMapFileReader.read(
				new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
						+ "<migrationMap>\n"
						+ "</migrationMap>"),
				target);
	}

	@Test
	public void emptyRecafSimpleFile() throws Exception {
		RecafSimpleFileReader.read(new StringReader(""), target);
	}

	@Test
	public void emptyJobfFile() throws Exception {
		JobfFileReader.read(new StringReader(""), target);
	}
}

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

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.NopMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;

public class InvalidContentReadTest {
	String tinyHeader = "v1	source	target\n";
	String tiny2Header = "tiny	2	0	source	target\n";

	@Test
	public void enigmaFile() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;

		checkThrows(" ", format);
		checkThrows("	", format);
		checkThrows("	CLASS", format);

		checkEnigmaLine(MappedElementKind.CLASS);
		checkEnigmaLine(MappedElementKind.FIELD);
		checkEnigmaLine(MappedElementKind.METHOD);
		// TODO: args
	}

	private void checkEnigmaLine(MappedElementKind kind) throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;
		String prefix = (kind == MappedElementKind.CLASS ? "" : "	") + kind.name();

		// Tabs for separation
		checkThrows(prefix + "	", format);
		checkThrows(prefix + "	src", format);

		// Spaces for separation
		prefix += " src";
		String suffix = "";

		if (kind != MappedElementKind.CLASS) {
			prefix = "CLASS src\n" + prefix;
			suffix += kind == MappedElementKind.FIELD ? " I" : " ()V";
		}

		check(prefix, format, kind != MappedElementKind.CLASS);
		checkWorks(prefix + suffix, format);

		checkThrows(prefix + " ", format);
		checkThrows(prefix + " " + suffix, format);

		check(prefix + " dst", format, kind == MappedElementKind.METHOD); // field normally too, but doesn't have descriptor validation yet
		checkWorks(prefix + " dst" + suffix, format);

		checkThrows(prefix + " dst ", format);
		checkThrows(prefix + " dst " + suffix, format);

		check(prefix + " dst dst2", format, kind != MappedElementKind.FIELD);
		checkThrows(prefix + " dst dst2" + suffix, format);
	}

	@Test
	public void tinyFile() throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;

		checkThrows(" ", format);
		checkWorks(tinyHeader, format);
		checkThrows(tinyHeader + " ", format);
		checkThrows(tinyHeader + "	", format);

		checkTinyLine(MappedElementKind.CLASS);
		checkTinyLine(MappedElementKind.FIELD);
		checkTinyLine(MappedElementKind.METHOD);
		// TODO: args, vars
	}

	private void checkTinyLine(MappedElementKind kind) throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;
		String prefix = tinyHeader + kind.name();

		// No source/target
		checkThrows(prefix, format);

		// Spaces for separation
		checkThrows(prefix + " ", format);
		checkThrows(prefix + " src", format);

		// Tabs for separation
		prefix += "	src";

		if (kind != MappedElementKind.CLASS) {
			checkThrows(prefix, format);

			prefix += kind == MappedElementKind.FIELD ? "	I" : "	()V";
			checkThrows(prefix, format);

			prefix += "	src";
		}

		checkThrows(prefix, format);
		checkWorks(prefix + "	", format);
		checkWorks(prefix + "	dst", format);
		checkThrows(prefix + "	dst	", format);
		checkThrows(prefix + "	dst	dst2", format);
	}

	@Test
	public void tinyV2File() throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;

		checkThrows(" ", format);
		checkWorks(tiny2Header, format);
		checkThrows(tiny2Header + " ", format);
		checkThrows(tiny2Header + "	", format);

		checkTiny2Line(MappedElementKind.CLASS);
		checkTiny2Line(MappedElementKind.FIELD);
		checkTiny2Line(MappedElementKind.METHOD);
		// TODO: args, vars
	}

	private void checkTiny2Line(MappedElementKind kind) throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;
		String prefix = tiny2Header;

		if (kind == MappedElementKind.CLASS) {
			prefix += "c";
		} else {
			prefix += "c	src	\n	" + (kind == MappedElementKind.FIELD ? "f" : "m");
		}

		// No source/target
		checkThrows(prefix, format);

		// Spaces for separation
		checkThrows(prefix + " ", format);
		checkThrows(prefix + " src", format);

		// Tabs for separation
		if (kind != MappedElementKind.CLASS) {
			checkThrows(prefix, format);

			prefix += kind == MappedElementKind.FIELD ? "	I" : "	()V";
			checkThrows(prefix, format);
		}

		prefix += "	src";

		checkThrows(prefix, format);
		checkWorks(prefix + "	", format);
		checkWorks(prefix + "	dst", format);
		checkThrows(prefix + "	dst	", format);
		checkThrows(prefix + "	dst	dst2", format);
	}

	private void check(String fileContent, MappingFormat format, boolean shouldThrow) throws Exception {
		if (shouldThrow) {
			checkThrows(fileContent, format);
		} else {
			checkWorks(fileContent, format);
		}
	}

	private void checkWorks(String fileContent, MappingFormat format) throws Exception {
		MappingReader.read(new StringReader(fileContent), format, new NopMappingVisitor(true));
	}

	private void checkThrows(String fileContent, MappingFormat format) {
		assertThrows(Exception.class, () -> checkWorks(fileContent, format));
	}
}

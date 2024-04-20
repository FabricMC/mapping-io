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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.NopMappingVisitor;
import net.fabricmc.mappingio.format.ErrorCollector;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.ParsingError;
import net.fabricmc.mappingio.format.ParsingError.Severity;

public class InvalidContentReadTest {
	private static final String tinyHeader = "v1	source	target\n";
	private static final String tiny2Header = "tiny	2	0	source	target\n";
	private static final String tsrg2Header = "tsrg2 source target\n";

	@Test
	public void tinyFile() throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;

		checkThrows(" ", format);
		checkWorks(tinyHeader, format);

		checkTinyLine(MappedElementKind.CLASS);
		checkTinyLine(MappedElementKind.FIELD);
		checkTinyLine(MappedElementKind.METHOD);
	}

	private void checkTinyLine(MappedElementKind kind) throws Exception {
		MappingFormat format = MappingFormat.TINY_FILE;
		String prefix = tinyHeader + kind.name();

		// No source/target
		checkError(prefix, format);

		// Tabs for separation
		prefix += "\t";
		checkError(prefix, format);
		prefix += "src";
		checkError(prefix, format);

		if (kind == MappedElementKind.FIELD || kind == MappedElementKind.METHOD) {
			prefix += "\t";
			checkError(prefix, format);

			prefix += kind == MappedElementKind.FIELD ? "I" : "()V";
			checkError(prefix, format);

			prefix += "\tsrc";
			checkError(prefix, format);
		}

		checkWorks(prefix += "\t", format);
		checkWorks(prefix += "dst", format);
	}

	@Test
	public void tinyV2File() throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;

		checkThrows(" ", format);
		checkWorks(tiny2Header, format);
		checkError(tiny2Header + "\t", format); // missing property key

		checkTinyV2Line(MappedElementKind.CLASS);
		checkTinyV2Line(MappedElementKind.FIELD);
		checkTinyV2Line(MappedElementKind.METHOD);
		checkTinyV2Line(MappedElementKind.METHOD_ARG);
		checkTinyV2Line(MappedElementKind.METHOD_VAR);
	}

	private void checkTinyV2Line(MappedElementKind kind) throws Exception {
		MappingFormat format = MappingFormat.TINY_2_FILE;
		String prefix = tiny2Header + "c";

		if (kind != MappedElementKind.CLASS) {
			prefix += "\tsrc\t\n\t" + (kind == MappedElementKind.FIELD ? "f" : "m");
		}

		// No source/target
		checkError(prefix, format);

		// Tabs for separation
		if (kind != MappedElementKind.CLASS) {
			checkError(prefix, format);
			prefix += "\t";
			checkError(prefix, format);

			prefix += kind == MappedElementKind.FIELD ? "I" : "()V";
			checkError(prefix, format);
		}

		prefix += "\t";
		checkError(prefix, format);
		prefix += "src";

		checkWarning(prefix, format);
		checkWorks(prefix += "\t", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		if (kind == MappedElementKind.METHOD_ARG) {
			checkWarning(prefix += "\t\tp", format);
			checkError(prefix += "\t", format);
			checkError(prefix + "\t", format);
			checkWarning(prefix + "-1", format);
			checkWarning(prefix += "0", format);
			checkWarning(prefix += "\t", format);
			checkWorks(prefix + "\t", format);
			checkWarning(prefix += "src", format);
			checkWorks(prefix += "\t", format);
			checkWorks(prefix += "dst", format);
		} else if (kind == MappedElementKind.METHOD_VAR) {
			checkWarning(prefix += "\t\tv", format);
			checkError(prefix += "\t", format);
			checkError(prefix + "\t", format);
			checkWarning(prefix + "-1", format);
			checkWarning(prefix += "0", format);
			checkError(prefix += "\t", format);
			checkWarning(prefix + "-1", format);
			checkWarning(prefix += "0", format);
			checkWorks(prefix + "\t-1\t\t", format);
			checkError(prefix += "\t", format);
			checkWarning(prefix + "-2\t\t", format);
			checkWarning(prefix += "-1", format);
			checkWarning(prefix += "\t", format);
			checkWorks(prefix + "\t", format);
			checkWarning(prefix += "src", format);
			checkWorks(prefix += "\t", format);
			checkWorks(prefix += "dst", format);
		}
	}

	@Test
	public void enigmaFile() throws Exception {
		MappingFormat format = MappingFormat.ENIGMA_FILE;
		String prefix = "CLASS";

		// Class
		checkError(prefix, format);
		checkError(prefix += " ", format);
		checkWorks(prefix += "src", format);
		checkWorks(prefix += "\n", format);

		// Method
		checkError(prefix += "\tMETHOD", format);
		checkError(prefix + " ", format);
		checkWarning(prefix += " src", format);
		checkWarning(prefix + " ", format);
		checkWorks(prefix += " ()V", format);
		checkWorks(prefix += "\n", format);

		// Method arg
		checkError(prefix += "\t\tARG", format);
		checkError(prefix += " ", format);
		checkError(prefix + " ", format);
		checkError(prefix + "src", format);
		checkWorks(prefix += "0", format);
		checkWarning(prefix += " ", format);
		checkWorks(prefix += "dst", format);
	}

	@Test
	public void proguardFile() throws Exception {
		MappingFormat format = MappingFormat.PROGUARD_FILE;
		String prefix = "src";

		// Class
		checkError(prefix + ":", format);
		checkError((prefix += " ") + ":", format);
		checkError((prefix += "->") + ":", format);
		checkWarning((prefix += " ") + ":", format);
		checkWorks(prefix += "dst" + ":", format);
		checkWorks(prefix += "\n", format);

		// Field
		checkWorks(prefix += "    ", format);
		checkError(prefix += "int", format);
		checkError(prefix += " ", format);
		checkError(prefix += "src", format);
		checkError(prefix += " ", format);
		checkError(prefix += "->", format);
		checkError(prefix += " ", format);
		checkWorks(prefix += "dst", format);
		checkInfo(prefix + " dst2", format);
		checkWorks(prefix += "\n", format);

		// Method
		checkWorks(prefix += "    ", format);
		checkError(prefix += "void", format);
		checkError(prefix += " ", format);
		checkError(prefix += "src", format);
		checkError(prefix += "()", format);
		checkError(prefix += " ", format);
		checkError(prefix += "->", format);
		checkError(prefix += " ", format);
		checkWorks(prefix += "dst", format);
		checkInfo(prefix + " dst2", format);
		checkWorks(prefix += "\n", format);
	}

	@Test
	public void srgFile() throws Exception {
		MappingFormat format = MappingFormat.SRG_FILE;

		checkSrgLine(MappedElementKind.CLASS, format);
		checkSrgLine(MappedElementKind.FIELD, format);
		checkSrgLine(MappedElementKind.METHOD, format);
	}

	@Test
	public void xsrgFile() throws Exception {
		MappingFormat format = MappingFormat.XSRG_FILE;

		checkSrgLine(MappedElementKind.CLASS, format);
		checkSrgLine(MappedElementKind.FIELD, format);
		checkSrgLine(MappedElementKind.METHOD, format);
	}

	private void checkSrgLine(MappedElementKind kind, MappingFormat format) throws Exception {
		String prefix;

		if (kind == MappedElementKind.CLASS) {
			prefix = "CL:";
		} else {
			prefix = (kind == MappedElementKind.FIELD ? "FD:" : "MD:");
		}

		// No source/target
		checkError(prefix, format);

		// Spaces for separation
		prefix += " ";
		checkError(prefix, format);
		prefix += "src";
		check(prefix, format, true, kind == MappedElementKind.CLASS ? Severity.WARNING : Severity.ERROR);
		String suffix = "";

		if (kind != MappedElementKind.CLASS) {
			prefix += "/";
			checkError(prefix, format);

			prefix += "src";
			checkWarning(prefix, format);

			if (kind == MappedElementKind.METHOD || format == MappingFormat.XSRG_FILE) {
				prefix += " ";
				checkWarning(prefix, format);

				prefix += kind == MappedElementKind.FIELD ? "I" : "()V";
				checkWarning(prefix, format);
			}

			prefix += " dst/";
			checkWarning(prefix, format);

			if (kind == MappedElementKind.METHOD) {
				suffix += " ()V";
			} else if (format == MappingFormat.XSRG_FILE) {
				suffix += " I";
			}
		} else {
			prefix += " ";
		}

		checkWarning(prefix + " " + suffix, format);
		checkWorks(prefix + "dst" + suffix, format);
	}

	@Test
	public void jamFile() throws Exception {
		MappingFormat format = MappingFormat.JAM_FILE;

		checkJamLine(MappedElementKind.CLASS, format);
		checkJamLine(MappedElementKind.FIELD, format);
		checkJamLine(MappedElementKind.METHOD, format);
		checkJamLine(MappedElementKind.METHOD_ARG, format);
	}

	private void checkJamLine(MappedElementKind kind, MappingFormat format) throws Exception {
		String prefix;

		switch (kind) {
		case CLASS:
			prefix = "CL";
			break;
		case FIELD:
			prefix = "FD";
			break;
		case METHOD:
			prefix = "MD";
			break;
		case METHOD_ARG:
			prefix = "MP";
			break;
		default:
			throw new IllegalArgumentException("Invalid kind: " + kind);
		}

		// No source/target
		checkError(prefix, format);

		// Spaces for separation
		checkError(prefix += " ", format);
		check(prefix += "src", format, true, kind == MappedElementKind.CLASS ? Severity.WARNING : Severity.ERROR);

		if (kind != MappedElementKind.CLASS) {
			checkError(prefix += " ", format);
			checkWarning(prefix += "src", format);
			checkWarning(prefix += " ", format);

			prefix += (kind == MappedElementKind.FIELD ? "I" : "()V");

			if (kind != MappedElementKind.METHOD_ARG) {
				checkWarning(prefix, format);
			} else {
				checkError(prefix, format);
				checkError(prefix += " ", format);
				checkError(prefix + "-1", format);
				checkWarning(prefix += "0", format);
			}
		}

		checkWarning(prefix += " ", format);
		checkWorks(prefix += "dst", format);
	}

	@Test
	public void csrgFile() throws Exception {
		MappingFormat format = MappingFormat.CSRG_FILE;

		checkCsrgLine(MappedElementKind.CLASS, format);
		checkCsrgLine(MappedElementKind.FIELD, format);
		checkCsrgLine(MappedElementKind.METHOD, format);
	}

	private void checkCsrgLine(MappedElementKind kind, MappingFormat format) throws Exception {
		String prefix = "srcCls srcFld dstField\n"; // So the format is detected as CSRG
		checkWorks(prefix, format);

		checkError(prefix += "src", format);
		checkWarning(prefix += " ", format);

		if (kind == MappedElementKind.FIELD || kind == MappedElementKind.METHOD) {
			checkWorks(prefix += "src", format); // detected as <srcClsName> <dstClsName>
			prefix += " ";

			if (kind == MappedElementKind.FIELD) {
				checkError(prefix, format);
			} else {
				checkError(prefix, format);
				checkWorks(prefix += "()V", format); // detected as <srcClassName> <srcFieldName> <dstFieldName>
				checkWarning(prefix + " ", format);
			}
		}

		checkWorks(prefix += "dst", format);
	}

	@Test
	public void tsrgFile() throws Exception {
		checkTsrg(MappingFormat.TSRG_FILE);
	}

	@Test
	public void tsrgV2File() throws Exception {
		checkTsrg(MappingFormat.TSRG_2_FILE);
	}

	private void checkTsrg(MappingFormat format) throws Exception {
		String prefix = format == MappingFormat.TSRG_2_FILE ? tsrg2Header : "";

		// Class
		checkWorks(prefix, format);
		checkError(prefix += "src", format);
		checkWarning(prefix += " ", format);
		checkWarning(prefix + " ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		// Field
		for (int i = 0; i < 2; i++) {
			checkError(prefix += "\t", format);
			checkError(prefix += "src", format);
			checkError(prefix += " ", format);

			if (format == MappingFormat.TSRG_2_FILE && i == 1) {
				checkWorks(prefix += "I", format); // detected as <srcFldName> <dstFldName>
				checkWarning(prefix += " ", format);
				checkWarning(prefix + " ", format);
			}

			checkWorks(prefix += "dst", format);
			checkWorks(prefix += "\n", format);

			if (format != MappingFormat.TSRG_2_FILE) break;
		}

		// Method
		checkError(prefix += "\t", format);
		checkError(prefix += "src", format);
		checkError(prefix += " ", format);
		checkError(prefix + " ", format);
		checkError(prefix += "()V", format);
		checkWarning(prefix += " ", format);
		checkWarning(prefix + " ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		if (format == MappingFormat.TSRG_2_FILE) {
			checkError(prefix += "\t", format);
			checkWarning(prefix += "\t", format);
			checkWorks(prefix += "static", format);
			checkWorks(prefix += "\n", format);
		}

		// Method arg
		if (format == MappingFormat.TSRG_2_FILE) {
			checkError(prefix += "\t", format);
			checkWarning(prefix += "\t", format);
			checkWarning(prefix + " ", format);
			checkWarning(prefix + "-1", format);
			checkWarning(prefix += "0", format);
			checkWarning(prefix += " ", format);
			checkError(prefix += "src", format);
			checkWarning(prefix += " ", format);
			checkWarning(prefix + " ", format);
			checkWorks(prefix += "dst", format);
			checkWorks(prefix += "\n", format);
		}
	}

	@Test
	public void recafSimpleFile() throws Exception {
		MappingFormat format = MappingFormat.RECAF_SIMPLE_FILE;
		String prefix = "";

		// Class
		checkWorks(prefix, format);
		checkError(prefix += "src", format);
		checkError(prefix += " ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		// Field
		checkError(prefix += "src", format);
		checkError(prefix += ".", format);
		checkError(prefix += "src", format);
		checkError(prefix += " ", format);
		checkWorks(prefix += "I", format); // detected as <srcClsName> <dstClsName>
		checkWorks(prefix += " ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		// Method
		checkError(prefix += "src", format);
		checkError(prefix += ".", format);
		checkError(prefix += "src", format);
		checkError(prefix += "()V", format);
		checkError(prefix += " ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);
	}

	@Test
	public void jobfFile() throws Exception {
		MappingFormat format = MappingFormat.JOBF_FILE;
		String prefix = "";

		// Class
		checkWorks(prefix, format);
		checkError(prefix += "c", format);
		checkError(prefix += " ", format);
		checkError(prefix += "src", format);
		checkWarning(prefix += " = ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		// Field
		checkError(prefix += "f", format);
		checkError(prefix += " ", format);
		checkError(prefix += "src", format);
		checkError(prefix += ".", format);
		checkError(prefix += "src", format);
		checkError(prefix += ":", format);
		checkError(prefix += "I", format);
		checkWarning(prefix += " = ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		// Method
		checkError(prefix += "m", format);
		checkError(prefix += " ", format);
		checkError(prefix += "src", format);
		checkError(prefix += ".", format);
		checkError(prefix += "src", format);
		checkError(prefix += "()", format);
		checkError(prefix += "I", format);
		checkWarning(prefix += " = ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);

		// Inner class
		checkError(prefix += "c", format);
		checkError(prefix += " ", format);
		checkError(prefix += "src$src", format);
		checkWarning(prefix += " = ", format);
		checkWorks(prefix += "dst", format);
		checkWorks(prefix += "\n", format);
	}

	private void checkThrows(String fileContent, MappingFormat format) throws Exception {
		assertThrows(IOException.class, () -> checkWorks(fileContent, format));
	}

	private void check(String fileContent, MappingFormat format, boolean shouldError, @Nullable Severity expectedSeverity) throws Exception {
		if (!shouldError) {
			checkWorks(fileContent, format);
		} else {
			checkSeverity(fileContent, format, expectedSeverity);
		}
	}

	private void checkWorks(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, null);
	}

	private void checkInfo(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, Severity.INFO);
	}

	private void checkWarning(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, Severity.WARNING);
	}

	private void checkError(String fileContent, MappingFormat format) throws Exception {
		checkSeverity(fileContent, format, Severity.ERROR);
	}

	private void checkSeverity(String fileContent, MappingFormat format, @Nullable Severity expectedSeverity) throws Exception {
		List<ParsingError> errors = read(fileContent, format);

		if (expectedSeverity != null) {
			assertFalse(errors.isEmpty(), "Expected error not found");
			assertEquals(expectedSeverity, errors.get(0).getSeverity(), "Wrong error severity");
		} else {
			assertEquals(0, errors.size(), "Unexpected error found");
		}
	}

	private List<ParsingError> read(String fileContent, MappingFormat format) throws Exception {
		ErrorCollector errorCollector = ErrorCollector.create();
		MappingReader.read(new StringReader(fileContent), format, new NopMappingVisitor(true), errorCollector);
		return errorCollector.getErrors();
	}
}

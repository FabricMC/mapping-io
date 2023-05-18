/*
 * Copyright (c) 2022 FabricMC
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

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public final class SimpleReader {

	public static void read(Reader reader, MappingVisitor visitor) throws IOException {
		BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
		read(br, visitor);
	}

	public static void read(BufferedReader reader, MappingVisitor visitor) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {

			String[] split = line.split(" ");
			String obf = split[0];
			String deobf = split[1];

			if (!(line.contains(".") || line.contains("("))) {
				if (visitor.visitClass(obf)) {
					visitor.visitDstName(MappedElementKind.CLASS, 0, deobf);
					visitor.visitElementContent(MappedElementKind.CLASS);
				}
			} else if (line.contains(".") && !line.contains("(")) {
				if (visitor.visitField(obf.substring(obf.lastIndexOf(".") + 1), null)) {
					visitor.visitDstName(MappedElementKind.FIELD, 0, deobf);
					visitor.visitElementContent(MappedElementKind.FIELD);
				}
			} else if (line.contains(".") && line.contains("(")) {
				if (visitor.visitMethod(obf.substring(obf.lastIndexOf(".") + 1, obf.lastIndexOf("(")), obf.substring(obf.lastIndexOf("(")))) {
					visitor.visitDstName(MappedElementKind.METHOD, 0, deobf);
					visitor.visitElementContent(MappedElementKind.METHOD);
				}
			}
		}
	}
}

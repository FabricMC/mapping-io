/*
 * Copyright (c) 2021 FabricMC
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
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.fabricmc.mappingio.MappingVisitor;

public class JarReader {
	public static void read(Path path, MappingVisitor mappingVisitor) throws IOException {
		AnalyzingVisitor analyzingVisitor = new AnalyzingVisitor(mappingVisitor);

		ZipFile zipFile = new ZipFile(path.toFile());
		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		mappingVisitor.visitNamespaces("jar", new ArrayList<String>());

		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			String entryName = entry.getName();
			String parentName = Paths.get("/", entryName).getParent().toString();

			if (entryName.endsWith(".class") && !parentName.contains("-")) {
				processClass(zipFile.getInputStream(entry), analyzingVisitor);
			}
		}
	}

	private static void processClass(InputStream inputStream, AnalyzingVisitor analyzingVisitor) throws IOException {
		ClassReader reader = new ClassReader(inputStream);
		reader.accept(analyzingVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
	}

	private static final class AnalyzingVisitor extends ClassVisitor {
		AnalyzingVisitor(MappingVisitor mappingVisitor) {
			super(Integer.getInteger("mappingIo.asmApiVersion", Opcodes.ASM9));

			this.mappingVisitor = mappingVisitor;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			try {
				mappingVisitor.visitClass(name);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			try {
				mappingVisitor.visitField(name, descriptor);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			try {
				mappingVisitor.visitMethod(name, descriptor);
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		private final MappingVisitor mappingVisitor;
	}
}

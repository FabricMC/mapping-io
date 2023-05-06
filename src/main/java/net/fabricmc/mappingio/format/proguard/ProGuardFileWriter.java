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

package net.fabricmc.mappingio.format.proguard;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Type;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.ProgressListener;
import net.fabricmc.mappingio.format.AbstractMappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;

/**
 * A mapping writer for the ProGuard mapping format.
 * Note that this format is very basic: it only supports
 * one namespace pair and only classes, methods and fields
 * without comments.
 *
 * @see <a href="https://www.guardsquare.com/manual/tools/retrace">Official format documentation</a>
 */
public final class ProGuardFileWriter extends AbstractMappingWriter {
	private int dstNamespace = -1;
	private final String dstNamespaceString;

	/**
	 * Constructs a ProGuard mapping writer that uses
	 * the first destination namespace (index 0).
	 *
	 * @param writer the writer where the mappings will be written
	 */
	public ProGuardFileWriter(Writer writer, ProgressListener progressListener) {
		this(writer, 0, progressListener);
	}

	/**
	 * Constructs a ProGuard mapping writer.
	 *
	 * @param writer       the writer where the mappings will be written
	 * @param dstNamespace the namespace index to write as the destination namespace, must be at least 0
	 */
	public ProGuardFileWriter(Writer writer, int dstNamespace, ProgressListener progressListener) {
		super(MappingFormat.PROGUARD_FILE, writer, progressListener, "Writing Proguard file");

		this.writer = Objects.requireNonNull(writer, "writer cannot be null");
		this.dstNamespace = dstNamespace;
		this.dstNamespaceString = null;

		if (dstNamespace < 0) {
			throw new IllegalArgumentException("Namespace must be non-negative, found " + dstNamespace);
		}
	}

	/**
	 * Constructs a ProGuard mapping writer.
	 *
	 * @param writer       the writer where the mappings will be written
	 * @param dstNamespace the namespace name to write as the destination namespace
	 */
	public ProGuardFileWriter(Writer writer, String dstNamespace, ProgressListener progressListener) {
		super(MappingFormat.PROGUARD_FILE, writer, progressListener, "Writing Proguard file");

		this.writer = Objects.requireNonNull(writer, "writer cannot be null");
		this.dstNamespaceString = Objects.requireNonNull(dstNamespace, "namespace cannot be null");
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		if (dstNamespaceString != null) {
			dstNamespace = dstNamespaces.indexOf(dstNamespaceString);

			if (dstNamespace == -1) {
				throw new RuntimeException("Invalid destination namespace '" + dstNamespaceString + "' not in [" + String.join(", ", dstNamespaces) + ']');
			}
		}

		if (dstNamespace >= dstNamespaces.size()) {
			throw new IndexOutOfBoundsException("Namespace " + dstNamespace + " doesn't exist in [" + String.join(", ", dstNamespaces) + ']');
		}
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		super.visitClass(srcName);
		writer.write(toJavaClassName(srcName));
		writeArrow();
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		super.visitField(srcName, srcDesc);

		writeIndent();
		writer.write(toJavaType(srcDesc));
		writer.write(' ');
		writer.write(srcName);
		writeArrow();

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		super.visitMethod(srcName, srcDesc);

		Type type = Type.getMethodType(srcDesc);
		writeIndent();
		writer.write(toJavaType(type.getReturnType().getDescriptor()));
		writer.write(' ');
		writer.write(srcName);
		writer.write('(');
		Type[] args = type.getArgumentTypes();

		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				writer.write(',');
			}

			writer.write(toJavaType(args[i].getDescriptor()));
		}

		writer.write(')');
		writeArrow();
		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException {
		super.visitMethodArg(argPosition, lvIndex, srcName);
		return false;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
		super.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
		return false;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (this.dstNamespace != namespace) {
			return;
		}

		if (targetKind == MappedElementKind.CLASS) {
			writer.write(toJavaClassName(name));
			writer.write(':');
		} else {
			writer.write(name);
		}

		writer.write('\n');
	}

	private void writeArrow() throws IOException {
		writer.write(" -> ");
	}

	private void writeIndent() throws IOException {
		// This has to be exactly 4 spaces.
		writer.write("    ");
	}

	/**
	 * Replaces the slashes as package separators with dots
	 * since ProGuard uses Java-like dotted class names.
	 */
	private static String toJavaClassName(String name) {
		return name.replace('/', '.');
	}

	private static String toJavaType(String descriptor) {
		StringBuilder result = new StringBuilder();
		int arrayLevel = 0;

		for (int i = 0; i < descriptor.length(); i++) {
			switch (descriptor.charAt(i)) {
			case '[': arrayLevel++; break;
			case 'B': result.append("byte"); break;
			case 'S': result.append("short"); break;
			case 'I': result.append("int"); break;
			case 'J': result.append("long"); break;
			case 'F': result.append("float"); break;
			case 'D': result.append("double"); break;
			case 'C': result.append("char"); break;
			case 'Z': result.append("boolean"); break;
			case 'V': result.append("void"); break;
			case 'L':
				while (i + 1 < descriptor.length()) {
					char c = descriptor.charAt(++i);

					if (c == '/') {
						result.append('.');
					} else if (c == ';') {
						break;
					} else {
						result.append(c);
					}
				}

				break;
			default: throw new IllegalArgumentException("Unknown character in descriptor: " + descriptor.charAt(i));
			}
		}

		// TODO: This can be replaced by String.repeat in modern Java
		while (arrayLevel > 0) {
			result.append("[]");
			arrayLevel--;
		}

		return result.toString();
	}
}

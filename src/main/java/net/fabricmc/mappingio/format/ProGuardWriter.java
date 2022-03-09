package net.fabricmc.mappingio.format;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.objectweb.asm.Type;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingWriter;

/**
 * A mapping writer for the ProGuard mapping format.
 * Note that this format is very basic: it only supports
 * one namespace pair and only classes, methods and fields
 * without comments.
 *
 * @see <a href="https://www.guardsquare.com/manual/tools/retrace">Official format documentation</a>
 */
public final class ProGuardWriter implements MappingWriter {
	private final BufferedWriter writer;
	private int namespace = -1;
	private String namespaceString;

	/**
	 * Constructs a ProGuard mapping writer.
	 *
	 * @param writer    the writer where the mappings will be written
	 * @param namespace the namespace index to write as the destination namespace
	 */
	public ProGuardWriter(BufferedWriter writer, int namespace) {
		this.writer = writer;
		this.namespace = namespace;
	}

	/**
	 * Constructs a ProGuard mapping writer.
	 *
	 * @param writer    the writer where the mappings will be written
	 * @param namespace the namespace name to write as the destination namespace
	 */
	public ProGuardWriter(BufferedWriter writer, String namespace) {
		this.writer = writer;
		this.namespaceString = namespace;
	}

	/**
	 * Constructs a ProGuard mapping writer that uses
	 * the first destination namespace (index 0).
	 *
	 * @param writer the writer where the mappings will be written
	 */
	public ProGuardWriter(BufferedWriter writer) {
		this(writer, 0);
	}

	/**
	 * Closes the internal {@link BufferedWriter}.
	 *
	 * @throws IOException if an IO error occurs
	 */
	@Override
	public void close() throws IOException {
		writer.close();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		if (namespaceString != null) {
			namespace = dstNamespaces.indexOf(namespaceString);
		}
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		writer.write(toJavaClassName(srcName));
		writeArrow();
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		writeIndent();
		writer.write(toJavaType(srcDesc));
		writer.write(' ');
		writer.write(srcName);
		writeArrow();
		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
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
		// ignored
		return false;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName) throws IOException {
		// ignored
		return false;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (this.namespace != namespace) {
			return;
		}

		if (targetKind == MappedElementKind.CLASS) {
			writer.write(toJavaClassName(name));
			writer.write(':');
		} else {
			writer.write(name);
		}

		writer.newLine();
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		// ignored
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

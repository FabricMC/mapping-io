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

package net.fabricmc.mappingio.extras;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MappingTreeView;

public final class ClassAnalysisDescCompleter {
	private ClassAnalysisDescCompleter() {
	}

	public static void process(Path path, String namespace, MappingTree mappingTree) throws IOException {
		AnalyzingVisitor visitor = new AnalyzingVisitor(namespace, mappingTree);

		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new DirVisitor(visitor));
		} else {
			processFile(path, null, visitor);
		}
	}

	private static final class DirVisitor extends SimpleFileVisitor<Path> {
		DirVisitor(AnalyzingVisitor visitor) {
			this.visitor = visitor;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			buffer = processFile(file, buffer, visitor);

			return FileVisitResult.CONTINUE;
		}

		private final AnalyzingVisitor visitor;
		ByteBuffer buffer;
	}

	@SuppressWarnings("resource")
	private static ByteBuffer processFile(Path file, ByteBuffer buffer, AnalyzingVisitor visitor) throws IOException {
		String fileName = file.getFileName().toString().toLowerCase(Locale.ENGLISH);

		if (fileName.endsWith(".jar")) {
			URI uri = file.toUri();

			try {
				uri = new URI("jar:".concat(uri.getScheme()), uri.getHost(), uri.getPath(), uri.getFragment());
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}

			FileSystem fs = null;
			boolean closeFs = false;

			try {
				try {
					fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
					closeFs = true;
				} catch (FileSystemAlreadyExistsException e) {
					fs = FileSystems.getFileSystem(uri);
				}

				DirVisitor dirVisitor = new DirVisitor(visitor);

				for (Path rootDir : fs.getRootDirectories()) {
					Files.walkFileTree(rootDir, dirVisitor);
				}

				buffer = dirVisitor.buffer;
			} finally {
				if (closeFs) fs.close();
			}
		} else if (fileName.endsWith(".class")) {
			try (SeekableByteChannel channel = Files.newByteChannel(file)) {
				if (buffer == null) buffer = ByteBuffer.allocate((int) Math.min(channel.size() + 1, 100_000_000));

				while (channel.read(buffer) >= 0) {
					if (!buffer.hasRemaining()) {
						ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
						buffer.flip();
						newBuffer.put(buffer);
						buffer = newBuffer;
					}
				}
			}

			buffer.flip();
			processClass(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining(), visitor);
			buffer.clear();
		}

		return buffer;
	}

	public static void processClass(byte[] classBytes, String namespace, MappingTree mappingTree) {
		processClass(classBytes, 0, classBytes.length, new AnalyzingVisitor(namespace, mappingTree));
	}

	public static void processClass(byte[] classBytes, int offset, int length, String namespace, MappingTree mappingTree) {
		processClass(classBytes, offset, length, new AnalyzingVisitor(namespace, mappingTree));
	}

	private static void processClass(byte[] classBytes, int offset, int length, AnalyzingVisitor visitor) {
		ClassReader reader = new ClassReader(classBytes, offset, length);
		reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
	}

	private static final class AnalyzingVisitor extends ClassVisitor {
		AnalyzingVisitor(@Nullable String namespace, MappingTree mappingTree) {
			super(Integer.getInteger("mappingIo.asmApiVersion", Opcodes.ASM9));

			this.namespace = namespace != null ? mappingTree.getNamespaceId(namespace) : MappingTreeView.SRC_NAMESPACE_ID;
			if (this.namespace == MappingTreeView.NULL_NAMESPACE_ID) throw new IllegalArgumentException("Unknown namespace: "+namespace);

			this.mappingTree = mappingTree;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			cls = mappingTree.getClass(name, namespace);
		}

		@Override
		@Nullable
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (cls != null) {
				FieldMapping field = cls.getField(name, descriptor, namespace);

				if (field != null && field.getSrcDesc() == null) {
					field.setSrcDesc(mappingTree.mapDesc(descriptor, namespace, MappingTreeView.SRC_NAMESPACE_ID));
				}
			}

			return null;
		}

		@Override
		@Nullable
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if (cls != null) {
				MethodMapping method = cls.getMethod(name, descriptor, namespace);

				if (method != null && method.getSrcDesc() == null) {
					method.setSrcDesc(mappingTree.mapDesc(descriptor, namespace, MappingTreeView.SRC_NAMESPACE_ID));
				}
			}

			return null;
		}

		private final int namespace;
		private final MappingTree mappingTree;
		private ClassMapping cls;
	}
}

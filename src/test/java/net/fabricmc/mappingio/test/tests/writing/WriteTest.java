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

package net.fabricmc.mappingio.test.tests.writing;

import java.io.IOException;
import java.nio.file.Path;

import net.neoforged.srgutils.IMappingFile;
import net.neoforged.srgutils.INamedMappingFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.adapter.MappingNsCompleter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.test.TestMappings;
import net.fabricmc.mappingio.test.TestMappings.MappingDir;
import net.fabricmc.mappingio.test.TestUtil;
import net.fabricmc.mappingio.test.visitors.SubsetAsserter;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class WriteTest {
	@TempDir
	private static Path targetDir;

	@Test
	public void run() throws Exception {
		for (MappingDir dir : TestMappings.values()) {
			for (MappingFormat format : MappingFormat.values()) {
				check(dir, format);
			}
		}
	}

	private void check(MappingDir dir, MappingFormat format) throws Exception {
		if (!dir.supportsGeneration() || !format.hasWriter) {
			return;
		}

		Path path = targetDir.resolve(TestUtil.getFileName(format));
		MappingTreeView tree = dir.generate(new MemoryMappingTree());
		MappingVisitor target = MappingWriter.create(path, format);

		if (dir.isIn(TestMappings.PROPAGATION.BASE_DIR) && !format.features().hasNamespaces()) {
			target = new MappingNsCompleter(target);
		}

		if (dir == TestMappings.READING.REPEATED_ELEMENTS) {
			boolean isEnigma = format == MappingFormat.ENIGMA_FILE || format == MappingFormat.ENIGMA_DIR;
			TestMappings.generateRepeatedElements(target, !isEnigma, !isEnigma);
		} else {
			dir.generate(target);
		}

		if (!(dir == TestMappings.PROPAGATION.UNPROPAGATED && format == MappingFormat.ENIGMA_FILE)) {
			// See ValidContentReadTest for explanation
			readWithMio(tree, path, format);
		}

		readWithLorenz(path, format);
		readWithSrgUtils(tree, format);
	}

	private void readWithMio(MappingTreeView origTree, Path outputPath, MappingFormat outputFormat) throws Exception {
		VisitableMappingTree writtenTree = new MemoryMappingTree();
		MappingReader.read(outputPath, outputFormat, writtenTree);

		writtenTree.accept(new FlatAsRegularMappingVisitor(new SubsetAsserter(origTree, null, outputFormat)));
		origTree.accept(new FlatAsRegularMappingVisitor(new SubsetAsserter(writtenTree, outputFormat, null)));
	}

	private void readWithLorenz(Path path, MappingFormat format) throws Exception {
		org.cadixdev.lorenz.io.MappingFormat lorenzFormat = TestUtil.toLorenzFormat(format);
		if (lorenzFormat == null) return;
		lorenzFormat.read(path);
	}

	private void readWithSrgUtils(MappingTreeView tree, MappingFormat format) throws Exception {
		IMappingFile.Format srgUtilsFormat = TestUtil.toSrgUtilsFormat(format);
		if (srgUtilsFormat == null) return;

		// TODO: Remove once https://github.com/neoforged/SRGUtils/issues/7 is fixed
		if (format == MappingFormat.PROGUARD_FILE) return;

		// SrgUtils can't handle empty dst names
		VisitableMappingTree dstNsCompTree = new MemoryMappingTree();
		tree.accept(
				// TODO: Remove once https://github.com/neoforged/SRGUtils/issues/9 is fixed
				new MappingNsCompleter(
						// TODO: Remove once https://github.com/neoforged/SRGUtils/issues/8 is fixed
						new ForwardingMappingVisitor(dstNsCompTree) {
							@Override
							public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
								return !(format == MappingFormat.TINY_2_FILE && targetKind == MappedElementKind.METHOD_VAR);
							}
						}));

		Path path = TestUtil.writeToDir(dstNsCompTree, targetDir, format);
		INamedMappingFile.load(path.toFile());
	}
}

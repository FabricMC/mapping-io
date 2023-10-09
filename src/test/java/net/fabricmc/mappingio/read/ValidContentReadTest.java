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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import net.fabricmc.mappingio.FlatMappingVisitor;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.TestHelper;
import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodArgMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodVarMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class ValidContentReadTest {
	private static Path dir;
	private static MappingTree origTree;

	@BeforeAll
	public static void setup() throws Exception {
		dir = TestHelper.getResource("/read/valid/");
		origTree = TestHelper.createTestTree();
	}

	@Test
	public void enigmaFile() throws Exception {
		checkCommonContent("enigma.mappings", MappingFormat.ENIGMA_FILE);
	}

	@Test
	public void enigmaDirectory() throws Exception {
		checkCommonContent("enigma-dir", MappingFormat.ENIGMA_DIR);
	}

	@Test
	public void tinyFile() throws Exception {
		checkCommonContent("tiny.tiny", MappingFormat.TINY_FILE);
	}

	@Test
	public void tinyV2File() throws Exception {
		checkCommonContent("tinyV2.tiny", MappingFormat.TINY_2_FILE);
	}

	@Test
	public void srgFile() throws Exception {
		checkCommonContent("srg.srg", MappingFormat.SRG_FILE);
	}

	@Test
	public void tsrgFile() throws Exception {
		checkCommonContent("tsrg.tsrg", MappingFormat.TSRG_FILE);
	}

	@Test
	public void tsrg2File() throws Exception {
		checkCommonContent("tsrg2.tsrg", MappingFormat.TSRG_2_FILE);
	}

	private VisitableMappingTree checkCommonContent(String path, MappingFormat format) throws Exception {
		VisitableMappingTree tree = new MemoryMappingTree();
		format.reader.read(dir.resolve(path), tree);

		assertSubset(tree, format, origTree, null);
		assertSubset(origTree, null, tree, format);
		return tree;
	}

	private void assertSubset(MappingTree subTree, MappingFormat subFormat, MappingTree supTree, MappingFormat supFormat) throws Exception {
		int supDstNsCount = subTree.getMaxNamespaceId();
		boolean subHasNamespaces = subFormat == null ? true : subFormat.hasNamespaces;
		boolean supHasNamespaces = supFormat == null ? true : supFormat.hasNamespaces;
		boolean supHasFieldDesc = supFormat == null ? true : supFormat.hasFieldDescriptors;
		boolean supHasArgs = supFormat == null ? true : supFormat.supportsArgs;
		boolean supHasVars = supFormat == null ? true : supFormat.supportsLocals;
		boolean supHasComments = supFormat == null ? true : supFormat.supportsComments;

		subTree.accept(new FlatAsRegularMappingVisitor(new FlatMappingVisitor() {
			@Override
			public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
				assertTrue(srcNamespace.equals(subHasNamespaces ? supTree.getSrcNamespace() : MappingUtil.NS_SOURCE_FALLBACK));

				if (!subHasNamespaces) {
					assertTrue(dstNamespaces.size() == 1);
					assertTrue(dstNamespaces.get(0).equals(MappingUtil.NS_TARGET_FALLBACK));
					return;
				}

				for (String dstNs : dstNamespaces) {
					boolean contained = supTree.getDstNamespaces().contains(dstNs);

					if (!supHasNamespaces) {
						if (contained) return;
					} else {
						assertTrue(contained);
					}
				}

				if (!supHasNamespaces) throw new AssertionFailedError("SubTree namespace not contained in SupTree");
			}

			@Override
			public boolean visitClass(String srcName, String[] dstNames) throws IOException {
				ClassMapping supCls = supTree.getClass(srcName);
				Map<String, String> supDstNamesByNsName = new HashMap<>();

				for (int supNs = 0; supNs < supDstNsCount; supNs++) {
					supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supCls.getDstName(supNs));
				}

				for (int subNs = 0; subNs < dstNames.length; subNs++) {
					String supDstName = supDstNamesByNsName.get(subTree.getNamespaceName(subNs));
					if (!supHasNamespaces && supDstName == null) continue;
					assertTrue(dstNames[subNs] == null || Objects.equals(dstNames[subNs], supDstName));
				}

				return true;
			}

			@Override
			public void visitClassComment(String srcName, String[] dstNames, String comment) throws IOException {
				if (!supHasComments) return;
				assertEquals(supTree.getClass(srcName).getComment(), comment);
			}

			@Override
			public boolean visitField(String srcClsName, String srcName, String srcDesc,
					String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
				FieldMapping supFld = supTree.getClass(srcClsName).getField(srcName, srcDesc);
				Map<String, String[]> supDstDataByNsName = new HashMap<>();

				for (int supNs = 0; supNs < supDstNsCount; supNs++) {
					supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supFld.getDstName(supNs), supFld.getDstDesc(supNs)});
				}

				for (int subNs = 0; subNs < dstNames.length; subNs++) {
					String[] supDstData = supDstDataByNsName.get(subTree.getNamespaceName(subNs));
					if (!supHasNamespaces && supDstData == null) continue;

					String supDstName = supDstData[0];
					assertTrue(dstNames[subNs] == null || Objects.equals(dstNames[subNs], supDstName));

					if (!supHasFieldDesc) continue;
					String supDstDesc = supDstData[1];
					assertTrue(dstDescs == null || dstDescs[subNs] == null || Objects.equals(dstDescs[subNs], supDstDesc));
				}

				return true;
			}

			@Override
			public void visitFieldComment(String srcClsName, String srcName, String srcDesc,
					String[] dstClsNames, String[] dstNames, String[] dstDescs, String comment) throws IOException {
				if (!supHasComments) return;
				assertEquals(supTree.getClass(srcName).getField(srcName, srcDesc).getComment(), comment);
			}

			@Override
			public boolean visitMethod(String srcClsName, String srcName, String srcDesc,
					String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
				MethodMapping supMth = supTree.getClass(srcClsName).getMethod(srcName, srcDesc);
				Map<String, String[]> supDstDataByNsName = new HashMap<>();

				for (int supNs = 0; supNs < supDstNsCount; supNs++) {
					supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supMth.getDstName(supNs), supMth.getDstDesc(supNs)});
				}

				for (int subNs = 0; subNs < dstNames.length; subNs++) {
					String[] supDstData = supDstDataByNsName.get(subTree.getNamespaceName(subNs));
					if (!supHasNamespaces && supDstData == null) continue;

					String supDstName = supDstData[0];
					assertTrue(dstNames[subNs] == null || Objects.equals(dstNames[subNs], supDstName));

					String supDstDesc = supDstData[1];
					assertTrue(dstDescs == null || dstDescs[subNs] == null || Objects.equals(dstDescs[subNs], supDstDesc));
				}

				return true;
			}

			@Override
			public void visitMethodComment(String srcClsName, String srcName, String srcDesc,
					String[] dstClsNames, String[] dstNames, String[] dstDescs, String comment) throws IOException {
				if (!supHasComments) return;
				assertEquals(supTree.getClass(srcName).getMethod(srcName, srcDesc).getComment(), comment);
			}

			@Override
			public boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc, int argPosition, int lvIndex, String srcName,
					String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames) throws IOException {
				if (!supHasArgs) return false;
				MethodArgMapping supArg = supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getArg(argPosition, lvIndex, srcName);
				Map<String, String> supDstNamesByNsName = new HashMap<>();

				for (int supNs = 0; supNs < supDstNsCount; supNs++) {
					supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supArg.getDstName(supNs));
				}

				for (int subNs = 0; subNs < dstNames.length; subNs++) {
					String supDstName = supDstNamesByNsName.get(subTree.getNamespaceName(subNs));
					if (!supHasNamespaces && supDstName == null) continue;
					assertEquals(dstNames[subNs], supDstName);
				}

				return true;
			}

			@Override
			public void visitMethodArgComment(String srcClsName, String srcMethodName, String srcMethodDesc, int argPosition, int lvIndex, String srcArgName,
					String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames, String comment) throws IOException {
				if (!supHasComments) return;
				assertEquals(supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getArg(argPosition, lvIndex, comment).getComment(), comment);
			}

			@Override
			public boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc,
					int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName,
					String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames) throws IOException {
				if (!supHasVars) return false;
				MethodVarMapping supVar = supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
				Map<String, String> supDstNamesByNsName = new HashMap<>();

				for (int supNs = 0; supNs < supDstNsCount; supNs++) {
					supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supVar.getDstName(supNs));
				}

				for (int subNs = 0; subNs < dstNames.length; subNs++) {
					String supDstName = supDstNamesByNsName.get(subTree.getNamespaceName(subNs));
					if (!supHasNamespaces && supDstName == null) continue;
					assertEquals(dstNames[subNs], supDstName);
				}

				return true;
			}

			@Override
			public void visitMethodVarComment(String srcClsName, String srcMethodName, String srcMethodDesc,
					int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
					String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames, String comment) throws IOException {
				if (!supHasComments) return;
				assertEquals(supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, comment).getComment(), comment);
			}
		}));
	}
}

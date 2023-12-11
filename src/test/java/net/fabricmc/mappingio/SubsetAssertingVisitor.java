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

package net.fabricmc.mappingio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.MappingFormat.FeatureSet;
import net.fabricmc.mappingio.format.MappingFormat.FeatureSet.ElementCommentSupport;
import net.fabricmc.mappingio.format.MappingFormat.FeatureSet.OptionalFeature;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodArgMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodVarMappingView;

public class SubsetAssertingVisitor implements FlatMappingVisitor {
	/**
	 * @param supTree The superset tree.
	 * @param supFormat The superset format, or null if supTree has all the original data.
	 * @param subFormat The subset format, or null if lossless (i.e. if the visits are coming from a tree).
	 */
	public SubsetAssertingVisitor(MappingTreeView supTree, @Nullable MappingFormat supFormat, @Nullable MappingFormat subFormat) {
		this.supTree = supTree;
		this.supDstNsCount = supTree.getMaxNamespaceId();
		this.supFeatures = supFormat == null ? new FeatureSet(true) : supFormat.features;
		this.subFeatures = subFormat == null ? new FeatureSet(true) : subFormat.features;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		String expectedSrcNs = subFeatures.hasNamespaces() ? supTree.getSrcNamespace() : MappingUtil.NS_SOURCE_FALLBACK;
		assertEquals(expectedSrcNs, srcNamespace, "Incoming mappings have different source namespace than supTree");
		this.subDstNamespaces = dstNamespaces;

		if (!subFeatures.hasNamespaces()) {
			assertEquals(1, dstNamespaces.size(), "Incoming mappings have multiple namespaces despite declaring not to support them");
			assertEquals(MappingUtil.NS_TARGET_FALLBACK, dstNamespaces.get(0), "Incoming mappings don't have default namespace name of non-namespaced formats");
			return;
		}

		for (int i = 0; i < dstNamespaces.size(); i++) {
			String dstNs = dstNamespaces.get(i);
			boolean contained = supTree.getDstNamespaces().contains(dstNs);

			if (!supFeatures.hasNamespaces()) {
				// One of the sub namespaces must equal the sup namespace
				if (contained) {
					subNsIfSupNotNamespaced = i;
					break;
				}

				if (i < dstNamespaces.size() - 1) continue;
			}

			assertTrue(contained, "Incoming namespace not contained in supTree: " + dstNs);
		}
	}

	@Override
	public boolean visitClass(String srcName, @Nullable String[] dstNames) throws IOException {
		if (!supFeatures.supportsClasses()) return true; // sub-elements might still be supported

		ClassMappingView supCls = supTree.getClass(srcName);
		boolean supHasDstNames = supFeatures.classes().dstNames() != OptionalFeature.UNSUPPORTED;
		boolean subHasDstNames = subFeatures.classes().dstNames() != OptionalFeature.UNSUPPORTED;

		if (supCls == null) { // SupTree doesn't have this class, ensure the incoming mappings don't have any data for it
			if (supHasDstNames && subHasDstNames) {
				String[] subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};
				assertTrue(isEmpty(subDstNames), "Incoming class not contained in SupTree: " + srcName);
			}

			return true;
		}

		Map<String, String> supDstNamesByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supCls.getDstName(supNs));
		}

		if (supHasDstNames && subHasDstNames && dstNames != null) {
			for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
				String supDstName = supDstNamesByNsName.get(subDstNamespaces.get(subNs));
				if (supDstName == null && !supFeatures.hasNamespaces()) continue;

				String subDstName = dstNames[subNs];
				if (subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName))) continue; // uncompleted dst name

				assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming class destination name differs from supTree");
			}
		}

		return true;
	}

	@Override
	public void visitClassComment(String srcName, @Nullable String[] dstNames, String comment) throws IOException {
		if (!supFeatures.supportsClasses() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		assertEquals(supTree.getClass(srcName).getComment(), comment, "Incoming class comment not contained in supTree: " + srcName);
	}

	@Override
	public boolean visitField(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs) throws IOException {
		if (!supFeatures.supportsFields()) return true;

		FieldMappingView supFld = supTree.getClass(srcClsName).getField(srcName, srcDesc);
		boolean supHasSrcDescs = supFeatures.fields().srcDescs() != OptionalFeature.UNSUPPORTED;
		boolean subHasSrcDescs = subFeatures.fields().srcDescs() != OptionalFeature.UNSUPPORTED;
		boolean supHasDstNames = supFeatures.fields().dstNames() != OptionalFeature.UNSUPPORTED;
		boolean subHasDstNames = subFeatures.fields().dstNames() != OptionalFeature.UNSUPPORTED;
		boolean supHasDstDescs = supFeatures.fields().dstDescs() != OptionalFeature.UNSUPPORTED;
		boolean subHasDstDescs = subFeatures.fields().dstDescs() != OptionalFeature.UNSUPPORTED;

		if (supFld == null) { // SupTree doesn't have this field, ensure the incoming mappings don't have any data for it
			String[] subDstNames = null;
			String[] subDstDescs = null;

			if (supHasDstNames && subHasDstNames) subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};
			if (supHasDstDescs && subHasDstDescs) subDstDescs = supFeatures.hasNamespaces() || dstDescs == null ? dstDescs : new String[]{dstDescs[subNsIfSupNotNamespaced]};

			assertTrue(isEmpty(subDstNames) && isEmpty(subDstDescs), "Incoming field not contained in SupTree: " + srcName + ", descriptor: " + srcDesc);
			return true;
		}

		Map<String, String[]> supDstDataByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supFld.getDstName(supNs), supFld.getDstDesc(supNs)});
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasSrcDescs && subHasSrcDescs && srcDesc != null) {
				assertEquals(supFld.getSrcDesc(), srcDesc, "Incoming field source descriptor differs from supTree");
			}

			String[] supDstData = supDstDataByNsName.get(subDstNamespaces.get(subNs));
			if (supDstData == null && !supFeatures.hasNamespaces()) continue;

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String supDstName = supDstData[0];
				String subDstName = dstNames[subNs];
				boolean uncompletedDst = subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName));

				if (!uncompletedDst) {
					assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming field destination name differs from supTree");
				}
			}

			if (supHasDstDescs && subHasDstDescs && dstDescs != null) {
				String supDstDesc = supDstData[1];
				String subDstDesc = dstDescs[subNs];
				boolean uncompletedDst = subDstDesc == null && (supDstDesc == null || Objects.equals(supDstDesc, srcDesc));

				if (!uncompletedDst) {
					assertEquals(supDstDesc != null ? supDstDesc : srcDesc, subDstDesc, "Incoming field destination descriptor differs from supTree");
				}
			}
		}

		return true;
	}

	@Override
	public void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs, String comment) throws IOException {
		if (!supFeatures.supportsFields() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		assertEquals(supTree.getClass(srcClsName).getField(srcName, srcDesc).getComment(), comment);
	}

	@Override
	public boolean visitMethod(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs) throws IOException {
		if (!supFeatures.supportsMethods()) return true;

		MethodMappingView supMth = supTree.getClass(srcClsName).getMethod(srcName, srcDesc);
		boolean supHasSrcDescs = supFeatures.methods().srcDescs() != OptionalFeature.UNSUPPORTED;
		boolean subHasSrcDescs = subFeatures.methods().srcDescs() != OptionalFeature.UNSUPPORTED;
		boolean supHasDstNames = supFeatures.methods().dstNames() != OptionalFeature.UNSUPPORTED;
		boolean subHasDstNames = subFeatures.methods().dstNames() != OptionalFeature.UNSUPPORTED;
		boolean supHasDstDescs = supFeatures.methods().dstDescs() != OptionalFeature.UNSUPPORTED;
		boolean subHasDstDescs = subFeatures.methods().dstDescs() != OptionalFeature.UNSUPPORTED;

		if (supMth == null) { // SupTree doesn't have this method, ensure the incoming mappings don't have any data for it
			String[] subDstNames = null;
			String[] subDstDescs = null;

			if (supHasDstNames && subHasDstNames) subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};
			if (supHasDstDescs && subHasDstDescs) subDstDescs = supFeatures.hasNamespaces() || dstDescs == null ? dstDescs : new String[]{dstDescs[subNsIfSupNotNamespaced]};

			assertTrue(isEmpty(subDstNames) && isEmpty(subDstDescs), "Incoming method not contained in SupTree: " + srcName + ", descriptor: " + srcDesc);
			return true;
		}

		Map<String, String[]> supDstDataByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supMth.getDstName(supNs), supMth.getDstDesc(supNs)});
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasSrcDescs && subHasSrcDescs && srcDesc != null) {
				assertEquals(supMth.getSrcDesc(), srcDesc, "Incoming method source descriptor differs from supTree");
			}

			String[] supDstData = supDstDataByNsName.get(subDstNamespaces.get(subNs));
			if (supDstData == null && !supFeatures.hasNamespaces()) continue;

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String supDstName = supDstData[0];
				String subDstName = dstNames[subNs];
				boolean uncompletedDst = subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName));

				if (!uncompletedDst) {
					assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming method destination name differs from supTree");
				}
			}

			if (supHasDstDescs && subHasDstDescs && dstDescs != null) {
				String supDstDesc = supDstData[1];
				String subDstDesc = dstDescs[subNs];
				boolean uncompletedDst = subDstDesc == null && (supDstDesc == null || Objects.equals(supDstDesc, srcDesc));

				if (!uncompletedDst) {
					assertEquals(supDstDesc != null ? supDstDesc : srcDesc, subDstDesc, "Incoming method destination descriptor differs from supTree");
				}
			}
		}

		return true;
	}

	@Override
	public void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs, String comment) throws IOException {
		if (!supFeatures.supportsMethods() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		assertEquals(supTree.getClass(srcClsName).getMethod(srcName, srcDesc).getComment(), comment);
	}

	@Override
	public boolean visitMethodArg(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames) throws IOException {
		if (!supFeatures.supportsArgs()) return true;

		MethodArgMappingView supArg = supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getArg(argPosition, lvIndex, srcName);
		boolean supHasPositions = supFeatures.args().positions() != OptionalFeature.UNSUPPORTED;
		boolean subHasPositions = subFeatures.args().positions() != OptionalFeature.UNSUPPORTED;
		boolean supHasLvIndices = supFeatures.args().lvIndices() != OptionalFeature.UNSUPPORTED;
		boolean subHasLvIndices = subFeatures.args().lvIndices() != OptionalFeature.UNSUPPORTED;
		boolean supHasDstNames = supFeatures.args().dstNames() != OptionalFeature.UNSUPPORTED;
		boolean subHasDstNames = subFeatures.args().dstNames() != OptionalFeature.UNSUPPORTED;

		if (supArg == null) { // SupTree doesn't have this arg, ensure the incoming mappings don't have any data for it
			if (supHasDstNames && subHasDstNames) {
				String[] subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};
				assertTrue(isEmpty(subDstNames), "Incoming arg not contained in SupTree: " + "position: " + argPosition + ", lvIndex: " + lvIndex + ", name: " + srcName);
			}

			return true;
		}

		Map<String, String> supDstNamesByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supArg.getDstName(supNs));
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasPositions && subHasPositions) {
				assertEquals(supArg.getArgPosition(), argPosition, "Incoming arg position differs from supTree");
			}

			if (supHasLvIndices && subHasLvIndices) {
				assertEquals(supArg.getLvIndex(), lvIndex, "Incoming arg local variable index differs from supTree");
			}

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String supDstName = supDstNamesByNsName.get(subDstNamespaces.get(subNs));
				if (supDstName == null && !supFeatures.hasNamespaces()) continue;

				String subDstName = dstNames[subNs];
				if (subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName))) continue; // uncompleted dst name

				assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming arg destination name differs from supTree");
			}
		}

		return true;
	}

	@Override
	public void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int argPosition, int lvIndex, @Nullable String srcArgName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames, String comment) throws IOException {
		if (!supFeatures.supportsArgs() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		assertEquals(supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getArg(argPosition, lvIndex, srcArgName).getComment(), comment);
	}

	@Override
	public boolean visitMethodVar(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames) throws IOException {
		if (!supFeatures.supportsVars()) return true;

		MethodVarMappingView supVar = supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
		boolean supHasLvIndices = supFeatures.vars().lvIndices() != OptionalFeature.UNSUPPORTED;
		boolean subHasLvIndices = subFeatures.vars().lvIndices() != OptionalFeature.UNSUPPORTED;
		boolean supHasLvtIndices = supFeatures.vars().lvtRowIndices() != OptionalFeature.UNSUPPORTED;
		boolean subHasLvtIndices = subFeatures.vars().lvtRowIndices() != OptionalFeature.UNSUPPORTED;
		boolean supHasStartOpIndices = supFeatures.vars().startOpIndices() != OptionalFeature.UNSUPPORTED;
		boolean subHasStartOpIndices = subFeatures.vars().startOpIndices() != OptionalFeature.UNSUPPORTED;
		boolean supHasEndOpIndices = supFeatures.vars().endOpIndices() != OptionalFeature.UNSUPPORTED;
		boolean subHasEndOpIndices = subFeatures.vars().endOpIndices() != OptionalFeature.UNSUPPORTED;
		boolean supHasDstNames = supFeatures.vars().dstNames() != OptionalFeature.UNSUPPORTED;
		boolean subHasDstNames = subFeatures.vars().dstNames() != OptionalFeature.UNSUPPORTED;

		if (supVar == null) { // SupTree doesn't have this var, ensure the incoming mappings don't have any data for it
			if (supHasDstNames && subHasDstNames) {
				String[] subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};
				assertTrue(isEmpty(subDstNames), "Incoming var not contained in SupTree: " + "lvtRowIndex: " + lvtRowIndex + ", lvIndex: " + lvIndex + ", startOpIdx: " + startOpIdx + ", endOpIdx: " + endOpIdx + ", name: " + srcName);
			}

			return true;
		}

		Map<String, String> supDstNamesByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supVar.getDstName(supNs));
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasLvIndices && subHasLvIndices) {
				assertEquals(supVar.getLvIndex(), lvIndex, "Incoming var lvIndex differs from supTree");
			}

			if (supHasLvtIndices && subHasLvtIndices) {
				assertEquals(supVar.getLvtRowIndex(), lvtRowIndex, "Incoming var lvtIndex differs from supTree");
			}

			if (supHasStartOpIndices && subHasStartOpIndices) {
				assertEquals(supVar.getStartOpIdx(), startOpIdx, "Incoming var startOpIndex differs from supTree");
			}

			if (supHasEndOpIndices && subHasEndOpIndices) {
				assertEquals(supVar.getEndOpIdx(), endOpIdx, "Incoming var endOpIndex differs from supTree");
			}

			String supDstName = supDstNamesByNsName.get(subDstNamespaces.get(subNs));
			if (supDstName == null && !supFeatures.hasNamespaces()) continue;

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String subDstName = dstNames[subNs];
				if (subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName))) continue; // uncompleted dst name

				assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming var destination name differs from supTree");
			}
		}

		return true;
	}

	@Override
	public void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcVarName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames, String comment) throws IOException {
		if (!supFeatures.supportsVars() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		assertEquals(supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName).getComment(), comment);
	}

	private boolean isEmpty(String[] arr) {
		if (arr == null) return true;

		for (String s : arr) {
			if (s != null) return false;
		}

		return true;
	}

	private final MappingTreeView supTree;
	private final int supDstNsCount;
	private final FeatureSet supFeatures;
	private final FeatureSet subFeatures;
	private int subNsIfSupNotNamespaced;
	private List<String> subDstNamespaces;
}


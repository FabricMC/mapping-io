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

package net.fabricmc.mappingio.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.FlatMappingVisitor;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.format.FeatureSet;
import net.fabricmc.mappingio.format.FeatureSet.ElementCommentSupport;
import net.fabricmc.mappingio.format.FeatureSet.FeaturePresence;
import net.fabricmc.mappingio.format.FeatureSetBuilder;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodArgMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodVarMappingView;

/**
 * A visitor which asserts that the visited mappings are a subset of a superset tree.
 *
 * <p><b>Experimental feature</b>, may be removed or changed without further notice.
 */
@ApiStatus.Experimental
public class SubsetChecker implements FlatMappingVisitor {
	/**
	 * @param supTree The superset tree.
	 * @param supFormat The superset format, or null if supTree has all the original data.
	 * @param subFormat The subset format, or null if lossless (i.e. if the visits are coming from a tree).
	 * @param errorHandler The error handler, which will be called with the error message if an assertion fails.
	 * Currently expected to throw an exception, otherwise the checker will continue to run in an invalid state.
	 */
	public SubsetChecker(MappingTreeView supTree, @Nullable MappingFormat supFormat, @Nullable MappingFormat subFormat, Consumer<String> errorHandler) {
		this.supTree = supTree;
		this.subFormat = subFormat;
		this.supDstNsCount = supTree.getMaxNamespaceId();
		this.supFeatures = supFormat == null ? new FeatureSetBuilder(true).build() : supFormat.features();
		this.subFeatures = subFormat == null ? new FeatureSetBuilder(true).build() : subFormat.features();
		this.errorHandler = errorHandler;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		String expectedSrcNs = subFeatures.hasNamespaces() ? supTree.getSrcNamespace() : MappingUtil.NS_SOURCE_FALLBACK;
		assertEquals(expectedSrcNs, srcNamespace, "Incoming mappings have different source namespace than supTree");
		subDstNamespaces = new ArrayList<>(dstNamespaces);

		if (!subFeatures.hasNamespaces()) {
			assertEquals(1, dstNamespaces.size(), "Incoming mappings have multiple namespaces ("
					+ String.join(", ", dstNamespaces)
					+ ") despite their supposed originating format ("
					+ subFormat
					+ ") declaring not to support them");
			assertEquals(MappingUtil.NS_TARGET_FALLBACK, dstNamespaces.get(0), "Incoming mappings don't have default destination namespace name of non-namespaced formats");
			subDstNamespaces.set(0, supTree.getNamespaceName(0)); // TODO: Make this configurable
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

				if (i < dstNamespaces.size() - 1) {
					continue;
				} else if (supTree.getNamespaceName(0).equals(MappingUtil.NS_TARGET_FALLBACK)) {
					// None of the incoming namespaces equal supTree's, which uses the fallback namespace.
					// Let's assume it's equivalent to the first incoming sub namespace.
					// TODO: Make this configurable
					subNsIfSupNotNamespaced = 0;
					subDstNamespaces.set(0, supTree.getNamespaceName(0));
					break;
				}
			}

			assertTrue(contained, "Incoming namespace not contained in supTree: " + dstNs);
		}
	}

	@Override
	public boolean visitClass(String srcName, @Nullable String[] dstNames) throws IOException {
		if (!supFeatures.supportsClasses()) return true; // sub-elements might still be supported

		ClassMappingView supCls = supTree.getClass(srcName);
		boolean supHasDstNames = supFeatures.classes().dstNames() != FeaturePresence.ABSENT;
		boolean subHasDstNames = subFeatures.classes().dstNames() != FeaturePresence.ABSENT;
		boolean supHasRepackaging = supFeatures.classes().hasRepackaging();
		boolean subHasRepackaging = subFeatures.classes().hasRepackaging();

		if (supCls == null) { // supTree doesn't have this class, ensure the incoming mappings don't have any data for it
			if (supHasDstNames && subHasDstNames) {
				String[] subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};

				if (!isEmpty(subDstNames)) {
					boolean error = true;

					if (!supHasRepackaging) {
						for (String subDstName : subDstNames) {
							if (subDstName != null) {
								String srcPkg = getPackage(srcName);
								String dstPkg = getPackage(subDstName);

								if (srcPkg != null && srcPkg.equals(dstPkg)) {
									error = true;
									break;
								} else {
									// The incoming class has been repackaged, which supFormat doesn't support, that's why it's missing from supTree
									error = false;
								}
							}
						}
					}

					assertFalse(error, "Incoming class not contained in supTree: " + srcName);
				}
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

				if (supDstName == null) {
					supDstName = srcName;
				}

				boolean error = !Objects.equals(supDstName, subDstName);

				if (error && subDstName == null && !subHasRepackaging && supHasRepackaging) {
					String srcPkg = getPackage(srcName);
					String dstPkg = getPackage(supDstName);

					if (srcPkg != null && srcPkg.equals(dstPkg)) {
						// The incoming class has been repackaged in supTree, which subFormat doesn't support
						error = false;
					}
				}

				if (error) {
					assertEquals(supDstName, subDstName, "Incoming class destination name differs from supTree");
				}
			}
		}

		return true;
	}

	@Override
	public void visitClassComment(String srcName, @Nullable String[] dstNames, String comment) throws IOException {
		if (!supFeatures.supportsClasses() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		ClassMappingView supCls = requireNonNull(supTree.getClass(srcName), "Incoming class comment's parent class not contained in supTree: " + srcName);

		assertEquals(supCls.getComment(), comment, "Incoming class comment not contained in supTree: " + srcName);
	}

	@Override
	public boolean visitField(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs) throws IOException {
		if (!supFeatures.supportsFields()) return true;

		String subFldId = srcClsName + "#" + srcName + ":" + srcDesc;
		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming field's parent class not contained in supTree: " + subFldId);
		FieldMappingView supFld = supCls.getField(srcName, srcDesc);

		boolean supHasSrcDescs = supFeatures.fields().srcDescs() != FeaturePresence.ABSENT;
		boolean subHasSrcDescs = subFeatures.fields().srcDescs() != FeaturePresence.ABSENT;
		boolean supHasDstNames = supFeatures.fields().dstNames() != FeaturePresence.ABSENT;
		boolean subHasDstNames = subFeatures.fields().dstNames() != FeaturePresence.ABSENT;
		boolean supHasDstDescs = supFeatures.fields().dstDescs() != FeaturePresence.ABSENT;
		boolean subHasDstDescs = subFeatures.fields().dstDescs() != FeaturePresence.ABSENT;
		boolean supRequiresSrcDescs = supFeatures.fields().srcDescs() == FeaturePresence.REQUIRED;

		if (supFld == null) { // supTree doesn't have this field, ensure the incoming mappings don't have any data for it
			String[] subDstNames = null;
			String[] subDstDescs = null;

			if (supHasDstNames && subHasDstNames) subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};
			if (supHasDstDescs && subHasDstDescs) subDstDescs = supFeatures.hasNamespaces() || dstDescs == null ? dstDescs : new String[]{dstDescs[subNsIfSupNotNamespaced]};

			boolean noData = isEmpty(subDstNames) && isEmpty(subDstDescs);
			boolean missingRequiredSrcDesc = supRequiresSrcDescs && srcDesc == null;

			assertTrue(noData || missingRequiredSrcDesc, "Incoming field not contained in supTree: " + subFldId);
			return !missingRequiredSrcDesc;
		}

		String supFldId = srcClsName + "#" + srcName + ":" + supFld.getSrcDesc();
		Map<String, String[]> supDstDataByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supFld.getDstName(supNs), supFld.getDstDesc(supNs)});
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasSrcDescs && subHasSrcDescs && srcDesc != null) {
				assertEquals(supFldId, subFldId, "Incoming field source descriptor differs from supTree");
			}

			String[] supDstData = supDstDataByNsName.get(subDstNamespaces.get(subNs));
			if (supDstData == null && !supFeatures.hasNamespaces()) continue;

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String supDstName = supDstData[0];
				String subDstName = dstNames[subNs];
				boolean uncompletedDst = subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName));

				if (!uncompletedDst) {
					assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming field (" + subFldId + ") destination name differs from supTree");
				}
			}

			if (supHasDstDescs && subHasDstDescs && dstDescs != null) {
				String supDstDesc = supDstData[1];
				String subDstDesc = dstDescs[subNs];
				boolean uncompletedDst = subDstDesc == null && (supDstDesc == null || Objects.equals(supDstDesc, srcDesc));

				if (!uncompletedDst) {
					String subFldDestId = srcClsName + "#" + srcName + ":" + subDstDesc;
					String supFldDestId = srcClsName + "#" + srcName + ":" + (supDstDesc != null ? supDstDesc : srcDesc);
					assertEquals(supFldDestId, subFldDestId, "Incoming field destination descriptor differs from supTree");
				}
			}
		}

		return true;
	}

	@Override
	public void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs, String comment) throws IOException {
		if (!supFeatures.supportsFields() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		String subFldId = srcClsName + "#" + srcName + ":" + srcDesc;
		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming field comment's parent class not contained in supTree: " + subFldId);
		FieldMappingView supFld = requireNonNull(supCls.getField(srcName, srcDesc), "Incoming field comment's parent field not contained in supTree: " + subFldId);

		assertEquals(supFld.getComment(), comment, "Incoming comment differs from supTree");
	}

	@Override
	public boolean visitMethod(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs) throws IOException {
		if (!supFeatures.supportsMethods()) return true;

		String subMthId = srcClsName + "#" + srcName + srcDesc;
		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming method's parent class not contained in supTree: " + subMthId);
		MethodMappingView supMth = supCls.getMethod(srcName, srcDesc);

		boolean supHasSrcDescs = supFeatures.methods().srcDescs() != FeaturePresence.ABSENT;
		boolean subHasSrcDescs = subFeatures.methods().srcDescs() != FeaturePresence.ABSENT;
		boolean supHasDstNames = supFeatures.methods().dstNames() != FeaturePresence.ABSENT;
		boolean subHasDstNames = subFeatures.methods().dstNames() != FeaturePresence.ABSENT;
		boolean supHasDstDescs = supFeatures.methods().dstDescs() != FeaturePresence.ABSENT;
		boolean subHasDstDescs = subFeatures.methods().dstDescs() != FeaturePresence.ABSENT;
		boolean supRequiresSrcDescs = supFeatures.methods().srcDescs() == FeaturePresence.REQUIRED;

		if (supMth == null) { // supTree doesn't have this method, ensure the incoming mappings don't have any data for it
			String[] subDstNames = null;
			String[] subDstDescs = null;

			if (supHasDstNames && subHasDstNames) subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};
			if (supHasDstDescs && subHasDstDescs) subDstDescs = supFeatures.hasNamespaces() || dstDescs == null ? dstDescs : new String[]{dstDescs[subNsIfSupNotNamespaced]};

			boolean noData = isEmpty(subDstNames) && isEmpty(subDstDescs);
			boolean missingRequiredSrcDesc = supRequiresSrcDescs && srcDesc == null;

			assertTrue(noData || missingRequiredSrcDesc, "Incoming method not contained in supTree: " + subMthId);
			return !missingRequiredSrcDesc;
		}

		String supMthId = srcClsName + "#" + srcName + supMth.getSrcDesc();
		Map<String, String[]> supDstDataByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supMth.getDstName(supNs), supMth.getDstDesc(supNs)});
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasSrcDescs && subHasSrcDescs && srcDesc != null) {
				assertEquals(supMthId, subMthId, "Incoming method source descriptor differs from supTree");
			}

			String[] supDstData = supDstDataByNsName.get(subDstNamespaces.get(subNs));
			if (supDstData == null && !supFeatures.hasNamespaces()) continue;

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String supDstName = supDstData[0];
				String subDstName = dstNames[subNs];
				boolean uncompletedDst = subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName));

				if (!uncompletedDst) {
					assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming method (" + subMthId + ") destination name differs from supTree");
				}
			}

			if (supHasDstDescs && subHasDstDescs && dstDescs != null) {
				String supDstDesc = supDstData[1];
				String subDstDesc = dstDescs[subNs];
				boolean uncompletedDst = subDstDesc == null && (supDstDesc == null || Objects.equals(supDstDesc, srcDesc));

				if (!uncompletedDst) {
					String subMthDestId = srcClsName + "#" + srcName + subDstDesc;
					String supMthDestId = srcClsName + "#" + srcName + (supDstDesc != null ? supDstDesc : srcDesc);

					assertEquals(supMthDestId, subMthDestId, "Incoming method destination descriptor differs from supTree");
				}
			}
		}

		return true;
	}

	@Override
	public void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs, String comment) throws IOException {
		if (!supFeatures.supportsMethods() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		String subMthId = srcClsName + "#" + srcName + srcDesc;
		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming method comment's parent class not contained in supTree: " + subMthId);
		MethodMappingView supMth = requireNonNull(supCls.getMethod(srcName, srcDesc), "Incoming method comment's parent method not contained in supTree: " + subMthId);

		assertEquals(supMth.getComment(), comment, "Incoming comment differs from supTree");
	}

	@Override
	public boolean visitMethodArg(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames) throws IOException {
		if (!supFeatures.supportsArgs()) return true;

		String subArgId = srcClsName + "#" + srcMethodName + srcMethodDesc + ":" + argPosition + ":" + lvIndex + ":" + srcName;
		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming arg's parent class not contained in supTree: " + subArgId);
		MethodMappingView supMth = requireNonNull(supCls.getMethod(srcMethodName, srcMethodDesc), "Incoming arg's parent method not contained in supTree: " + subArgId);
		MethodArgMappingView supArg = supMth.getArg(argPosition, lvIndex, srcName);

		boolean supHasPositions = supFeatures.args().positions() != FeaturePresence.ABSENT;
		boolean subHasPositions = subFeatures.args().positions() != FeaturePresence.ABSENT;
		boolean supHasLvIndices = supFeatures.args().lvIndices() != FeaturePresence.ABSENT;
		boolean subHasLvIndices = subFeatures.args().lvIndices() != FeaturePresence.ABSENT;
		boolean supHasSrcNames = supFeatures.args().srcNames() != FeaturePresence.ABSENT;
		boolean subHasSrcNames = subFeatures.args().srcNames() != FeaturePresence.ABSENT;
		boolean supHasDstNames = supFeatures.args().dstNames() != FeaturePresence.ABSENT;
		boolean subHasDstNames = subFeatures.args().dstNames() != FeaturePresence.ABSENT;

		if (supArg == null) { // supTree doesn't have this arg, ensure the incoming mappings don't have any data for it
			if (supHasDstNames && subHasDstNames) {
				String[] subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};

				assertTrue(isEmpty(subDstNames), "Incoming arg not contained in supTree: " + subArgId);
			}

			return true;
		}

		Map<String, String> supDstNamesByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supArg.getDstName(supNs));
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasPositions && subHasPositions) {
				assertEquals(supArg.getArgPosition(), argPosition, "Incoming arg (" + subArgId + ") argPosition differs from supTree");
			}

			if (supHasLvIndices && subHasLvIndices) {
				assertEquals(supArg.getLvIndex(), lvIndex, "Incoming arg (" + subArgId + ") lvIndex differs from supTree");
			}

			if (supHasSrcNames && subHasSrcNames) {
				assertEquals(supArg.getSrcName(), srcName, "Incoming arg (" + subArgId + ") srcName differs from supTree");
			}

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String supDstName = supDstNamesByNsName.get(subDstNamespaces.get(subNs));
				String subDstName = dstNames[subNs];
				boolean uncompletedDst = subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName));

				if (!uncompletedDst) {
					assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming arg (" + subArgId + ") destination name differs from supTree");
				}
			}
		}

		return true;
	}

	@Override
	public void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int argPosition, int lvIndex, @Nullable String srcArgName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames, String comment) throws IOException {
		if (!supFeatures.supportsArgs() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		String subArgId = srcClsName + "#" + srcMethodName + srcMethodDesc + ":" + argPosition + ":" + lvIndex + ":" + srcArgName;

		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming arg comment's parent class not contained in supTree: " + subArgId);
		MethodMappingView supMth = requireNonNull(supCls.getMethod(srcMethodName, srcMethodDesc), "Incoming arg comment's parent method not contained in supTree: " + subArgId);
		MethodArgMappingView supArg = requireNonNull(supMth.getArg(argPosition, lvIndex, srcArgName), "Incoming arg comment's parent arg not contained in supTree: " + subArgId);

		assertEquals(supArg.getComment(), comment, "Incoming comment differs from supTree");
	}

	@Override
	public boolean visitMethodVar(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames) throws IOException {
		if (!supFeatures.supportsVars()) return true;

		String subVarId = srcClsName + "#" + srcMethodName + srcMethodDesc + ":" + lvtRowIndex + ":" + lvIndex + ":" + startOpIdx + ":" + endOpIdx + ":" + srcName;
		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming var's parent class not contained in supTree: " + subVarId);
		MethodMappingView supMth = requireNonNull(supCls.getMethod(srcMethodName, srcMethodDesc), "Incoming var's parent method not contained in supTree: " + subVarId);
		MethodVarMappingView supVar = supMth.getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);

		boolean supHasLvIndices = supFeatures.vars().lvIndices() != FeaturePresence.ABSENT;
		boolean subHasLvIndices = subFeatures.vars().lvIndices() != FeaturePresence.ABSENT;
		boolean supHasLvtIndices = supFeatures.vars().lvtRowIndices() != FeaturePresence.ABSENT;
		boolean subHasLvtIndices = subFeatures.vars().lvtRowIndices() != FeaturePresence.ABSENT;
		boolean supHasStartOpIndices = supFeatures.vars().startOpIndices() != FeaturePresence.ABSENT;
		boolean subHasStartOpIndices = subFeatures.vars().startOpIndices() != FeaturePresence.ABSENT;
		boolean supHasEndOpIndices = supFeatures.vars().endOpIndices() != FeaturePresence.ABSENT;
		boolean subHasEndOpIndices = subFeatures.vars().endOpIndices() != FeaturePresence.ABSENT;
		boolean supHasSrcNames = supFeatures.vars().srcNames() != FeaturePresence.ABSENT;
		boolean subHasSrcNames = subFeatures.vars().srcNames() != FeaturePresence.ABSENT;
		boolean supHasDstNames = supFeatures.vars().dstNames() != FeaturePresence.ABSENT;
		boolean subHasDstNames = subFeatures.vars().dstNames() != FeaturePresence.ABSENT;

		if (supVar == null) { // supTree doesn't have this var, ensure the incoming mappings don't have any data for it
			if (supHasDstNames && subHasDstNames) {
				String[] subDstNames = supFeatures.hasNamespaces() || dstNames == null ? dstNames : new String[]{dstNames[subNsIfSupNotNamespaced]};

				assertTrue(isEmpty(subDstNames), "Incoming var not contained in supTree: " + subVarId);
			}

			return true;
		}

		Map<String, String> supDstNamesByNsName = new HashMap<>();

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supVar.getDstName(supNs));
		}

		for (int subNs = 0; subNs < subDstNamespaces.size(); subNs++) {
			if (supHasLvIndices && subHasLvIndices) {
				assertEquals(supVar.getLvIndex(), lvIndex, "Incoming var (" + subVarId + ") lvIndex differs from supTree");
			}

			if (supHasLvtIndices && subHasLvtIndices) {
				assertEquals(supVar.getLvtRowIndex(), lvtRowIndex, "Incoming var (" + subVarId + ") lvtRowIndex differs from supTree");
			}

			if (supHasStartOpIndices && subHasStartOpIndices) {
				assertEquals(supVar.getStartOpIdx(), startOpIdx, "Incoming var (" + subVarId + ") startOpIndex differs from supTree");
			}

			if (supHasEndOpIndices && subHasEndOpIndices) {
				assertEquals(supVar.getEndOpIdx(), endOpIdx, "Incoming var (" + subVarId + ") endOpIndex differs from supTree");
			}

			if (supHasSrcNames && subHasSrcNames) {
				assertEquals(supVar.getSrcName(), srcName, "Incoming var (" + subVarId + ") srcName differs from supTree");
			}

			if (supHasDstNames && subHasDstNames && dstNames != null) {
				String supDstName = supDstNamesByNsName.get(subDstNamespaces.get(subNs));
				String subDstName = dstNames[subNs];
				boolean uncompletedDst = subDstName == null && (supDstName == null || Objects.equals(supDstName, srcName));

				if (!uncompletedDst) {
					assertEquals(supDstName != null ? supDstName : srcName, subDstName, "Incoming var (" + subVarId + ") destination name differs from supTree");
				}
			}
		}

		return true;
	}

	@Override
	public void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc, int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcVarName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames, @Nullable String[] dstMethodDescs, @Nullable String[] dstNames, String comment) throws IOException {
		if (!supFeatures.supportsVars() || supFeatures.elementComments() == ElementCommentSupport.NONE) return;

		String subVarId = srcClsName + "#" + srcMethodName + srcMethodDesc + ":" + lvtRowIndex + ":" + lvIndex + ":" + startOpIdx + ":" + endOpIdx + ":" + srcVarName;

		ClassMappingView supCls = requireNonNull(supTree.getClass(srcClsName), "Incoming var comment's parent class not contained in supTree: " + subVarId);
		MethodMappingView supMth = requireNonNull(supCls.getMethod(srcMethodName, srcMethodDesc), "Incoming var comment's parent method not contained in supTree: " + subVarId);
		MethodVarMappingView supVar = requireNonNull(supMth.getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName), "Incoming var comment's parent var not contained in supTree: " + subVarId);

		assertEquals(supVar.getComment(), comment, "Incoming comment differs from supTree");
	}

	protected void assertTrue(boolean condition, String message) {
		if (!condition) {
			errorHandler.accept(message);
		}
	}

	protected void assertFalse(boolean condition, String message) {
		if (condition) {
			errorHandler.accept(message);
		}
	}

	protected void assertEquals(Object expected, Object actual, String message) {
		if (!Objects.equals(expected, actual)) {
			errorHandler.accept(message + ": Expected: " + expected + ", Actual: " + actual);
		}
	}

	protected void assertNotNull(Object obj, String message) {
		if (obj == null) {
			errorHandler.accept(message);
		}
	}

	private <T> T requireNonNull(T obj, String message) {
		assertNotNull(obj, message);
		return obj;
	}

	private boolean isEmpty(String[] arr) {
		if (arr == null) return true;

		for (String s : arr) {
			if (s != null) return false;
		}

		return true;
	}

	@Nullable
	private String getPackage(String name) {
		int lastSlash = name.lastIndexOf('/');
		return lastSlash == -1 ? null : name.substring(0, lastSlash);
	}

	private final MappingTreeView supTree;
	private final int supDstNsCount;
	private final MappingFormat subFormat;
	private final FeatureSet supFeatures;
	private final FeatureSet subFeatures;
	private final Consumer<String> errorHandler;
	private int subNsIfSupNotNamespaced;
	private List<String> subDstNamespaces;
}

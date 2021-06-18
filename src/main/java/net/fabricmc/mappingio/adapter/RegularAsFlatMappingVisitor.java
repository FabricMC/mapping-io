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

package net.fabricmc.mappingio.adapter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import net.fabricmc.mappingio.FlatMappingVisitor;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;

public final class RegularAsFlatMappingVisitor implements FlatMappingVisitor {
	public RegularAsFlatMappingVisitor(MappingVisitor next) {
		this.next = next;
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return next.getFlags();
	}

	@Override
	public void reset() {
		lastClass = null;
		lastMemberName = lastMemberDesc = null;
		lastArgPosition = lastLvIndex = lastStartOpIdx = -1;

		next.reset();
	}

	@Override
	public boolean visitHeader() throws IOException {
		return next.visitHeader();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		next.visitNamespaces(srcNamespace, dstNamespaces);

		Set<MappingFlag> flags = next.getFlags();
		relayDstFieldDescs = flags.contains(MappingFlag.NEEDS_DST_FIELD_DESC);
		relayDstMethodDescs = flags.contains(MappingFlag.NEEDS_DST_METHOD_DESC);
	}

	@Override
	public void visitMetadata(String key, String value) throws IOException {
		next.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() throws IOException {
		return next.visitContent();
	}

	@Override
	public boolean visitClass(String srcName, String[] dstNames) throws IOException {
		return visitClass(srcName, dstNames, null);
	}

	@Override
	public boolean visitClass(String srcName, String dstName) throws IOException {
		return visitClass(srcName, null, dstName);
	}

	private boolean visitClass(String srcName, String[] dstNames, String dstName) throws IOException {
		if (!srcName.equals(lastClass)) {
			lastClass = srcName;
			lastMemberName = lastMemberDesc = null;
			lastArgPosition = lastLvIndex = lastStartOpIdx = -1;
			relayLastClass = next.visitClass(srcName) && visitDstNames(MappedElementKind.CLASS, dstNames, dstName);
		}

		return relayLastClass;
	}

	@Override
	public void visitClassComment(String srcName, String[] dstNames, String comment) throws IOException {
		if (!visitClass(srcName, dstNames, null)) return;
		next.visitComment(MappedElementKind.CLASS, comment);
	}

	@Override
	public void visitClassComment(String srcName, String dstName, String comment) throws IOException {
		if (!visitClass(srcName, null, dstName)) return;
		next.visitComment(MappedElementKind.CLASS, comment);
	}

	@Override
	public boolean visitField(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
		return visitField(srcClsName, srcName, srcDesc, dstClsNames, dstNames, dstDescs, null, null, null);
	}

	@Override
	public boolean visitField(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc) throws IOException {
		return visitField(srcClsName, srcName, srcDesc, null, null, null, dstClsName, dstName, dstDesc);
	}

	private boolean visitField(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs, String dstClsName, String dstName, String dstDesc) throws IOException {
		if (!visitClass(srcClsName, dstClsNames, dstClsName)) return false;

		if (!lastMemberIsField || !srcName.equals(lastMemberName) || srcDesc != null && !srcDesc.equals(lastMemberDesc)) {
			lastMemberName = srcName;
			lastMemberDesc = srcDesc;
			lastMemberIsField = true;
			lastArgPosition = lastLvIndex = lastStartOpIdx = -1;
			relayLastMember = next.visitField(srcName, srcDesc) && visitDstNamesDescs(MappedElementKind.FIELD, dstNames, dstDescs, dstName, dstDesc);
		}

		return relayLastMember;
	}

	@Override
	public void visitFieldComment(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs,
			String comment) throws IOException {
		if (!visitField(srcClsName, srcName, srcDesc, dstClsNames, dstNames, dstDescs, null, null, null)) return;
		next.visitComment(MappedElementKind.FIELD, comment);
	}

	@Override
	public void visitFieldComment(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc,
			String comment) throws IOException {
		if (!visitField(srcClsName, srcName, srcDesc, null, null, null, dstClsName, dstName, dstDesc)) return;
		next.visitComment(MappedElementKind.FIELD, comment);
	}

	@Override
	public boolean visitMethod(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
		return visitMethod(srcClsName, srcName, srcDesc, dstClsNames, dstNames, dstDescs, null, null, null);
	}

	@Override
	public boolean visitMethod(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc) throws IOException {
		return visitMethod(srcClsName, srcName, srcDesc, null, null, null, dstClsName, dstName, dstDesc);
	}

	private boolean visitMethod(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs, String dstClsName, String dstName, String dstDesc) throws IOException {
		if (!visitClass(srcClsName, dstClsNames, dstClsName)) return false;

		if (lastMemberIsField || !srcName.equals(lastMemberName) || srcDesc != null && !srcDesc.equals(lastMemberDesc)) {
			lastMemberName = srcName;
			lastMemberDesc = srcDesc;
			lastMemberIsField = false;
			lastArgPosition = lastLvIndex = lastStartOpIdx = -1;
			relayLastMember = next.visitMethod(srcName, srcDesc) && visitDstNamesDescs(MappedElementKind.METHOD, dstNames, dstDescs, dstName, dstDesc);
		}

		return relayLastMember;
	}

	@Override
	public void visitMethodComment(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs,
			String comment) throws IOException {
		if (!visitMethod(srcClsName, srcName, srcDesc, dstClsNames, dstNames, dstDescs, null, null, null)) return;
		next.visitComment(MappedElementKind.METHOD, comment);
	}

	@Override
	public void visitMethodComment(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc,
			String comment) throws IOException {
		if (!visitMethod(srcClsName, srcName, srcDesc, null, null, null, dstClsName, dstName, dstDesc)) return;
		next.visitComment(MappedElementKind.METHOD, comment);
	}

	@Override
	public boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstArgNames) throws IOException {
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcArgName,
				dstClsNames, dstMethodNames, dstMethodDescs, dstArgNames, null, null, null, null);
	}

	@Override
	public boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstArgName) throws IOException {
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcArgName,
				null, null, null, null, dstClsName, dstMethodName, dstMethodDesc, dstArgName);
	}

	private boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstName) throws IOException {
		if (!visitMethod(srcClsName, srcMethodName, srcMethodDesc, dstClsNames, dstMethodNames, dstMethodDescs, dstClsName, dstMethodName, dstMethodDesc)) return false;

		if (!lastMethodSubIsArg || argPosition != lastArgPosition || lvIndex != lastLvIndex) {
			lastArgPosition = argPosition;
			lastLvIndex = lvIndex;
			lastMethodSubIsArg = true;
			relayLastMethodSub = next.visitMethodArg(argPosition, lvIndex, srcName) && visitDstNames(MappedElementKind.METHOD_ARG, dstNames, dstName);
		}

		return relayLastMethodSub;
	}

	@Override
	public void visitMethodArgComment(String srcClsName, String srcMethodName, String srcMethodDesc, int argPosition, int lvIndex, String srcArgName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstArgNames,
			String comment) throws IOException {
		if (!visitMethodArg(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcArgName,
				dstClsNames, dstMethodNames, dstMethodDescs, dstArgNames, null, null, null, null)) {
			return;
		}

		next.visitComment(MappedElementKind.METHOD_ARG, comment);
	}

	@Override
	public void visitMethodArgComment(String srcClsName, String srcMethodName, String srcMethodDesc, int argPosition,
			int lvIndex, String srcArgName, String dstClsName, String dstMethodName, String dstMethodDesc,
			String dstArgName, String comment) throws IOException {
		if (!visitMethodArg(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcArgName,
				null, null, null, null, dstClsName, dstMethodName, dstMethodDesc, dstArgName)) {
			return;
		}

		next.visitComment(MappedElementKind.METHOD_ARG, comment);
	}

	@Override
	public boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc, int lvtRowIndex, int lvIndex, int startOpIdx, String srcVarName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstVarNames) throws IOException {
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc, lvtRowIndex, lvIndex, startOpIdx, srcVarName,
				dstClsNames, dstMethodNames, dstMethodDescs, dstVarNames, null, null, null, null);
	}

	@Override
	public boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc, int lvtRowIndex, int lvIndex, int startOpIdx, String srcVarName,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstVarName) throws IOException {
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc, lvtRowIndex, lvIndex, startOpIdx, srcVarName,
				null, null, null, null, dstClsName, dstMethodName, dstMethodDesc, dstVarName);
	}

	private boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, String srcName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstName) throws IOException {
		if (!visitMethod(srcClsName, srcMethodName, srcMethodDesc, dstClsNames, dstMethodNames, dstMethodDescs, dstClsName, dstMethodName, dstMethodDesc)) return false;

		if (lastMethodSubIsArg || lvtRowIndex != lastArgPosition || lvIndex != lastLvIndex || startOpIdx != lastStartOpIdx) {
			lastArgPosition = lvtRowIndex;
			lastLvIndex = lvIndex;
			lastStartOpIdx = startOpIdx;
			lastMethodSubIsArg = false;
			relayLastMethodSub = next.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, srcName) && visitDstNames(MappedElementKind.METHOD_VAR, dstNames, dstName);
		}

		return relayLastMethodSub;
	}

	@Override
	public void visitMethodVarComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, String srcVarName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstVarNames,
			String comment) throws IOException {
		if (!visitMethodVar(srcClsName, srcMethodName, srcMethodDesc, lvtRowIndex, lvIndex, startOpIdx, srcVarName,
				dstClsNames, dstMethodNames, dstMethodDescs, dstVarNames, null, null, null, null)) {
			return;
		}

		next.visitComment(MappedElementKind.METHOD_VAR, comment);
	}

	@Override
	public void visitMethodVarComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, String srcVarName,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstVarName,
			String comment) throws IOException {
		if (!visitMethodVar(srcClsName, srcMethodName, srcMethodDesc, lvtRowIndex, lvIndex, startOpIdx, srcVarName,
				null, null, null, null, dstClsName, dstMethodName, dstMethodDesc, dstVarName)) {
			return;
		}

		next.visitComment(MappedElementKind.METHOD_VAR, comment);
	}

	@Override
	public boolean visitEnd() throws IOException {
		return next.visitEnd();
	}

	private boolean visitDstNames(MappedElementKind targetKind, String[] dstNames, String dstName) throws IOException {
		if (dstNames != null) {
			for (int i = 0; i < dstNames.length; i++) {
				String name = dstNames[i];
				if (name != null) next.visitDstName(targetKind, i, name);
			}
		} else if (dstName != null) {
			next.visitDstName(targetKind, 0, dstName);
		}

		return next.visitElementContent(targetKind);
	}

	private boolean visitDstNamesDescs(MappedElementKind targetKind, String[] dstNames, String[] dstDescs, String dstName, String dstDesc) throws IOException {
		boolean relayMemberDesc = targetKind == MappedElementKind.FIELD && relayDstFieldDescs
				|| targetKind != MappedElementKind.FIELD && relayDstMethodDescs;

		if (dstNames != null || dstDescs != null) {
			if (dstNames != null) {
				for (int i = 0; i < dstNames.length; i++) {
					String name = dstNames[i];
					if (name != null) next.visitDstName(targetKind, i, name);
				}
			}

			if (dstDescs != null && relayMemberDesc) {
				for (int i = 0; i < dstDescs.length; i++) {
					String desc = dstDescs[i];
					if (desc != null) next.visitDstDesc(targetKind, i, desc);
				}
			}
		} else {
			if (dstName != null) next.visitDstName(targetKind, 0, dstName);
			if (dstDesc != null && relayMemberDesc) next.visitDstDesc(targetKind, 0, dstDesc);
		}

		return next.visitElementContent(targetKind);
	}

	private final MappingVisitor next;

	private boolean relayDstFieldDescs;
	private boolean relayDstMethodDescs;

	private String lastClass;
	private boolean relayLastClass;
	private String lastMemberName, lastMemberDesc;
	private boolean lastMemberIsField;
	private boolean relayLastMember;
	private int lastArgPosition, lastLvIndex, lastStartOpIdx;
	private boolean lastMethodSubIsArg;
	private boolean relayLastMethodSub;
}

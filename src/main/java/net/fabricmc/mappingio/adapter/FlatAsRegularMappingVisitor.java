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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.FlatMappingVisitor;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;

public final class FlatAsRegularMappingVisitor implements MappingVisitor {
	public FlatAsRegularMappingVisitor(FlatMappingVisitor out) {
		this.next = out;
	}

	@Override
	public Set<MappingFlag> getFlags() {
		return next.getFlags();
	}

	@Override
	public void reset() {
		next.reset();
	}

	@Override
	public boolean visitHeader() throws IOException {
		return next.visitHeader();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		next.visitNamespaces(srcNamespace, dstNamespaces);

		int count = dstNamespaces.size();
		dstNames = new String[count];
		Set<MappingFlag> flags = next.getFlags();

		if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS)) {
			dstClassNames = new String[count];
			dstMemberNames = new String[count];
		} else {
			dstClassNames = dstMemberNames = null;
		}

		dstMemberDescs = flags.contains(MappingFlag.NEEDS_DST_FIELD_DESC) || flags.contains(MappingFlag.NEEDS_DST_METHOD_DESC) ? new String[count] : null;
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) throws IOException {
		next.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() throws IOException {
		return next.visitContent();
	}

	@Override
	public boolean visitClass(String srcName) {
		this.srcClsName = srcName;

		Arrays.fill(dstNames, null);
		if (dstClassNames != null) Arrays.fill(dstClassNames, null);

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) {
		this.srcMemberName = srcName;
		this.srcMemberDesc = srcDesc;

		Arrays.fill(dstNames, null);
		if (dstMemberNames != null) Arrays.fill(dstMemberNames, null);
		if (dstMemberDescs != null) Arrays.fill(dstMemberDescs, null);

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) {
		this.srcMemberName = srcName;
		this.srcMemberDesc = srcDesc;

		Arrays.fill(dstNames, null);
		if (dstMemberNames != null) Arrays.fill(dstMemberNames, null);
		if (dstMemberDescs != null) Arrays.fill(dstMemberDescs, null);

		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) {
		this.srcMemberSubName = srcName;
		this.argIdx = argPosition;
		this.lvIndex = lvIndex;

		Arrays.fill(dstNames, null);

		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
		this.srcMemberSubName = srcName;
		this.argIdx = lvtRowIndex;
		this.lvIndex = lvIndex;
		this.startOpIdx = startOpIdx;
		this.endOpIdx = endOpIdx;

		Arrays.fill(dstNames, null);

		return true;
	}

	@Override
	public boolean visitEnd() throws IOException {
		return next.visitEnd();
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		dstNames[namespace] = name;
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) {
		if (dstMemberDescs != null) dstMemberDescs[namespace] = desc;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		boolean relay;

		switch (targetKind) {
		case CLASS:
			relay = next.visitClass(srcClsName, dstNames);
			if (relay && dstClassNames != null) System.arraycopy(dstNames, 0, dstClassNames, 0, dstNames.length);
			break;
		case FIELD:
			relay = next.visitField(srcClsName, srcMemberName, srcMemberDesc, dstClassNames, dstNames, dstMemberDescs);
			if (relay && dstMemberNames != null) System.arraycopy(dstNames, 0, dstMemberNames, 0, dstNames.length);
			break;
		case METHOD:
			relay = next.visitMethod(srcClsName, srcMemberName, srcMemberDesc, dstClassNames, dstNames, dstMemberDescs);
			if (relay && dstMemberNames != null) System.arraycopy(dstNames, 0, dstMemberNames, 0, dstNames.length);
			break;
		case METHOD_ARG:
			relay = next.visitMethodArg(srcClsName, srcMemberName, srcMemberDesc,
					argIdx, lvIndex, srcMemberSubName,
					dstClassNames, dstMemberNames, dstMemberDescs, dstNames);
			break;
		case METHOD_VAR:
			relay = next.visitMethodVar(srcClsName, srcMemberName, srcMemberDesc,
					argIdx, lvIndex, startOpIdx, endOpIdx, srcMemberSubName,
					dstClassNames, dstMemberNames, dstMemberDescs, dstNames);
			break;
		default:
			throw new IllegalStateException();
		}

		return relay;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		switch (targetKind) {
		case CLASS:
			next.visitClassComment(srcClsName, dstClassNames, comment);
			break;
		case FIELD:
			next.visitFieldComment(srcClsName, srcMemberName, srcMemberDesc,
					dstClassNames, dstMemberNames, dstMemberDescs, comment);
			break;
		case METHOD:
			next.visitMethodComment(srcClsName, srcMemberName, srcMemberDesc,
					dstClassNames, dstMemberNames, dstMemberDescs, comment);
			break;
		case METHOD_ARG:
			next.visitMethodArgComment(srcClsName, srcMemberName, srcMemberDesc, argIdx, lvIndex, srcMemberSubName,
					dstClassNames, dstMemberNames, dstMemberDescs, dstNames, comment);
			break;
		case METHOD_VAR:
			next.visitMethodVarComment(srcClsName, srcMemberName, srcMemberDesc, argIdx, lvIndex, startOpIdx, endOpIdx, srcMemberSubName,
					dstClassNames, dstMemberNames, dstMemberDescs, dstNames, comment);
			break;
		}
	}

	private final FlatMappingVisitor next;

	private String srcClsName;
	private String srcMemberName;
	private String srcMemberDesc;
	private String srcMemberSubName;
	private int argIdx, lvIndex, startOpIdx, endOpIdx;
	private String[] dstNames;
	private String[] dstClassNames;
	private String[] dstMemberNames;
	private String[] dstMemberDescs;
}

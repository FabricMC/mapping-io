/*
 * Copyright (c) 2025 FabricMC
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

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

/**
 * A mapping visitor that nulls out destination names and descriptors that are equal to their source counterparts.
 *
 * @apiNote Extending this class is allowed, but no guarantees are made regarding the stability of its protected members.
 */
public class RedundantDstDataFilter extends ForwardingMappingVisitor {
	public RedundantDstDataFilter(MappingVisitor next) {
		super(next);
	}

	protected void init() {
		srcName = null;
		srcDesc = null;
	}

	@Override
	public void reset() {
		init();
		super.reset();
	}

	@Override
	public boolean visitPackage(String srcName) throws IOException {
		this.srcName = srcName;
		return super.visitPackage(srcName);
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		this.srcName = srcName;
		return super.visitClass(srcName);
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		this.srcName = srcName;
		this.srcDesc = srcDesc;
		return super.visitField(srcName, srcDesc);
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		this.srcName = srcName;
		this.srcDesc = srcDesc;
		return super.visitMethod(srcName, srcDesc);
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		this.srcName = srcName;
		return super.visitMethodArg(argPosition, lvIndex, srcName);
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		this.srcName = srcName;
		return super.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (name != null && name.equals(srcName)) {
			name = null;
		}

		super.visitDstName(targetKind, namespace, name);
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		if (desc != null && desc.equals(srcDesc)) {
			desc = null;
		}

		super.visitDstDesc(targetKind, namespace, desc);
	}

	@Override
	public boolean visitEnd() throws IOException {
		init();
		return super.visitEnd();
	}

	protected String srcName;
	protected String srcDesc;
}

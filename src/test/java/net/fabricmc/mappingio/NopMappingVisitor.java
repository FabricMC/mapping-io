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

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class NopMappingVisitor implements MappingVisitor {
	public NopMappingVisitor(boolean visitSubVisitors) {
		this.visitSubVisitors = visitSubVisitors;
	}

	@Override
	public boolean visitHeader() throws IOException {
		return visitSubVisitors;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
	}

	@Override
	public boolean visitContent() throws IOException {
		return visitSubVisitors;
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		return visitSubVisitors;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
		return visitSubVisitors;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
		return visitSubVisitors;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
		return visitSubVisitors;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
		return visitSubVisitors;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		return visitSubVisitors;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
	}

	protected final boolean visitSubVisitors;
}

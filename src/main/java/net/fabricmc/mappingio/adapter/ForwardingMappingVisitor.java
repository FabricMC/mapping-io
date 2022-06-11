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
import java.util.Objects;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;

public abstract class ForwardingMappingVisitor implements MappingVisitor {
	protected ForwardingMappingVisitor(MappingVisitor next) {
		Objects.requireNonNull(next, "null next");

		this.next = next;
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
	public boolean visitClass(String srcName) throws IOException {
		return next.visitClass(srcName);
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		return next.visitField(srcName, srcDesc);
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		return next.visitMethod(srcName, srcDesc);
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException {
		return next.visitMethodArg(argPosition, lvIndex, srcName);
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
		return next.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
	}

	@Override
	public boolean visitEnd() throws IOException {
		return next.visitEnd();
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		next.visitDstName(targetKind, namespace, name);
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		next.visitDstDesc(targetKind, namespace, desc);
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		return next.visitElementContent(targetKind);
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		next.visitComment(targetKind, comment);
	}

	protected final MappingVisitor next;
}

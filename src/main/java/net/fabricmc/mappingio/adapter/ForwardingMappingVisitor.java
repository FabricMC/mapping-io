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
	public boolean visitHeader() {
		return next.visitHeader();
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) {
		next.visitNamespaces(srcNamespace, dstNamespaces);
	}

	@Override
	public void visitMetadata(String key, String value) {
		next.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() {
		return next.visitContent();
	}

	@Override
	public boolean visitClass(String srcName) {
		return next.visitClass(srcName);
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) {
		return next.visitField(srcName, srcDesc);
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) {
		return next.visitMethod(srcName, srcDesc);
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) {
		return next.visitMethodArg(argPosition, lvIndex, srcName);
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName) {
		return next.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, srcName);
	}

	@Override
	public boolean visitEnd() {
		return next.visitEnd();
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		next.visitDstName(targetKind, namespace, name);
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) {
		next.visitDstDesc(targetKind, namespace, desc);
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) {
		return next.visitElementContent(targetKind);
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) {
		next.visitComment(targetKind, comment);
	}

	protected final MappingVisitor next;
}

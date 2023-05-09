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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

public final class MappingDstNsRemover extends ForwardingMappingVisitor {
	public MappingDstNsRemover(MappingVisitor next, Integer... namespacesToRemove) {
		super(next);

		Objects.requireNonNull(namespacesToRemove, "null namespacesToRemove array");

		this.namespacesIndicesToRemove = Arrays.asList(namespacesToRemove);
		this.namespaceNamesToRemove = Collections.emptyList();
	}

	public MappingDstNsRemover(MappingVisitor next, String... namespacesToRemove) {
		super(next);

		Objects.requireNonNull(namespacesToRemove, "null namespacesToRemove array");

		this.namespacesIndicesToRemove = Collections.emptyList();
		this.namespaceNamesToRemove = Arrays.asList(namespacesToRemove);
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		int listSize = dstNamespaces.size() - Math.max(namespacesIndicesToRemove.size(), namespaceNamesToRemove.size());
		filteredNamespacesIndices = new ArrayList<>(listSize);
		filteredNamespaceNames = new ArrayList<>(listSize);

		for (int i = 0; i < dstNamespaces.size(); i++) {
			String dstName = dstNamespaces.get(i);

			if (namespacesIndicesToRemove.contains(i) || namespaceNamesToRemove.contains(dstName)) {
				continue;
			}

			filteredNamespacesIndices.add(i);
			filteredNamespaceNames.add(dstName);
		}

		super.visitNamespaces(srcNamespace, filteredNamespaceNames);
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (!filteredNamespacesIndices.contains(namespace)) {
			return;
		}

		super.visitDstName(targetKind, filteredNamespacesIndices.indexOf(namespace), name);
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		if (!filteredNamespacesIndices.contains(namespace)) {
			return;
		}

		super.visitDstDesc(targetKind, filteredNamespacesIndices.indexOf(namespace), desc);
	}

	private final List<Integer> namespacesIndicesToRemove;
	private final List<String> namespaceNamesToRemove;
	private List<Integer> filteredNamespacesIndices;
	private List<String> filteredNamespaceNames;
}

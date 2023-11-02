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

package net.fabricmc.mappingio.extras;

import java.util.Objects;

import org.objectweb.asm.commons.Remapper;

import net.fabricmc.mappingio.tree.MappingTreeView;

/**
 * An ASM remapper that remaps between two namespaces in a {@link MappingTreeView}.
 */
public final class MappingTreeRemapper extends Remapper {
	private final MappingTreeView tree;
	private final int fromId;
	private final int toId;

	/**
	 * Constructs a {@code MappingTreeRemapper}.
	 *
	 * @param tree the mapping tree view
	 * @param from the input namespace, must be in the tree
	 * @param to   the output namespace, must be in the tree
	 */
	public MappingTreeRemapper(MappingTreeView tree, String from, String to) {
		Objects.requireNonNull(tree, "Mapping tree cannot be null");
		Objects.requireNonNull(from, "Input namespace cannot be null");
		Objects.requireNonNull(to, "Output namespace cannot be null");
		this.tree = tree;
		this.fromId = getNamespaceId(tree, from);
		this.toId = getNamespaceId(tree, to);
	}

	private static int getNamespaceId(MappingTreeView tree, String namespace) {
		int id = tree.getNamespaceId(namespace);

		if (id == MappingTreeView.NULL_NAMESPACE_ID) {
			throw new IllegalArgumentException(
					"Namespace '" + namespace
							+ "' not present in mapping tree. Available: src: " + tree.getSrcNamespace()
							+ ", dst: " + tree.getDstNamespaces());
		}

		return id;
	}

	private String getNameOrDefault(MappingTreeView.ElementMappingView element, String defaultValue) {
		String targetName = element.getName(toId);
		return targetName != null ? targetName : defaultValue;
	}

	@Override
	public String map(String internalName) {
		return tree.mapClassName(internalName, fromId, toId);
	}

	@Override
	public String mapMethodName(String owner, String name, String descriptor) {
		MappingTreeView.ClassMappingView ownerMapping = tree.getClass(owner, fromId);
		if (ownerMapping == null) return name;

		MappingTreeView.MethodMappingView mapping = ownerMapping.getMethod(name, descriptor, fromId);
		return mapping != null ? getNameOrDefault(mapping, name) : name;
	}

	@Override
	public String mapFieldName(String owner, String name, String descriptor) {
		MappingTreeView.ClassMappingView ownerMapping = tree.getClass(owner, fromId);
		if (ownerMapping == null) return name;

		MappingTreeView.FieldMappingView mapping = ownerMapping.getField(name, descriptor, fromId);
		return mapping != null ? getNameOrDefault(mapping, name) : name;
	}

	@Override
	public String mapRecordComponentName(String owner, String name, String descriptor) {
		return mapFieldName(owner, name, descriptor);
	}
}

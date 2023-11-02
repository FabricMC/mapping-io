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

package net.fabricmc.mappingio.tree;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;

public interface HierarchyInfoProvider<T> {
	String getNamespace();

	/**
	 * @return The internal name of the owner class highest up in the hierarchy.
	 */
	@Nullable
	String resolveField(String owner, String name, @Nullable String desc);

	/**
	 * @return The internal name of the owner class highest up in the hierarchy.
	 */
	@Nullable
	String resolveMethod(String owner, String name, @Nullable String desc);

	@Nullable
	T getMethodHierarchy(String owner, String name, @Nullable String desc);

	@Nullable
	default T getMethodHierarchy(MethodMappingView method) {
		int nsId = method.getTree().getNamespaceId(getNamespace());
		if (nsId == MappingTreeView.NULL_NAMESPACE_ID) throw new IllegalArgumentException("disassociated namespace");

		String owner = method.getOwner().getName(nsId);
		String name = method.getName(nsId);
		String desc = method.getDesc(nsId);

		if (owner == null || name == null) {
			return null;
		} else {
			return getMethodHierarchy(owner, name, desc);
		}
	}

	int getHierarchySize(T hierarchy);

	Collection<? extends MethodMappingView> getHierarchyMethods(T hierarchy, MappingTreeView tree);

	@SuppressWarnings("unchecked")
	default Collection<? extends MethodMapping> getHierarchyMethods(T hierarchy, MappingTree tree) {
		return (Collection<? extends MethodMapping>) getHierarchyMethods(hierarchy, (MappingTreeView) tree);
	}
}

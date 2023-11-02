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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;
import net.fabricmc.mappingio.tree.TinyRemapperHierarchyProvider.HierarchyData;
import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrEnvironment;
import net.fabricmc.tinyremapper.api.TrField;
import net.fabricmc.tinyremapper.api.TrMethod;

public final class TinyRemapperHierarchyProvider implements HierarchyInfoProvider<HierarchyData> {
	public TinyRemapperHierarchyProvider(TrEnvironment env, String namespace) {
		this.env = env;
		this.namespace = namespace;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	@Nullable
	public String resolveField(String owner, String name, @Nullable String desc) {
		TrClass cls = env.getClass(owner);
		if (cls == null) return null;

		TrField field = cls.resolveField(name, desc);

		return field != null ? field.getOwner().getName() : null;
	}

	@Override
	@Nullable
	public String resolveMethod(String owner, String name, @Nullable String desc) {
		if (desc == null) return null; // TODO: Tiny Remapper limitation

		TrClass cls = env.getClass(owner);
		if (cls == null) return null;

		TrMethod method = cls.resolveMethod(name, desc);

		return method != null ? method.getOwner().getName() : null;
	}

	@Override
	@Nullable
	public HierarchyData getMethodHierarchy(String owner, String name, @Nullable String desc) {
		if (desc == null) return null; // TODO: Tiny Remapper limitation

		TrClass cls = env.getClass(owner);
		if (cls == null) return null;

		TrMethod method = cls.resolveMethod(name, desc);
		if (method == null) return null;

		if (!method.isVirtual()) {
			return new HierarchyData(Collections.singletonList(method));
		}

		cls = method.getOwner();

		List<TrMethod> methods = new ArrayList<>();
		methods.add(method);
		Queue<TrClass> toCheckUp = new ArrayDeque<>();
		Queue<TrClass> toCheckDown = new ArrayDeque<>();
		Set<TrClass> queuedUp = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<TrClass> queuedDown = Collections.newSetFromMap(new IdentityHashMap<>());
		toCheckUp.add(cls);
		toCheckDown.add(cls);
		queuedUp.add(cls);
		queuedDown.add(cls);

		do {
			while ((cls = toCheckUp.poll()) != null) {
				for (TrClass parent : cls.getParents()) {
					method = parent.getMethod(name, desc);

					if (method != null && method.isVirtual()
							&& queuedDown.add(parent)) {
						methods.add(method);
						toCheckDown.add(parent);
					}

					if (queuedUp.add(parent)) {
						toCheckUp.add(parent);
					}
				}
			}

			while ((cls = toCheckDown.poll()) != null) {
				for (TrClass child : cls.getChildren()) {
					method = child.getMethod(name, desc);

					if (method != null && method.isVirtual()
							&& queuedUp.add(child)) {
						methods.add(method);
						toCheckUp.add(child);
					}

					if (queuedDown.add(child)) {
						toCheckDown.add(child);
					}
				}
			}
		} while (!toCheckUp.isEmpty() || !toCheckDown.isEmpty());

		assert methods.size() == new HashSet<>(methods).size();

		return new HierarchyData(methods);
	}

	@Override
	public int getHierarchySize(@Nullable HierarchyData hierarchy) {
		return hierarchy != null ? hierarchy.methods.size() : 0;
	}

	@Override
	public Collection<? extends MethodMappingView> getHierarchyMethods(@Nullable HierarchyData hierarchy, MappingTreeView tree) {
		if (hierarchy == null) return Collections.emptyList();

		List<MethodMappingView> ret = new ArrayList<>(hierarchy.methods.size());
		int ns = tree.getNamespaceId(namespace);
		assert ns != MappingTreeView.NULL_NAMESPACE_ID;

		for (TrMethod method : hierarchy.methods) {
			MethodMappingView m = tree.getMethod(method.getOwner().getName(), method.getName(), method.getDesc(), ns);
			if (m != null) ret.add(m);
		}

		return ret;
	}

	public static final class HierarchyData {
		HierarchyData(Collection<TrMethod> methods) {
			this.methods = methods;
		}

		final Collection<TrMethod> methods;
	}

	private final TrEnvironment env;
	private final String namespace;
}

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

package net.fabricmc.mappingio.tree;

import java.util.Collection;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.ElementMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodArgMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodVarMappingView;

/**
 * A {@link Collection}-based read-only view of element mappings present in a mapping tree.
 *
 * <p>The meaning of "compatibility" as used in {@link #containsCompatible(Object)}
 * and {@link #containsAllCompatible(Collection)} is determined by the backing
 * mapping tree's element mapping getters.
 *
 * @param <E> The stored Elements' type.
 * @param <V> The View type correlating to the stored mapping type.
 */
@ApiStatus.NonExtendable
public interface MappingCollectionView<E extends ElementMappingView, V extends ElementMappingView> extends Collection<E> {
	boolean containsCompatible(V o);
	boolean containsAllCompatible(Collection<? extends V> c);

	interface ClassMappingCollectionView<E extends ClassMappingView> extends MappingCollection<E, ClassMappingView> { }

	interface FieldMappingCollectionView<E extends FieldMappingView> extends MappingCollection<E, FieldMappingView> { }

	interface MethodMappingCollectionView<E extends MethodMappingView> extends MappingCollection<E, MethodMappingView> { }

	interface MethodArgMappingCollectionView<E extends MethodArgMappingView> extends MappingCollection<E, MethodArgMappingView> { }

	interface MethodVarMappingCollectionView<E extends MethodVarMappingView> extends MappingCollection<E, MethodVarMappingView> { }
}

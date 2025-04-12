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
 * A {@link Collection}-based modifiable view of element mappings present in a mapping tree.
 *
 * <p>Contrary to what's defined in {@link Collection}'s Javadocs, the {@code add}
 * methods here do not guarantee adding a passed element into the collection,
 * instead its data may be merged into a compatible existing entry in case and such that
 * {@link #containsCompatible(Object)} returns {@code true} for the passed element.
 *
 * <p>The following methods also have alternative versions that operate on
 * compatible elements rather than equal ones:
 * <ul>
 * <li>{@link Collection#contains(Object)},
 * <li>{@link Collection#containsAll(Collection)},
 * <li>{@link Collection#remove(Object)},
 * <li>{@link Collection#removeAll(Collection)} and
 * <li>{@link Collection#retainAll(Collection)}.
 * </ul>
 * Compatibility is determined via the backing mapping tree's mapping element getters.
 *
 * <p>Additionally, the {@link Collection#add(Object)} and {@link Collection#addAll(Collection)}
 * methods have overloaded variants that accept read-only views of the held mapping element type,
 * which are converted to the tree's internal representation if necessary and then added to the tree.
 *
 * @param <E> The stored Elements' type.
 * @param <V> The View type correlating to the stored mapping type.
 */
@ApiStatus.NonExtendable
public interface MappingCollection<E extends ElementMappingView, V extends ElementMappingView> extends MappingCollectionView<E, V> {
	boolean add(V e);
	boolean addAllViews(Collection<? extends V> c);
	boolean removeCompatible(V e);
	boolean removeAllCompatible(Collection<? extends V> c);
	boolean retainAllCompatible(Collection<? extends V> c);

	interface ClassMappingCollection<E extends ClassMappingView> extends ClassMappingCollectionView<E> { }

	interface FieldMappingCollection<E extends FieldMappingView> extends FieldMappingCollectionView<E> { }

	interface MethodMappingCollection<E extends MethodMappingView> extends MethodMappingCollectionView<E> { }

	interface MethodArgMappingCollection<E extends MethodArgMappingView> extends MethodArgMappingCollectionView<E> { }

	interface MethodVarMappingCollection<E extends MethodVarMappingView> extends MethodVarMappingCollectionView<E> { }
}

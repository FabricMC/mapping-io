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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.ElementMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.ElementMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodArgMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodVarMappingView;

/**
 * @param <E> The stored Elements' type.
 * @param <V> The View type correlating to the stored mapping type.
 * @param <O> The stored elements' Owner type, or any if this is a root collection.
 */
abstract class MappingCollectionImpl<E extends ElementMappingView, V extends ElementMappingView, O extends ElementMapping> implements MappingCollection<E, V> {
	private MappingCollectionImpl(MemoryMappingTree tree, @Nullable O owner, Collection<E> backing, MappedElementKind elementKind) {
		this.tree = tree;
		this.owner = owner;
		this.backing = backing;
		this.elementKind = elementKind;
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Nullable
	protected abstract E getCompatible(V o);

	@Override
	public boolean contains(Object o) {
		if (o instanceof ElementMappingView) {
			ElementMappingView oElem = (ElementMappingView) o;

			if (oElem.getKind() == elementKind) {
				return backing.contains(o);
			}
		}

		return false;
	}

	@Override
	public boolean containsCompatible(V o) {
		return getCompatible(o) != null;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean containsAllCompatible(Collection<? extends V> c) {
		for (V o : c) {
			if (!containsCompatible(o)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Iterator<E> iterator() {
		return new IteratorWrapper(new ArrayList<>(backing).iterator(), this);
	}

	@Override
	public Object[] toArray() {
		return backing.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return backing.toArray(a);
	}

	// Mutating methods

	@Override
	public abstract boolean add(V e);

	@Override
	public boolean addAllViews(Collection<? extends V> c) {
		tree.assertNotInVisitPass();
		boolean addedAny = false;

		for (V e : c) {
			addedAny |= add(e);
		}

		return addedAny;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean addAll(Collection<? extends E> c) {
		return addAllViews((Collection<? extends V>) c);
	}

	@Override
	public boolean remove(Object o) {
		tree.assertNotInVisitPass();
		return backing.remove(o);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean removeCompatible(V o) {
		tree.assertNotInVisitPass();

		if (o instanceof ElementMappingView) {
			ElementMappingView oElem = (ElementMappingView) o;

			if (oElem.getKind() == elementKind) {
				return removeCompatibleInternal((V) oElem);
			}
		}

		return false;
	}

	protected abstract boolean removeCompatibleInternal(V e);

	@Override
	public boolean removeAll(Collection<?> c) {
		tree.assertNotInVisitPass();
		boolean removedAny = false;

		for (Object o : c) {
			removedAny |= remove(o);
		}

		return removedAny;
	}

	@Override
	public boolean removeAllCompatible(Collection<? extends V> c) {
		tree.assertNotInVisitPass();
		boolean removedAny = false;

		for (V o : c) {
			removedAny |= removeCompatible(o);
		}

		return removedAny;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		tree.assertNotInVisitPass();
		boolean removedAny = false;

		for (Iterator<E> it = iterator(); it.hasNext();) {
			E e = it.next();

			if (!c.contains(e)) {
				it.remove();
				removedAny = true;
			}
		}

		return removedAny;
	}

	@Override
	public boolean retainAllCompatible(Collection<? extends V> c) {
		tree.assertNotInVisitPass();
		Set<E> toRemove = new HashSet<>(this);
		boolean removedAny = false;

		for (V o : c) {
			E e = getCompatible(o);

			if (e != null) {
				toRemove.remove(e);

				if (toRemove.isEmpty()) {
					break;
				}
			}
		}

		for (E e : toRemove) {
			assert remove(e);
			removedAny = true;
		}

		return removedAny;
	}

	@Override
	public void clear() {
		tree.assertNotInVisitPass();

		for (Iterator<E> it = iterator(); it.hasNext();) {
			it.next();
			it.remove();
		}
	}

	private final class IteratorWrapper implements Iterator<E> {
		IteratorWrapper(Iterator<E> backing, MappingCollectionImpl<E, V, O> owner) {
			this.backing = backing;
			this.owner = owner;
		}

		@Override
		public boolean hasNext() {
			return backing.hasNext();
		}

		@Override
		public E next() {
			lastReturned = backing.next();
			return lastReturned;
		}

		@Override
		public void remove() {
			owner.remove(lastReturned);
		}

		private final Iterator<E> backing;
		private final MappingCollectionImpl<E, V, O> owner;
		private E lastReturned;
	}

	static class ClassMappingCollectionImpl<E extends ClassMappingView> extends MappingCollectionImpl<E, ClassMappingView, ClassMapping> implements ClassMappingCollection<E> {
		ClassMappingCollectionImpl(MemoryMappingTree tree, Collection<E> backing) {
			super(tree, null, backing, MappedElementKind.CLASS);
		}

		@Override
		public boolean add(ClassMappingView e) {
			tree.addClass(e);
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E getCompatible(ClassMappingView o) {
			return (E) tree.getClass(o.getSrcName());
		}

		@Override
		protected boolean removeCompatibleInternal(ClassMappingView o) {
			return tree.removeClass(o.getSrcName()) != null;
		}
	}

	static class FieldMappingCollectionImpl<E extends FieldMappingView, O extends ClassMapping> extends MappingCollectionImpl<E, FieldMappingView, O> implements FieldMappingCollection<E> {
		FieldMappingCollectionImpl(MemoryMappingTree tree, O owner, Collection<E> backing) {
			super(tree, owner, backing, MappedElementKind.FIELD);
		}

		@Override
		public boolean add(FieldMappingView e) {
			owner.addField(e);
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E getCompatible(FieldMappingView o) {
			return (E) owner.getField(o.getSrcName(), o.getSrcDesc());
		}

		@Override
		protected boolean removeCompatibleInternal(FieldMappingView o) {
			return owner.removeField(o.getSrcName(), o.getSrcDesc()) != null;
		}
	}

	static class MethodMappingCollectionImpl<E extends MethodMappingView, O extends ClassMapping> extends MappingCollectionImpl<E, MethodMappingView, O> implements MethodMappingCollection<E> {
		MethodMappingCollectionImpl(MemoryMappingTree tree, O owner, Collection<E> backing) {
			super(tree, owner, backing, MappedElementKind.METHOD);
		}

		@Override
		public boolean add(MethodMappingView e) {
			owner.addMethod(e);
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E getCompatible(MethodMappingView o) {
			return (E) owner.getMethod(o.getSrcName(), o.getSrcDesc());
		}

		@Override
		protected boolean removeCompatibleInternal(MethodMappingView o) {
			return owner.removeMethod(o.getSrcName(), o.getSrcDesc()) != null;
		}
	}

	static class MethodArgMappingCollectionImpl<E extends MethodArgMappingView, O extends MethodMapping> extends MappingCollectionImpl<E, MethodArgMappingView, O> implements MethodArgMappingCollection<E> {
		MethodArgMappingCollectionImpl(MemoryMappingTree tree, O owner, Collection<E> backing) {
			super(tree, owner, backing, MappedElementKind.METHOD_ARG);
		}

		@Override
		public boolean add(MethodArgMappingView e) {
			owner.addArg(e);
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E getCompatible(MethodArgMappingView o) {
			return (E) owner.getArg(o.getArgPosition(), o.getLvIndex(), o.getSrcName());
		}

		@Override
		protected boolean removeCompatibleInternal(MethodArgMappingView o) {
			return owner.removeArg(o.getArgPosition(), o.getLvIndex(), o.getSrcName()) != null;
		}
	}

	static class MethodVarMappingCollectionImpl<E extends MethodVarMappingView, O extends MethodMapping> extends MappingCollectionImpl<E, MethodVarMappingView, O> implements MethodVarMappingCollection<E> {
		MethodVarMappingCollectionImpl(MemoryMappingTree tree, O owner, Collection<E> backing) {
			super(tree, owner, backing, MappedElementKind.METHOD_VAR);
		}

		@Override
		public boolean add(MethodVarMappingView e) {
			owner.addVar(e);
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected E getCompatible(MethodVarMappingView o) {
			return (E) owner.getVar(o.getLvtRowIndex(), o.getLvIndex(), o.getStartOpIdx(), o.getEndOpIdx(), o.getSrcName());
		}

		@Override
		protected boolean removeCompatibleInternal(MethodVarMappingView o) {
			return owner.removeVar(o.getLvtRowIndex(), o.getLvIndex(), o.getStartOpIdx(), o.getEndOpIdx(), o.getSrcName()) != null;
		}
	}

	protected final MemoryMappingTree tree;
	protected final @Nullable O owner;
	protected final MappedElementKind elementKind;
	protected final Collection<E> backing;
}

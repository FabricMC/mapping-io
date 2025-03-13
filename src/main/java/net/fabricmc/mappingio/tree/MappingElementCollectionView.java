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
import java.util.Iterator;

final class MappingElementCollectionView<E> implements Collection<E> {
	MappingElementCollectionView(MemoryMappingTree owner, Collection<E> backing) {
		this.owner = owner;
		this.backing = backing;
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return backing.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return backing.containsAll(c);
	}

	@Override
	public Iterator<E> iterator() {
		return new IteratorWrapper(backing.iterator());
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
	public boolean add(E e) {
		throw new UnsupportedOperationException("Adding children to mapping elements using collections is not allowed");
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException("Adding children to mapping elements using collections is not allowed");
	}

	@Override
	public boolean remove(Object o) {
		owner.assertNotInVisitPass();
		return backing.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		owner.assertNotInVisitPass();
		return backing.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		owner.assertNotInVisitPass();
		return backing.retainAll(c);
	}

	@Override
	public void clear() {
		owner.assertNotInVisitPass();
	}

	private final MemoryMappingTree owner;
	private final Collection<E> backing;

	private final class IteratorWrapper implements Iterator<E> {
		IteratorWrapper(Iterator<E> backing) {
			this.backing = backing;
		}

		@Override
		public boolean hasNext() {
			return backing.hasNext();
		}

		@Override
		public E next() {
			return backing.next();
		}

		@Override
		public void remove() {
			owner.assertNotInVisitPass();
			backing.remove();
		}

		private final Iterator<E> backing;
	}
}

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

import java.util.AbstractList;
import java.util.List;

final class MappingElementListView<E> extends AbstractList<E> {
	MappingElementListView(MemoryMappingTree owner, List<E> backing) {
		this.owner = owner;
		this.backing = backing;
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public E get(int index) {
		return backing.get(index);
	}

	@Override
	public E remove(int index) {
		owner.assertNotInVisitPass();
		return backing.remove(index);
	}

	private final MemoryMappingTree owner;
	private final List<E> backing;
}

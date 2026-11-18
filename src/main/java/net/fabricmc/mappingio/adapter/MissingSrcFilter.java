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

import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class MissingSrcFilter extends ForwardingMappingVisitor {
	public MissingSrcFilter(MemoryMappingTree next) {
		super(next);

		this.next = next;
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		if (next.getClass(srcName) != null) {
			this.classSrcName = srcName;
			return super.visitClass(srcName);
		}

		return false;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		if (next.getField(this.classSrcName, srcName, srcDesc) != null) {
			return super.visitField(srcName, srcDesc);
		}

		return false;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		if (next.getMethod(this.classSrcName, srcName, srcDesc) != null) {
			return super.visitMethod(srcName, srcDesc);
		}

		return false;
	}

	protected final MemoryMappingTree next;
	protected String classSrcName;
}

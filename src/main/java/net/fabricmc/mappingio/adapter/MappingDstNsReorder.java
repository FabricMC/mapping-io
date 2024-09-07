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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

/**
 * A mapping visitor that reorders and/or drops destination namespaces.
 */
public final class MappingDstNsReorder extends ForwardingMappingVisitor {
	/**
	 * @param next The next visitor to forward the data to.
	 * @param newDstNs The destination namespaces, in the desired order.
	 * Omitting entries from the list is going to drop them.
	 */
	public MappingDstNsReorder(MappingVisitor next, List<String> newDstNs) {
		super(next);

		Objects.requireNonNull(newDstNs, "null newDstNs list");

		this.newDstNs = newDstNs;
	}

	/**
	 * @param next The next visitor to forward the data to
	 * @param newDstNs The destination namespaces, in the desired order.
	 * Omitting entries from the list is going to drop them.
	 */
	public MappingDstNsReorder(MappingVisitor next, String... newDstNs) {
		this(next, Arrays.asList(newDstNs));
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		nsMap = new int[dstNamespaces.size()];

		for (int i = 0; i < dstNamespaces.size(); i++) {
			nsMap[i] = newDstNs.indexOf(dstNamespaces.get(i));
		}

		super.visitNamespaces(srcNamespace, newDstNs);
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		namespace = nsMap[namespace];

		if (namespace >= 0) {
			super.visitDstName(targetKind, namespace, name);
		}
	}

	@Override
	public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
		namespace = nsMap[namespace];

		if (namespace >= 0) {
			super.visitDstDesc(targetKind, namespace, desc);
		}
	}

	private final List<String> newDstNs;
	private int[] nsMap;
}

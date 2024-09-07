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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.fabricmc.mappingio.MappingVisitor;

/**
 * A mapping visitor that renames namespaces.
 */
public final class MappingNsRenamer extends ForwardingMappingVisitor {
	/**
	 * @param next The next visitor to forward the data to.
	 * @param nameMap A map of which namespaces should be renamed to which new names.
	 */
	public MappingNsRenamer(MappingVisitor next, Map<String, String> nameMap) {
		super(next);

		Objects.requireNonNull(nameMap, "null name map");

		this.nameMap = nameMap;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		String newSrcNamespace = nameMap.getOrDefault(srcNamespace, srcNamespace);
		List<String> newDstNamespaces = new ArrayList<>(dstNamespaces.size());

		for (String ns : dstNamespaces) {
			newDstNamespaces.add(nameMap.getOrDefault(ns, ns));
		}

		super.visitNamespaces(newSrcNamespace, newDstNamespaces);
	}

	private final Map<String, String> nameMap;
}

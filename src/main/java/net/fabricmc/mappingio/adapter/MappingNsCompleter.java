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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

public final class MappingNsCompleter extends ForwardingMappingVisitor {
	public MappingNsCompleter(MappingVisitor next, Map<String, String> alternatives) {
		this(next, alternatives, false);
	}

	public MappingNsCompleter(MappingVisitor next, Map<String, String> alternatives, boolean addMissing) {
		super(next);

		this.alternatives = alternatives;
		this.addMissing = addMissing;
	}

	@Override
	public boolean visitHeader() {
		relayHeaderOrMetadata = next.visitHeader();

		return true;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) {
		if (addMissing) {
			boolean copied = false;

			for (String ns : alternatives.keySet()) {
				if (ns.equals(srcNamespace) || dstNamespaces.contains(ns)) {
					continue;
				}

				if (!copied) {
					dstNamespaces = new ArrayList<>(dstNamespaces);
					copied = true;
				}

				dstNamespaces.add(ns);
			}
		}

		int count = dstNamespaces.size();
		alternativesMapping = new int[count];
		dstNames = new String[count];

		for (int i = 0; i < count; i++) {
			String src = alternatives.get(dstNamespaces.get(i));
			int srcIdx;

			if (src == null) {
				srcIdx = i;
			} else if (src.equals(srcNamespace)) {
				srcIdx = -1;
			} else {
				srcIdx = dstNamespaces.indexOf(src);
				if (srcIdx < 0) throw new RuntimeException("invalid alternative mapping ns "+src+": not in "+dstNamespaces+" or "+srcNamespace);
			}

			alternativesMapping[i] = srcIdx;
		}

		if (relayHeaderOrMetadata) next.visitNamespaces(srcNamespace, dstNamespaces);
	}

	@Override
	public void visitMetadata(String key, String value) {
		if (relayHeaderOrMetadata) next.visitMetadata(key, value);
	}

	@Override
	public boolean visitContent() {
		relayHeaderOrMetadata = true; // for in-content metadata

		return next.visitContent();
	}

	@Override
	public boolean visitClass(String srcName) {
		this.srcName = srcName;

		return next.visitClass(srcName);
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) {
		this.srcName = srcName;

		return next.visitField(srcName, srcDesc);
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) {
		this.srcName = srcName;

		return next.visitMethod(srcName, srcDesc);
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) {
		this.srcName = srcName;

		return next.visitMethodArg(argPosition, lvIndex, srcName);
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName) {
		this.srcName = srcName;

		return next.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, srcName);
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		dstNames[namespace] = name;
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) {
		nsLoop: for (int i = 0; i < dstNames.length; i++) {
			String name = dstNames[i];

			if (name == null) {
				int src = i;
				long visited = 1L << src;

				do {
					int newSrc = alternativesMapping[src];

					if (newSrc < 0) { // mapping to src name
						name = srcName;
						break; // srcName must never be null
					} else if (newSrc == src) { // no-op (identity) mapping, explicit in case src > 64
						continue nsLoop; // always null
					} else if ((visited & 1L << newSrc) != 0) { // cyclic mapping
						continue nsLoop; // always null
					} else {
						src = newSrc;
						name = dstNames[src];
						visited |= 1L << src;
					}
				} while (name == null);

				assert name != null;
			}

			next.visitDstName(targetKind, i, name);
		}

		Arrays.fill(dstNames, null);

		return next.visitElementContent(targetKind);
	}

	private final Map<String, String> alternatives;
	private final boolean addMissing;
	private int[] alternativesMapping;

	private String srcName;
	private String[] dstNames;

	private boolean relayHeaderOrMetadata;
}

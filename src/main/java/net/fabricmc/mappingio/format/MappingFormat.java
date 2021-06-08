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

package net.fabricmc.mappingio.format;

public enum MappingFormat {
	TINY("Tiny", "tiny", true, true, false, false, false),
	TINY_2("Tiny v2", "tiny", true, true, true, true, true),
	ENIGMA("Enigma", null, false, true, true, true, false),
	MCP("MCP", null, false, false, true, true, false),
	SRG("SRG", "srg", false, false, false, false, false),
	TSRG("TSRG", "tsrg", false, false, false, false, false),
	TSRG2("TSRG2", "tsrg", true, false, false, true, false),
	PROGUARD("ProGuard", "map", false, true, false, false, false);

	MappingFormat(String name, String fileExt,
			boolean hasNamespaces, boolean hasFieldDescriptors,
			boolean supportsComments, boolean supportsArgs, boolean supportsLocals) {
		this.name = name;
		this.fileExt = fileExt;
		this.hasNamespaces = hasNamespaces;
		this.hasFieldDescriptors = hasFieldDescriptors;
		this.supportsComments = supportsComments;
		this.supportsArgs = supportsArgs;
		this.supportsLocals = supportsLocals;
	}

	public boolean hasSingleFile() {
		return fileExt != null;
	}

	public String getGlobPattern() {
		if (fileExt == null) throw new UnsupportedOperationException("not applicable to dir based format");

		return "*."+fileExt;
	}

	public final String name;
	public final String fileExt;
	public final boolean hasNamespaces;
	public final boolean hasFieldDescriptors;
	public final boolean supportsComments;
	public final boolean supportsArgs;
	public final boolean supportsLocals;
}
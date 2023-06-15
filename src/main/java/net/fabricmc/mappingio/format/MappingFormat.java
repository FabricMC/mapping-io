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
	TINY_FILE("Tiny file", "tiny", true, true, false, false, false),
	TINY_2_FILE("Tiny v2 file", "tiny", true, true, true, true, true),
	ENIGMA_FILE("Enigma file", "mapping", false, true, true, true, false),
	ENIGMA_DIR("Enigma directory", null, false, true, true, true, false),
	MCP_DIR("MCP directory", null, false, false, true, true, false),
	SRG_FILE("SRG file", "srg", false, false, false, false, false),
	TSRG_FILE("TSRG file", "tsrg", false, false, false, false, false),
	TSRG_2_FILE("TSRG2 file", "tsrg", true, false, false, true, false),
	PROGUARD_FILE("ProGuard file", "map", false, true, false, false, false);

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

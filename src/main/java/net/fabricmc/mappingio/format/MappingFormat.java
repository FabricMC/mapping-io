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
	TINY_FILE("Tiny file", "tiny", true, true, false, false, false, MetadataSupport.ARBITRARY, MetadataSupport.NONE),
	TINY_2_FILE("Tiny v2 file", "tiny", true, true, true, true, true, MetadataSupport.ARBITRARY, MetadataSupport.NONE),
	ENIGMA_FILE("Enigma file", "mappings", false, true, true, true, false, MetadataSupport.NONE, MetadataSupport.HARDCODED),
	ENIGMA_DIR("Enigma directory", null, false, true, true, true, false, MetadataSupport.NONE, MetadataSupport.HARDCODED),
	MCP_DIR("MCP directory", null, false, false, true, true, false, MetadataSupport.NONE, MetadataSupport.NONE),
	SRG_FILE("SRG file", "srg", false, false, false, false, false, MetadataSupport.NONE, MetadataSupport.NONE),
	TSRG_FILE("TSRG file", "tsrg", false, false, false, false, false, MetadataSupport.NONE, MetadataSupport.NONE),
	TSRG_2_FILE("TSRG2 file", "tsrg", true, false, false, true, false, MetadataSupport.NONE, MetadataSupport.HARDCODED),
	PROGUARD_FILE("ProGuard file", "map", false, true, false, false, false, MetadataSupport.NONE, MetadataSupport.HARDCODED),
	MATCH_FILE("Match file", "match", false, true, false, true, true, MetadataSupport.HARDCODED, MetadataSupport.HARDCODED);

	MappingFormat(String name, String fileExt,
			boolean hasNamespaces, boolean hasFieldDescriptors,
			boolean supportsComments, boolean supportsArgs, boolean supportsLocals,
			MetadataSupport fileMetadataSupport, MetadataSupport elementMetadataSupport) {
		this.name = name;
		this.fileExt = fileExt;
		this.hasNamespaces = hasNamespaces;
		this.hasFieldDescriptors = hasFieldDescriptors;
		this.supportsComments = supportsComments;
		this.supportsArgs = supportsArgs;
		this.supportsLocals = supportsLocals;
		this.fileMetadataSupport = fileMetadataSupport;
		this.elementMetadataSupport = elementMetadataSupport;
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
	public final MetadataSupport fileMetadataSupport;
	public final MetadataSupport elementMetadataSupport;

	public enum MetadataSupport {
		/** No metadata at all. */
		NONE,

		/** Only some select properties.  */
		HARDCODED,

		/** Arbitrary metadata may be attached. */
		ARBITRARY
	}
}

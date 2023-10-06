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

/**
 * Represents a supported mapping format. Feature comparison table:
 * <table>
 *   <tr>
 *     <th>Format</th>
 *     <th>Namespaces</th>
 *     <th>Field descriptors</th>
 *     <th>Comments</th>
 *     <th>Parameters</th>
 *     <th>Local variables</th>
 *     <th>Metadata</th>
 *   </tr>
 *   <tr>
 *     <td>Tiny v1</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✔ (Currently limited support)</td>
 *   </tr>
 *   <tr>
 *     <td>Tiny v2</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *   </tr>
 *   <tr>
 *     <td>Enigma</td>
 *     <td>✖</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *   </tr>
 *   <tr>
 *     <td>SRG</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *   </tr>
 *   <tr>
 *     <td>TSRG</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *   </tr>
 *   <tr>
 *     <td>TSRG2</td>
 *     <td>✔</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✔</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *   </tr>
 *   <tr>
 *     <td>ProGuard</td>
 *     <td>✖</td>
 *     <td>✔</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *     <td>✖</td>
 *   </tr>
 * </table>
 */
// Format order is determined by importance to Fabric tooling, format family and release order therein.
public enum MappingFormat {
	/**
	 * The {@code Tiny} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny">here</a>.
	 */
	TINY_FILE("Tiny file", "tiny", true, true, false, false, false),

	/**
	 * The {@code Tiny v2} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny2">here</a>.
	 */
	TINY_2_FILE("Tiny v2 file", "tiny", true, true, true, true, true),

	/**
	 * Enigma's mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 */
	ENIGMA_FILE("Enigma file", "mappings", false, true, true, true, false),

	/**
	 * Enigma's mapping format (in directory form), as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 */
	ENIGMA_DIR("Enigma directory", null, false, true, true, true, false),

	/**
	 * The {@code SRG} ({@code Searge RetroGuard}) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L81">here</a>.
	 */
	SRG_FILE("SRG file", "srg", false, false, false, false, false),

	/**
	 * The {@code TSRG} ({@code Tiny SRG}, since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L213">here</a>.
	 */
	TSRG_FILE("TSRG file", "tsrg", false, false, false, false, false),

	/**
	 * The {@code TSRG v2} mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L262-L285">here</a>.
	 */
	TSRG_2_FILE("TSRG2 file", "tsrg", true, false, false, true, false),

	/**
	 * ProGuard's mapping format, as specified <a href="https://www.guardsquare.com/manual/tools/retrace">here</a>.
	 */
	PROGUARD_FILE("ProGuard file", "map", false, true, false, false, false),
	MCP_DIR("MCP directory", null, false, false, true, true, false);

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

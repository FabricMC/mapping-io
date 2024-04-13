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

import org.jetbrains.annotations.Nullable;

/**
 * Represents a supported mapping format. Every format can be assumed to have an associated reader available.
 *
 * <p>A feature comparison table can be found <a href="https://fabricmc.net/wiki/documentation:mapping_formats">here</a>.
 */
// Format order is determined by importance to Fabric tooling, format family and release order therein (synced with the table linked above).
public enum MappingFormat {
	/**
	 * The {@code Tiny} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * File metadata only has limited support as of now, and is hardcoded to intermediary counters.
	 */
	TINY_FILE("Tiny file", "tiny", true, true, false, false, false, true),

	/**
	 * The {@code Tiny v2} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny2">here</a>.
	 */
	TINY_2_FILE("Tiny v2 file", "tiny", true, true, true, true, true, true),

	/**
	 * Enigma's mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Access modifiers are currently not supported.
	 */
	ENIGMA_FILE("Enigma file", "mapping", false, true, true, true, false, true),

	/**
	 * Enigma's mapping format (in directory form), as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Access modifiers are currently not supported.
	 */
	ENIGMA_DIR("Enigma directory", null, false, true, true, true, false, true),

	/**
	 * The {@code SRG} ("Searge RetroGuard") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L81">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	SRG_FILE("SRG file", "srg", false, false, false, false, false, true),

	/**
	 * The {@code XSRG} ("Extended SRG") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L84">here</a>.
	 * Same as SRG, but with field descriptors.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	XSRG_FILE("XSRG file", "xsrg", false, true, false, false, false, true),

	/**
	 * The {@code JAM} ("Java Associated Mapping"; formerly {@code SRGX}) mapping format, as specified <a href="https://github.com/caseif/JAM">here</a>.
	 */
	JAM_FILE("JAM file", "jam", false, true, false, true, false, true),

	/**
	 * The {@code CSRG} ("Compact SRG", since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L207">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	CSRG_FILE("CSRG file", "csrg", false, false, false, false, false, true),

	/**
	 * The {@code TSRG} ("Tiny SRG", since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L213">here</a>.
	 * Same as CSRG, but hierarchical instead of flat.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	TSRG_FILE("TSRG file", "tsrg", false, false, false, false, false, true),

	/**
	 * The {@code TSRG v2} mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L262-L285">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings and static markers for methods are currently not supported.
	 */
	TSRG_2_FILE("TSRG2 file", "tsrg", true, true, false, true, false, true),

	/**
	 * ProGuard's mapping format, as specified <a href="https://www.guardsquare.com/manual/tools/retrace">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Line numbers are currently not supported.
	 */
	PROGUARD_FILE("ProGuard file", "txt", false, true, false, false, false, true),

	/**
	 * Recaf's {@code Simple} mapping format, as specified <a href="https://github.com/Col-E/Recaf/blob/e9765d4e02991a9dd48e67c9572a063c14552e7c/src/main/java/me/coley/recaf/mapping/SimpleMappings.java#L14-L23">here</a>.
	 */
	RECAF_SIMPLE_FILE("Recaf Simple file", "txt", false, true, false, false, false, true),

	/**
	 * The {@code JOBF} mapping format, as specified <a href="https://github.com/skylot/jadx/blob/2d5c0fda4a0c5d16207a5f48edb72e6efa7d5bbd/jadx-core/src/main/java/jadx/core/deobf/DeobfPresets.java">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	JOBF_FILE("JOBF file", "jobf", false, true, false, false, false, true);

	MappingFormat(String name, @Nullable String fileExt,
			boolean hasNamespaces, boolean hasFieldDescriptors,
			boolean supportsComments, boolean supportsArgs, boolean supportsLocals,
			boolean hasWriter) {
		this.name = name;
		this.fileExt = fileExt;
		this.hasNamespaces = hasNamespaces;
		this.hasFieldDescriptors = hasFieldDescriptors;
		this.supportsComments = supportsComments;
		this.supportsArgs = supportsArgs;
		this.supportsLocals = supportsLocals;
		this.hasWriter = hasWriter;
	}

	public boolean hasSingleFile() {
		return fileExt != null;
	}

	public String getGlobPattern() {
		if (fileExt == null) throw new UnsupportedOperationException("not applicable to dir based format");

		return "*."+fileExt;
	}

	public final String name;
	@Nullable
	public final String fileExt;
	public final boolean hasNamespaces;
	public final boolean hasFieldDescriptors;
	public final boolean supportsComments;
	public final boolean supportsArgs;
	public final boolean supportsLocals;
	public final boolean hasWriter;
}

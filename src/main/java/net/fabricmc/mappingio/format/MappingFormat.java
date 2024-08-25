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

import net.fabricmc.mappingio.format.FeatureSet.ElementCommentSupport;
import net.fabricmc.mappingio.format.FeatureSet.FeaturePresence;
import net.fabricmc.mappingio.format.FeatureSet.MetadataSupport;

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
	TINY_FILE("Tiny file", "tiny", true, FeatureSetBuilder.create()
			.withNamespaces(true)
			.withFileMetadata(MetadataSupport.FIXED) // TODO: change this to ARBITRARY once https://github.com/FabricMC/mapping-io/pull/29 is merged
			.withClasses(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withFileComments(true)),

	/**
	 * The {@code Tiny v2} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny2">here</a>.
	 */
	TINY_2_FILE("Tiny v2 file", "tiny", true, FeatureSetBuilder.create()
			.withNamespaces(true)
			.withFileMetadata(MetadataSupport.ARBITRARY)
			.withClasses(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withArgs(a -> a
					.withLvIndices(FeaturePresence.REQUIRED)
					.withSrcNames(FeaturePresence.OPTIONAL)
					.withDstNames(FeaturePresence.OPTIONAL))
			.withVars(v -> v
					.withLvIndices(FeaturePresence.REQUIRED)
					.withLvtRowIndices(FeaturePresence.OPTIONAL)
					.withStartOpIndices(FeaturePresence.REQUIRED)
					.withSrcNames(FeaturePresence.OPTIONAL)
					.withDstNames(FeaturePresence.OPTIONAL))
			.withElementComments(ElementCommentSupport.SHARED)
			.withFileComments(true)), // only in reserved places

	/**
	 * Enigma's mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Access modifiers are currently not supported.
	 */
	ENIGMA_FILE("Enigma file", "mapping", true, FeatureSetBuilder.create()
			.withElementMetadata(MetadataSupport.FIXED) // access modifiers
			.withClasses(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withArgs(a -> a
					.withLvIndices(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.OPTIONAL))
			.withElementComments(ElementCommentSupport.SHARED)
			.withFileComments(true)),

	/**
	 * Enigma's mapping format (in directory form), as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Access modifiers are currently not supported.
	 */
	ENIGMA_DIR("Enigma directory", null, true, FeatureSetBuilder.createFrom(ENIGMA_FILE.features)),

	/**
	 * The {@code SRG} ("Searge RetroGuard") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L81">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	SRG_FILE("SRG file", "srg", true, FeatureSetBuilder.create()
			.withPackages(p -> p
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))
			.withClasses(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))
			.withFields(f -> f
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.REQUIRED)
					.withDstDescs(FeaturePresence.REQUIRED))
			.withFileComments(true)),

	/**
	 * The {@code XSRG} ("Extended SRG") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L84">here</a>.
	 *
	 * <p>Same as SRG, but with field descriptors.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	XSRG_FILE("XSRG file", "xsrg", true, FeatureSetBuilder.createFrom(SRG_FILE.features)
			.withFields(f -> f
					.withSrcDescs(FeaturePresence.REQUIRED)
					.withDstDescs(FeaturePresence.REQUIRED))),

	/**
	 * The {@code JAM} ("Java Associated Mapping"; formerly {@code SRGX}) mapping format, as specified <a href="https://github.com/caseif/JAM">here</a>.
	 */
	JAM_FILE("JAM file", "jam", true, FeatureSetBuilder.createFrom(SRG_FILE.features)
			.withPackages(p -> p
					.withSrcNames(FeaturePresence.ABSENT)
					.withDstNames(FeaturePresence.ABSENT))
			.withFields(f -> f
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withDstDescs(FeaturePresence.ABSENT))
			.withArgs(a -> a
					.withPositions(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.OPTIONAL)
					.withDstNames(FeaturePresence.REQUIRED))),

	/**
	 * The {@code CSRG} ("Compact SRG", since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L207">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	CSRG_FILE("CSRG file", "csrg", true, FeatureSetBuilder.createFrom(SRG_FILE.features)
			.withMethods(m -> m
					.withDstDescs(FeaturePresence.ABSENT))),

	/**
	 * The {@code TSRG} ("Tiny SRG", since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L213">here</a>.
	 *
	 * <p>Same as CSRG, but hierarchical instead of flat.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	TSRG_FILE("TSRG file", "tsrg", true, FeatureSetBuilder.createFrom(CSRG_FILE.features)),

	/**
	 * The {@code TSRG v2} mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L262-L285">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings and static markers for methods are currently not supported.
	 */
	TSRG_2_FILE("TSRG v2 file", "tsrg", true, FeatureSetBuilder.createFrom(TSRG_FILE.features)
			.withNamespaces(true)
			.withElementMetadata(MetadataSupport.FIXED) // static info for methods
			.withFields(f -> f
					.withSrcDescs(FeaturePresence.OPTIONAL))
			.withArgs(a -> a
					.withLvIndices(FeaturePresence.REQUIRED)
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))),

	/**
	 * ProGuard's mapping format, as specified <a href="https://www.guardsquare.com/manual/tools/retrace">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Line numbers are currently not supported.
	 */
	PROGUARD_FILE("ProGuard file", "txt", true, FeatureSetBuilder.create()
			.withElementMetadata(MetadataSupport.FIXED) // line numbers
			.withClasses(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))
			.withFields(f -> f
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withFileComments(true)),

	/**
	 * Recaf's {@code Simple} mapping format, as specified <a href="https://github.com/Col-E/Recaf/blob/e9765d4e02991a9dd48e67c9572a063c14552e7c/src/main/java/me/coley/recaf/mapping/SimpleMappings.java#L14-L23">here</a>.
	 */
	RECAF_SIMPLE_FILE("Recaf Simple file", "txt", true, FeatureSetBuilder.create()
			.withClasses(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))
			.withFields(f -> f
					.withSrcNames(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.OPTIONAL)
					.withDstNames(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withFileComments(true)),

	/**
	 * The {@code JOBF} mapping format, as specified <a href="https://github.com/skylot/jadx/blob/2d5c0fda4a0c5d16207a5f48edb72e6efa7d5bbd/jadx-core/src/main/java/jadx/core/deobf/DeobfPresets.java">here</a>.
	 *
	 * <h2>Implementation notes</h2>
	 * Package mappings are currently not supported.
	 */
	JOBF_FILE("JOBF file", "jobf", true, FeatureSetBuilder.create()
			.withPackages(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))
			.withClasses(c -> c
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED))
			.withFields(f -> f
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(FeaturePresence.REQUIRED)
					.withDstNames(FeaturePresence.REQUIRED)
					.withSrcDescs(FeaturePresence.REQUIRED))
			.withFileComments(true));

	MappingFormat(String name, @Nullable String fileExt, boolean hasWriter, FeatureSetBuilder featureBuilder) {
		this.name = name;
		this.fileExt = fileExt;
		this.hasWriter = hasWriter;
		this.features = featureBuilder.build();
		this.hasNamespaces = features.hasNamespaces();
		this.hasFieldDescriptors = features.fields().srcDescs() != FeaturePresence.ABSENT || features.fields().dstDescs() != FeaturePresence.ABSENT;
		this.supportsComments = features.elementComments() != ElementCommentSupport.NONE;
		this.supportsArgs = features.supportsArgs();
		this.supportsLocals = features.supportsVars();
	}

	public FeatureSet features() {
		return features;
	}

	public boolean hasSingleFile() {
		return fileExt != null;
	}

	public String getGlobPattern() {
		if (fileExt == null) throw new UnsupportedOperationException("not applicable to dir based format");

		return "*."+fileExt;
	}

	private final FeatureSet features;
	public final String name;
	public final boolean hasWriter;
	@Nullable
	public final String fileExt;

	/**
	 * @deprecated Use {@link #features()} instead.
	 */
	@Deprecated
	public final boolean hasNamespaces;

	/**
	 * @deprecated Use {@link #features()} instead.
	 */
	@Deprecated
	public final boolean hasFieldDescriptors;

	/**
	 * @deprecated Use {@link #features()} instead.
	 */
	@Deprecated
	public final boolean supportsComments;

	/**
	 * @deprecated Use {@link #features()} instead.
	 */
	@Deprecated
	public final boolean supportsArgs;

	/**
	 * @deprecated Use {@link #features()} instead.
	 */
	@Deprecated
	public final boolean supportsLocals;
}

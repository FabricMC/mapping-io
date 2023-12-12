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

import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.format.MappingFormat.FeatureSet.ElementCommentSupport;
import net.fabricmc.mappingio.format.MappingFormat.FeatureSet.MetadataSupport;
import net.fabricmc.mappingio.format.MappingFormat.FeatureSet.OptionalFeature;

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
 *     <td>src</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>✔ (Currently limited support)</td>
 *   </tr>
 *   <tr>
 *     <td>Tiny v2</td>
 *     <td>✔</td>
 *     <td>src</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *   </tr>
 *   <tr>
 *     <td>Enigma</td>
 *     <td>-</td>
 *     <td>src</td>
 *     <td>✔</td>
 *     <td>✔</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>SRG</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>XSRG</td>
 *     <td>-</td>
 *     <td>src & dst</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>CSRG/TSRG</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>TSRG2</td>
 *     <td>✔</td>
 *     <td>src</td>
 *     <td>-</td>
 *     <td>✔</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>ProGuard</td>
 *     <td>-</td>
 *     <td>src</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>IntelliJ migration map</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>-</td>
 *   </tr>
 * </table>
 */
// Format order is determined by importance to Fabric tooling, format family and release order therein.
public enum MappingFormat {
	/**
	 * The {@code Tiny} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny">here</a>.
	 */
	TINY_FILE("Tiny file", "tiny", new FeatureSet()
			.withNamespaces()
			.withFileMetadata(MetadataSupport.FIXED) // TODO: change this to ARBITRARY once https://github.com/FabricMC/mapping-io/pull/29 is merged
			.withClasses(c -> c
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withFileComments()),

	/**
	 * The {@code Tiny v2} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny2">here</a>.
	 */
	TINY_2_FILE("Tiny v2 file", "tiny", new FeatureSet()
			.withNamespaces()
			.withFileMetadata(MetadataSupport.ARBITRARY)
			.withClasses(c -> c
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withArgs(a -> a
					.withLvIndices(OptionalFeature.REQUIRED)
					.withSrcNames(OptionalFeature.OPTIONAL)
					.withDstNames(OptionalFeature.OPTIONAL))
			.withVars(v -> v
					.withLvIndices(OptionalFeature.REQUIRED)
					.withLvtRowIndices(OptionalFeature.OPTIONAL)
					.withStartOpIndices(OptionalFeature.REQUIRED)
					.withSrcNames(OptionalFeature.OPTIONAL)
					.withDstNames(OptionalFeature.OPTIONAL))
			.withElementComments(ElementCommentSupport.SHARED)
			.withFileComments()), // not sure about this one

	/**
	 * Enigma's mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 */
	ENIGMA_FILE("Enigma file", "mapping", new FeatureSet()
			.withElementMetadata(MetadataSupport.FIXED) // access modifiers
			.withClasses(c -> c
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withArgs(a -> a
					.withLvIndices(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.OPTIONAL))
			.withElementComments(ElementCommentSupport.SHARED)
			.withFileComments()),

	/**
	 * Enigma's mapping format (in directory form), as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 */
	ENIGMA_DIR("Enigma directory", null, ENIGMA_FILE.features.clone()),

	/**
	 * The {@code SRG} ("Searge RetroGuard") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L81">here</a>.
	 */
	SRG_FILE("SRG file", "srg", new FeatureSet()
			.withPackages(p -> p
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED))
			.withClasses(c -> c
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED))
			.withFields(f -> f
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED)
					.withSrcDescs(OptionalFeature.REQUIRED)
					.withDstDescs(OptionalFeature.REQUIRED))
			.withFileComments()), // not sure about this one

	/**
	 * The {@code XSRG} ("Extended SRG") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L84">here</a>.
	 *
	 * <p>Same as SRG, but with field descriptors.
	 */
	XSRG_FILE("XSRG file", "xsrg", SRG_FILE.features.clone()
			.withFields(f -> f
					.withSrcDescs(OptionalFeature.REQUIRED)
					.withDstDescs(OptionalFeature.REQUIRED))),

	/**
	 * The {@code CSRG} ("Compact SRG", since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L207">here</a>.
	 */
	CSRG_FILE("CSRG file", "csrg", SRG_FILE.features.clone()
			.withMethods(m -> m
					.withDstDescs(OptionalFeature.UNSUPPORTED))),

	/**
	 * The {@code TSRG} ("Tiny SRG", since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L213">here</a>.
	 * Same as CSRG, but hierarchical instead of flat.
	 */
	TSRG_FILE("TSRG file", "tsrg", CSRG_FILE.features.clone()),

	/**
	 * The {@code TSRG v2} mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L262-L285">here</a>.
	 */
	TSRG_2_FILE("TSRG2 file", "tsrg", TSRG_FILE.features.clone()
			.withNamespaces()
			.withElementMetadata(MetadataSupport.FIXED) // static info for methods
			.withFields(f -> f
					.withSrcDescs(OptionalFeature.OPTIONAL))
			.withArgs(a -> a
					.withLvIndices(OptionalFeature.REQUIRED)
					.withSrcNames(OptionalFeature.OPTIONAL) // unsure about this one
					.withDstNames(OptionalFeature.REQUIRED))),

	/**
	 * ProGuard's mapping format, as specified <a href="https://www.guardsquare.com/manual/tools/retrace">here</a>.
	 */
	PROGUARD_FILE("ProGuard file", "txt", new FeatureSet()
			.withElementMetadata(MetadataSupport.FIXED) // line numbers
			.withClasses(c -> c
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED))
			.withFields(f -> f
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED)
					.withSrcDescs(OptionalFeature.REQUIRED))
			.withFileComments()),

	/**
	 * The IntelliJ IDEA migration map format, as implemented <a href="https://github.com/JetBrains/intellij-community/tree/5b6191dd34e05de8897f5da68757146395a260cc/java/java-impl-refactorings/src/com/intellij/refactoring/migration">here</a>.
	 *
	 * <p>Only supports packages and classes.
	 */
	INTELLIJ_MIGRATION_MAP_FILE("IntelliJ migration map file", "xml", new FeatureSet()
			.withFileMetadata(MetadataSupport.FIXED) // migration map name and description
			.withPackages(p -> p
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED))
			.withClasses(c -> c
					.withSrcNames(OptionalFeature.REQUIRED)
					.withDstNames(OptionalFeature.REQUIRED)));

	MappingFormat(String name, @Nullable String fileExt, FeatureSet features) {
		this.features = features;
		this.name = name;
		this.fileExt = fileExt;
		this.hasNamespaces = features.hasNamespaces;
		this.hasFieldDescriptors = features.fields.descriptors.srcDescriptors != OptionalFeature.UNSUPPORTED || features.fields.descriptors.dstDescriptors != OptionalFeature.UNSUPPORTED;
		this.supportsComments = features.elementComments != ElementCommentSupport.NONE;
		this.supportsArgs = features.supportsArgs();
		this.supportsLocals = features.supportsVars();
	}

	public boolean hasSingleFile() {
		return fileExt != null;
	}

	public String getGlobPattern() {
		if (fileExt == null) throw new UnsupportedOperationException("not applicable to dir based format");

		return "*."+fileExt;
	}

	@ApiStatus.Internal
	public final FeatureSet features;

	public final String name;
	@Nullable
	public final String fileExt;
	public final boolean hasNamespaces;
	public final boolean hasFieldDescriptors;
	public final boolean supportsComments;
	public final boolean supportsArgs;
	public final boolean supportsLocals;

	@ApiStatus.Internal
	public static class FeatureSet {
		public FeatureSet() {
			this(false);
		}

		public FeatureSet(boolean initWithFullSupport) {
			this(initWithFullSupport,
					initWithFullSupport ? MetadataSupport.ARBITRARY : MetadataSupport.NONE,
					initWithFullSupport ? MetadataSupport.ARBITRARY : MetadataSupport.NONE,
					new NameFeature(initWithFullSupport),
					new NameFeature(initWithFullSupport),
					new MemberSupport(initWithFullSupport),
					new MemberSupport(initWithFullSupport),
					new LocalSupport(initWithFullSupport),
					new LocalSupport(initWithFullSupport),
					initWithFullSupport ? ElementCommentSupport.NAMESPACED : ElementCommentSupport.NONE,
					initWithFullSupport);
		}

		private FeatureSet(boolean hasNamespaces, MetadataSupport fileMetadata, MetadataSupport elementMetadata, NameFeature packages, NameFeature classes, MemberSupport fields, MemberSupport methods, LocalSupport args, LocalSupport vars, ElementCommentSupport elementComments, boolean hasFileComments) {
			this.hasNamespaces = hasNamespaces;
			this.fileMetadata = fileMetadata;
			this.elementMetadata = elementMetadata;
			this.packages = packages;
			this.classes = classes;
			this.fields = fields;
			this.methods = methods;
			this.args = args;
			this.vars = vars;
			this.elementComments = elementComments;
			this.hasFileComments = hasFileComments;
		}

		public FeatureSet withNamespaces() {
			this.hasNamespaces = true;
			return this;
		}

		public FeatureSet withFileMetadata(MetadataSupport supportLevel) {
			this.fileMetadata = supportLevel;
			return this;
		}

		public FeatureSet withElementMetadata(MetadataSupport supportLevel) {
			this.elementMetadata = supportLevel;
			return this;
		}

		public FeatureSet withPackages(Consumer<NameFeature> featureApplier) {
			featureApplier.accept(packages);
			return this;
		}

		public FeatureSet withClasses(Consumer<NameFeature> featureApplier) {
			featureApplier.accept(classes);
			return this;
		}

		public FeatureSet withFields(Consumer<MemberSupport> featureApplier) {
			featureApplier.accept(fields);
			return this;
		}

		public FeatureSet withMethods(Consumer<MemberSupport> featureApplier) {
			featureApplier.accept(methods);
			return this;
		}

		public FeatureSet withArgs(Consumer<LocalSupport> featureApplier) {
			featureApplier.accept(args);
			return this;
		}

		public FeatureSet withVars(Consumer<LocalSupport> featureApplier) {
			featureApplier.accept(vars);
			return this;
		}

		public FeatureSet withElementComments(ElementCommentSupport supportLevel) {
			this.elementComments = supportLevel;
			return this;
		}

		public FeatureSet withFileComments() {
			this.hasFileComments = true;
			return this;
		}

		@Override
		public FeatureSet clone() {
			return new FeatureSet(
					hasNamespaces,
					fileMetadata,
					elementMetadata,
					packages.clone(),
					classes.clone(),
					fields.clone(),
					methods.clone(),
					args.clone(),
					vars.clone(),
					elementComments,
					hasFileComments);
		}

		public boolean hasNamespaces() {
			return hasNamespaces;
		}

		public MetadataSupport fileMetadata() {
			return fileMetadata;
		}

		public MetadataSupport elementMetadata() {
			return elementMetadata;
		}

		public NameFeature packages() {
			return packages;
		}

		public NameFeature classes() {
			return classes;
		}

		public MemberSupport fields() {
			return fields;
		}

		public MemberSupport methods() {
			return methods;
		}

		public LocalSupport args() {
			return args;
		}

		public LocalSupport vars() {
			return vars;
		}

		public ElementCommentSupport elementComments() {
			return elementComments;
		}

		public boolean hasFileComments() {
			return hasFileComments;
		}

		public boolean supportsClasses() {
			return classes.srcNames != OptionalFeature.UNSUPPORTED
					|| classes.dstNames != OptionalFeature.UNSUPPORTED;
		}

		public boolean supportsFields() {
			return supports(fields);
		}

		public boolean supportsMethods() {
			return supports(methods);
		}

		private boolean supports(MemberSupport members) {
			return members.srcNames() != OptionalFeature.UNSUPPORTED
					|| members.dstNames() != OptionalFeature.UNSUPPORTED
					|| members.srcDescs() != OptionalFeature.UNSUPPORTED
					|| members.dstDescs() != OptionalFeature.UNSUPPORTED;
		}

		public boolean supportsArgs() {
			return supports(args);
		}

		public boolean supportsVars() {
			return supports(vars);
		}

		private boolean supports(LocalSupport locals) {
			return locals.positions != OptionalFeature.UNSUPPORTED
					|| locals.lvIndices != OptionalFeature.UNSUPPORTED
					|| locals.lvtRowIndices != OptionalFeature.UNSUPPORTED
					|| locals.startOpIndices != OptionalFeature.UNSUPPORTED
					|| locals.endOpIndices != OptionalFeature.UNSUPPORTED
					|| locals.names.srcNames != OptionalFeature.UNSUPPORTED
					|| locals.names.dstNames != OptionalFeature.UNSUPPORTED
					|| locals.descriptors.srcDescriptors != OptionalFeature.UNSUPPORTED
					|| locals.descriptors.dstDescriptors != OptionalFeature.UNSUPPORTED;
		}

		private boolean hasNamespaces;
		private MetadataSupport fileMetadata;
		private MetadataSupport elementMetadata;
		private NameFeature packages;
		private NameFeature classes;
		private MemberSupport fields;
		private MemberSupport methods;
		private LocalSupport args;
		private LocalSupport vars;
		private ElementCommentSupport elementComments;
		private boolean hasFileComments;

		enum MetadataSupport {
			/** No metadata at all. */
			NONE,

			/** Only some select properties.  */
			FIXED,

			/** Arbitrary metadata may be attached. */
			ARBITRARY
		}

		interface NameOwner<T> {
			T withSrcNames(OptionalFeature supportLevel);
			T withDstNames(OptionalFeature supportLevel);
		}

		interface DescOwner<T> {
			T withSrcDescs(OptionalFeature supportLevel);
			T withDstDescs(OptionalFeature supportLevel);
		}

		public static class MemberSupport implements NameOwner<MemberSupport>, DescOwner<MemberSupport> {
			MemberSupport() {
				this(false);
			}

			MemberSupport(boolean initWithFullSupport) {
				this(new NameFeature(initWithFullSupport), new DescFeature(initWithFullSupport));
			}

			private MemberSupport(NameFeature names, DescFeature descriptors) {
				this.names = names;
				this.descriptors = descriptors;
			}

			@Override
			public MemberSupport withSrcNames(OptionalFeature srcNameFeature) {
				names.withSrcNames(srcNameFeature);
				return this;
			}

			@Override
			public MemberSupport withDstNames(OptionalFeature dstNameFeature) {
				names.withDstNames(dstNameFeature);
				return this;
			}

			@Override
			public MemberSupport withSrcDescs(OptionalFeature supportLevel) {
				descriptors.withSrcDescs(supportLevel);
				return this;
			}

			@Override
			public MemberSupport withDstDescs(OptionalFeature supportLevel) {
				descriptors.withDstDescs(supportLevel);
				return this;
			}

			@Override
			public MemberSupport clone() {
				return new MemberSupport(names.clone(), descriptors.clone());
			}

			public OptionalFeature srcNames() {
				return names.srcNames;
			}

			public OptionalFeature dstNames() {
				return names.dstNames;
			}

			public OptionalFeature srcDescs() {
				return descriptors.srcDescriptors;
			}

			public OptionalFeature dstDescs() {
				return descriptors.dstDescriptors;
			}

			private NameFeature names;
			private DescFeature descriptors;
		}

		public static class LocalSupport implements NameOwner<LocalSupport>, DescOwner<LocalSupport> {
			LocalSupport() {
				this(false);
			}

			LocalSupport(boolean initWithFullSupport) {
				this(initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED,
						initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED,
						initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED,
						initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED,
						initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED,
						new NameFeature(),
						new DescFeature());
			}

			private LocalSupport(OptionalFeature positions, OptionalFeature lvIndices, OptionalFeature lvtRowIndices, OptionalFeature startOpIndices, OptionalFeature endOpIndices, NameFeature names, DescFeature descriptors) {
				this.positions = positions;
				this.lvIndices = lvIndices;
				this.lvtRowIndices = lvtRowIndices;
				this.startOpIndices = startOpIndices;
				this.endOpIndices = endOpIndices;
				this.names = names;
				this.descriptors = descriptors;
			}

			public LocalSupport withPositionSupport(OptionalFeature positionFeature) {
				this.positions = positionFeature;
				return this;
			}

			public LocalSupport withLvIndices(OptionalFeature lvIndexFeature) {
				this.lvIndices = lvIndexFeature;
				return this;
			}

			public LocalSupport withLvtRowIndices(OptionalFeature lvtRowIndexFeature) {
				this.lvtRowIndices = lvtRowIndexFeature;
				return this;
			}

			public LocalSupport withStartOpIndices(OptionalFeature startOpIndexFeature) {
				this.startOpIndices = startOpIndexFeature;
				return this;
			}

			public LocalSupport withEndOpIndexSupport(OptionalFeature endOpIndexFeature) {
				this.endOpIndices = endOpIndexFeature;
				return this;
			}

			@Override
			public LocalSupport withSrcNames(OptionalFeature supportLevel) {
				names.withSrcNames(supportLevel);
				return this;
			}

			@Override
			public LocalSupport withDstNames(OptionalFeature supportLevel) {
				names.withDstNames(supportLevel);
				return this;
			}

			@Override
			public LocalSupport withSrcDescs(OptionalFeature supportLevel) {
				descriptors.withSrcDescs(supportLevel);
				return this;
			}

			@Override
			public LocalSupport withDstDescs(OptionalFeature supportLevel) {
				descriptors.withDstDescs(supportLevel);
				return this;
			}

			@Override
			public LocalSupport clone() {
				return new LocalSupport(
						positions,
						lvIndices,
						lvtRowIndices,
						startOpIndices,
						endOpIndices,
						names.clone(),
						descriptors.clone());
			}

			public OptionalFeature positions() {
				return positions;
			}

			public OptionalFeature lvIndices() {
				return lvIndices;
			}

			public OptionalFeature lvtRowIndices() {
				return lvtRowIndices;
			}

			public OptionalFeature startOpIndices() {
				return startOpIndices;
			}

			public OptionalFeature endOpIndices() {
				return endOpIndices;
			}

			public OptionalFeature srcNames() {
				return names.srcNames;
			}

			public OptionalFeature dstNames() {
				return names.dstNames;
			}

			public OptionalFeature srcDescs() {
				return descriptors.srcDescriptors;
			}

			public OptionalFeature dstDescs() {
				return descriptors.dstDescriptors;
			}

			private OptionalFeature positions;
			private OptionalFeature lvIndices;
			private OptionalFeature lvtRowIndices;
			private OptionalFeature startOpIndices;
			private OptionalFeature endOpIndices;
			private NameFeature names;
			private DescFeature descriptors;
		}

		public static class NameFeature {
			NameFeature() {
				this(false);
			}

			NameFeature(boolean initWithFullSupport) {
				this(initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED,
						initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED);
			}

			private NameFeature(OptionalFeature srcNames, OptionalFeature dstNames) {
				this.srcNames = srcNames;
				this.dstNames = dstNames;
			}

			public NameFeature withSrcNames(OptionalFeature srcNameFeature) {
				this.srcNames = srcNameFeature;
				return this;
			}

			public NameFeature withDstNames(OptionalFeature dstNameFeature) {
				this.dstNames = dstNameFeature;
				return this;
			}

			@Override
			public NameFeature clone() {
				return new NameFeature(srcNames, dstNames);
			}

			public OptionalFeature srcNames() {
				return srcNames;
			}

			public OptionalFeature dstNames() {
				return dstNames;
			}

			private OptionalFeature srcNames;
			private OptionalFeature dstNames;
		}

		public static class DescFeature {
			DescFeature() {
				this(false);
			}

			DescFeature(boolean initWithFullSupport) {
				this(initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED,
						initWithFullSupport ? OptionalFeature.OPTIONAL : OptionalFeature.UNSUPPORTED);
			}

			private DescFeature(OptionalFeature srcDescriptors, OptionalFeature dstDescriptors) {
				this.srcDescriptors = srcDescriptors;
				this.dstDescriptors = dstDescriptors;
			}

			public DescFeature withSrcDescs(OptionalFeature srcDescriptorFeature) {
				this.srcDescriptors = srcDescriptorFeature;
				return this;
			}

			public DescFeature withDstDescs(OptionalFeature dstDescriptorFeature) {
				this.dstDescriptors = dstDescriptorFeature;
				return this;
			}

			@Override
			public DescFeature clone() {
				return new DescFeature(srcDescriptors, dstDescriptors);
			}

			public OptionalFeature srcDescs() {
				return srcDescriptors;
			}

			public OptionalFeature dstDescs() {
				return dstDescriptors;
			}

			private OptionalFeature srcDescriptors;
			private OptionalFeature dstDescriptors;
		}

		public enum OptionalFeature {
			REQUIRED,
			OPTIONAL,
			UNSUPPORTED
		}

		public enum ElementCommentSupport {
			NAMESPACED,
			SHARED,
			NONE
		}
	}
}

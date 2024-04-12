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
import net.fabricmc.mappingio.format.MappingFormat.FeatureSet.SupportLevel;

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
 * </table>
 */
// Format order is determined by importance to Fabric tooling, format family and release order therein.
public enum MappingFormat {
	/**
	 * The {@code Tiny} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny">here</a>.
	 */
	TINY_FILE("Tiny file", "tiny", new FeatureSetImpl()
			.withNamespaces()
			.withFileMetadata(MetadataSupport.FIXED) // TODO: change this to ARBITRARY once https://github.com/FabricMC/mapping-io/pull/29 is merged
			.withClasses(c -> c
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withFileComments()),

	/**
	 * The {@code Tiny v2} mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:tiny2">here</a>.
	 */
	TINY_2_FILE("Tiny v2 file", "tiny", new FeatureSetImpl()
			.withNamespaces()
			.withFileMetadata(MetadataSupport.ARBITRARY)
			.withClasses(c -> c
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withArgs(a -> a
					.withLvIndices(SupportLevel.REQUIRED)
					.withSrcNames(SupportLevel.OPTIONAL)
					.withDstNames(SupportLevel.OPTIONAL))
			.withVars(v -> v
					.withLvIndices(SupportLevel.REQUIRED)
					.withLvtRowIndices(SupportLevel.OPTIONAL)
					.withStartOpIndices(SupportLevel.REQUIRED)
					.withSrcNames(SupportLevel.OPTIONAL)
					.withDstNames(SupportLevel.OPTIONAL))
			.withElementComments(ElementCommentSupport.SHARED)
			.withFileComments()),

	/**
	 * Enigma's mapping format, as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 */
	ENIGMA_FILE("Enigma file", "mapping", new FeatureSetImpl()
			.withElementMetadata(MetadataSupport.FIXED) // access modifiers
			.withClasses(c -> c
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL))
			.withFields(f -> f
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withArgs(a -> a
					.withLvIndices(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.OPTIONAL))
			.withElementComments(ElementCommentSupport.SHARED)
			.withFileComments()),

	/**
	 * Enigma's mapping format (in directory form), as specified <a href="https://fabricmc.net/wiki/documentation:enigma_mappings">here</a>.
	 */
	ENIGMA_DIR("Enigma directory", null, ENIGMA_FILE.features.clone()),

	/**
	 * The {@code SRG} ("Searge RetroGuard") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L81">here</a>.
	 */
	SRG_FILE("SRG file", "srg", new FeatureSetImpl()
			.withPackages(p -> p
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED))
			.withClasses(c -> c
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED))
			.withFields(f -> f
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED)
					.withSrcDescs(SupportLevel.REQUIRED)
					.withDstDescs(SupportLevel.REQUIRED))
			.withFileComments()),

	/**
	 * The {@code XSRG} ("Extended SRG") mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L69-L84">here</a>.
	 *
	 * <p>Same as SRG, but with field descriptors.
	 */
	XSRG_FILE("XSRG file", "xsrg", SRG_FILE.features.clone()
			.withFields(f -> f
					.withSrcDescs(SupportLevel.REQUIRED)
					.withDstDescs(SupportLevel.REQUIRED))),

	/**
	 * The {@code CSRG} ("Compact SRG", since it saves disk space over SRG) mapping format, as specified <a href="https://github.com/MinecraftForge/SrgUtils/blob/67f30647ece29f18256ca89a23cda6216d6bd21e/src/main/java/net/minecraftforge/srgutils/InternalUtils.java#L196-L207">here</a>.
	 */
	CSRG_FILE("CSRG file", "csrg", SRG_FILE.features.clone()
			.withMethods(m -> m
					.withDstDescs(SupportLevel.UNSUPPORTED))),

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
					.withSrcDescs(SupportLevel.OPTIONAL))
			.withArgs(a -> a
					.withLvIndices(SupportLevel.REQUIRED)
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED))),

	/**
	 * ProGuard's mapping format, as specified <a href="https://www.guardsquare.com/manual/tools/retrace">here</a>.
	 */
	PROGUARD_FILE("ProGuard file", "txt", new FeatureSetImpl()
			.withElementMetadata(MetadataSupport.FIXED) // line numbers
			.withClasses(c -> c
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED))
			.withFields(f -> f
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withMethods(m -> m
					.withSrcNames(SupportLevel.REQUIRED)
					.withDstNames(SupportLevel.REQUIRED)
					.withSrcDescs(SupportLevel.REQUIRED))
			.withFileComments());

	MappingFormat(String name, @Nullable String fileExt, FeatureSetImpl features) {
		this.features = features;
		this.name = name;
		this.fileExt = fileExt;
		this.hasNamespaces = features.hasNamespaces;
		this.hasFieldDescriptors = features.fields.descriptors.srcDescriptors != SupportLevel.UNSUPPORTED || features.fields.descriptors.dstDescriptors != SupportLevel.UNSUPPORTED;
		this.supportsComments = features.elementComments != ElementCommentSupport.NONE;
		this.supportsArgs = features.supportsArgs();
		this.supportsLocals = features.supportsVars();
	}

	public FeatureSet getFeatures() {
		return features;
	}

	public boolean hasSingleFile() {
		return fileExt != null;
	}

	public String getGlobPattern() {
		if (fileExt == null) throw new UnsupportedOperationException("not applicable to dir based format");

		return "*."+fileExt;
	}

	private final FeatureSetImpl features;
	public final String name;
	@Nullable
	public final String fileExt;

	/**
	 * @deprecated Use {@link #getFeatures()} instead.
	 */
	@Deprecated
	public final boolean hasNamespaces;

	/**
	 * @deprecated Use {@link #getFeatures()} instead.
	 */
	@Deprecated
	public final boolean hasFieldDescriptors;

	/**
	 * @deprecated Use {@link #getFeatures()} instead.
	 */
	@Deprecated
	public final boolean supportsComments;

	/**
	 * @deprecated Use {@link #getFeatures()} instead.
	 */
	@Deprecated
	public final boolean supportsArgs;

	/**
	 * @deprecated Use {@link #getFeatures()} instead.
	 */
	@Deprecated
	public final boolean supportsLocals;

	@ApiStatus.Internal
	public static final class FeatureSetImpl implements FeatureSet {
		public FeatureSetImpl() {
			this(false);
		}

		public FeatureSetImpl(boolean initWithFullSupport) {
			this(initWithFullSupport,
					initWithFullSupport ? MetadataSupport.ARBITRARY : MetadataSupport.NONE,
					initWithFullSupport ? MetadataSupport.ARBITRARY : MetadataSupport.NONE,
					new NameFeatureImpl(initWithFullSupport),
					new NameFeatureImpl(initWithFullSupport),
					new MemberSupportImpl(initWithFullSupport),
					new MemberSupportImpl(initWithFullSupport),
					new LocalSupportImpl(initWithFullSupport),
					new LocalSupportImpl(initWithFullSupport),
					initWithFullSupport ? ElementCommentSupport.NAMESPACED : ElementCommentSupport.NONE,
					initWithFullSupport);
		}

		private FeatureSetImpl(boolean hasNamespaces, MetadataSupport fileMetadata, MetadataSupport elementMetadata, NameFeatureImpl packages, NameFeatureImpl classes, MemberSupportImpl fields, MemberSupportImpl methods, LocalSupportImpl args, LocalSupportImpl vars, ElementCommentSupport elementComments, boolean hasFileComments) {
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

		private FeatureSetImpl withNamespaces() {
			this.hasNamespaces = true;
			return this;
		}

		private FeatureSetImpl withFileMetadata(MetadataSupport supportLevel) {
			this.fileMetadata = supportLevel;
			return this;
		}

		private FeatureSetImpl withElementMetadata(MetadataSupport supportLevel) {
			this.elementMetadata = supportLevel;
			return this;
		}

		private FeatureSetImpl withPackages(Consumer<NameFeatureImpl> featureApplier) {
			featureApplier.accept(packages);
			return this;
		}

		private FeatureSetImpl withClasses(Consumer<NameFeatureImpl> featureApplier) {
			featureApplier.accept(classes);
			return this;
		}

		private FeatureSetImpl withFields(Consumer<MemberSupportImpl> featureApplier) {
			featureApplier.accept(fields);
			return this;
		}

		private FeatureSetImpl withMethods(Consumer<MemberSupportImpl> featureApplier) {
			featureApplier.accept(methods);
			return this;
		}

		private FeatureSetImpl withArgs(Consumer<LocalSupportImpl> featureApplier) {
			featureApplier.accept(args);
			return this;
		}

		private FeatureSetImpl withVars(Consumer<LocalSupportImpl> featureApplier) {
			featureApplier.accept(vars);
			return this;
		}

		private FeatureSetImpl withElementComments(ElementCommentSupport supportLevel) {
			this.elementComments = supportLevel;
			return this;
		}

		private FeatureSetImpl withFileComments() {
			this.hasFileComments = true;
			return this;
		}

		@Override
		protected FeatureSetImpl clone() {
			return new FeatureSetImpl(
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

		@Override
		public boolean hasNamespaces() {
			return hasNamespaces;
		}

		@Override
		public MetadataSupport fileMetadata() {
			return fileMetadata;
		}

		@Override
		public MetadataSupport elementMetadata() {
			return elementMetadata;
		}

		@Override
		public NameFeatureImpl packages() {
			return packages;
		}

		@Override
		public NameFeatureImpl classes() {
			return classes;
		}

		@Override
		public MemberSupportImpl fields() {
			return fields;
		}

		@Override
		public MemberSupportImpl methods() {
			return methods;
		}

		@Override
		public LocalSupportImpl args() {
			return args;
		}

		@Override
		public LocalSupportImpl vars() {
			return vars;
		}

		@Override
		public ElementCommentSupport elementComments() {
			return elementComments;
		}

		@Override
		public boolean hasFileComments() {
			return hasFileComments;
		}

		private boolean hasNamespaces;
		private MetadataSupport fileMetadata;
		private MetadataSupport elementMetadata;
		private NameFeatureImpl packages;
		private NameFeatureImpl classes;
		private MemberSupportImpl fields;
		private MemberSupportImpl methods;
		private LocalSupportImpl args;
		private LocalSupportImpl vars;
		private ElementCommentSupport elementComments;
		private boolean hasFileComments;

		public static class MemberSupportImpl implements MemberSupport {
			MemberSupportImpl() {
				this(false);
			}

			MemberSupportImpl(boolean initWithFullSupport) {
				this(new NameFeatureImpl(initWithFullSupport), new DescFeatureImpl(initWithFullSupport));
			}

			private MemberSupportImpl(NameFeatureImpl names, DescFeatureImpl descriptors) {
				this.names = names;
				this.descriptors = descriptors;
			}

			public MemberSupportImpl withSrcNames(SupportLevel srcNameFeature) {
				names.withSrcNames(srcNameFeature);
				return this;
			}

			public MemberSupportImpl withDstNames(SupportLevel dstNameFeature) {
				names.withDstNames(dstNameFeature);
				return this;
			}

			public MemberSupportImpl withSrcDescs(SupportLevel supportLevel) {
				descriptors.withSrcDescs(supportLevel);
				return this;
			}

			public MemberSupportImpl withDstDescs(SupportLevel supportLevel) {
				descriptors.withDstDescs(supportLevel);
				return this;
			}

			@Override
			public MemberSupportImpl clone() {
				return new MemberSupportImpl(names.clone(), descriptors.clone());
			}

			@Override
			public SupportLevel srcNames() {
				return names.srcNames;
			}

			@Override
			public SupportLevel dstNames() {
				return names.dstNames;
			}

			@Override
			public SupportLevel srcDescs() {
				return descriptors.srcDescriptors;
			}

			@Override
			public SupportLevel dstDescs() {
				return descriptors.dstDescriptors;
			}

			private NameFeatureImpl names;
			private DescFeatureImpl descriptors;
		}

		public static class LocalSupportImpl implements LocalSupport {
			LocalSupportImpl() {
				this(false);
			}

			LocalSupportImpl(boolean initWithFullSupport) {
				this(initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
						initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
						initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
						initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
						initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
						new NameFeatureImpl(),
						new DescFeatureImpl());
			}

			private LocalSupportImpl(SupportLevel positions, SupportLevel lvIndices, SupportLevel lvtRowIndices, SupportLevel startOpIndices, SupportLevel endOpIndices, NameFeatureImpl names, DescFeatureImpl descriptors) {
				this.positions = positions;
				this.lvIndices = lvIndices;
				this.lvtRowIndices = lvtRowIndices;
				this.startOpIndices = startOpIndices;
				this.endOpIndices = endOpIndices;
				this.names = names;
				this.descriptors = descriptors;
			}

			public LocalSupportImpl withPositionSupport(SupportLevel positionFeature) {
				this.positions = positionFeature;
				return this;
			}

			public LocalSupportImpl withLvIndices(SupportLevel lvIndexFeature) {
				this.lvIndices = lvIndexFeature;
				return this;
			}

			public LocalSupportImpl withLvtRowIndices(SupportLevel lvtRowIndexFeature) {
				this.lvtRowIndices = lvtRowIndexFeature;
				return this;
			}

			public LocalSupportImpl withStartOpIndices(SupportLevel startOpIndexFeature) {
				this.startOpIndices = startOpIndexFeature;
				return this;
			}

			public LocalSupportImpl withEndOpIndexSupport(SupportLevel endOpIndexFeature) {
				this.endOpIndices = endOpIndexFeature;
				return this;
			}

			public LocalSupportImpl withSrcNames(SupportLevel supportLevel) {
				names.withSrcNames(supportLevel);
				return this;
			}

			public LocalSupportImpl withDstNames(SupportLevel supportLevel) {
				names.withDstNames(supportLevel);
				return this;
			}

			public LocalSupportImpl withSrcDescs(SupportLevel supportLevel) {
				descriptors.withSrcDescs(supportLevel);
				return this;
			}

			public LocalSupportImpl withDstDescs(SupportLevel supportLevel) {
				descriptors.withDstDescs(supportLevel);
				return this;
			}

			@Override
			public LocalSupportImpl clone() {
				return new LocalSupportImpl(
						positions,
						lvIndices,
						lvtRowIndices,
						startOpIndices,
						endOpIndices,
						names.clone(),
						descriptors.clone());
			}

			@Override
			public SupportLevel positions() {
				return positions;
			}

			@Override
			public SupportLevel lvIndices() {
				return lvIndices;
			}

			@Override
			public SupportLevel lvtRowIndices() {
				return lvtRowIndices;
			}

			@Override
			public SupportLevel startOpIndices() {
				return startOpIndices;
			}

			@Override
			public SupportLevel endOpIndices() {
				return endOpIndices;
			}

			@Override
			public SupportLevel srcNames() {
				return names.srcNames;
			}

			@Override
			public SupportLevel dstNames() {
				return names.dstNames;
			}

			@Override
			public SupportLevel srcDescs() {
				return descriptors.srcDescriptors;
			}

			@Override
			public SupportLevel dstDescs() {
				return descriptors.dstDescriptors;
			}

			private SupportLevel positions;
			private SupportLevel lvIndices;
			private SupportLevel lvtRowIndices;
			private SupportLevel startOpIndices;
			private SupportLevel endOpIndices;
			private NameFeatureImpl names;
			private DescFeatureImpl descriptors;
		}

		public static class NameFeatureImpl implements NameFeature {
			NameFeatureImpl() {
				this(false);
			}

			NameFeatureImpl(boolean initWithFullSupport) {
				this(initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
						initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED);
			}

			private NameFeatureImpl(SupportLevel srcNames, SupportLevel dstNames) {
				this.srcNames = srcNames;
				this.dstNames = dstNames;
			}

			public NameFeatureImpl withSrcNames(SupportLevel srcNameFeature) {
				this.srcNames = srcNameFeature;
				return this;
			}

			public NameFeatureImpl withDstNames(SupportLevel dstNameFeature) {
				this.dstNames = dstNameFeature;
				return this;
			}

			@Override
			public NameFeatureImpl clone() {
				return new NameFeatureImpl(srcNames, dstNames);
			}

			@Override
			public SupportLevel srcNames() {
				return srcNames;
			}

			@Override
			public SupportLevel dstNames() {
				return dstNames;
			}

			private SupportLevel srcNames;
			private SupportLevel dstNames;
		}

		public static class DescFeatureImpl implements DescFeature {
			DescFeatureImpl() {
				this(false);
			}

			DescFeatureImpl(boolean initWithFullSupport) {
				this(initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
						initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED);
			}

			private DescFeatureImpl(SupportLevel srcDescriptors, SupportLevel dstDescriptors) {
				this.srcDescriptors = srcDescriptors;
				this.dstDescriptors = dstDescriptors;
			}

			public DescFeatureImpl withSrcDescs(SupportLevel srcDescriptorFeature) {
				this.srcDescriptors = srcDescriptorFeature;
				return this;
			}

			public DescFeatureImpl withDstDescs(SupportLevel dstDescriptorFeature) {
				this.dstDescriptors = dstDescriptorFeature;
				return this;
			}

			@Override
			public DescFeatureImpl clone() {
				return new DescFeatureImpl(srcDescriptors, dstDescriptors);
			}

			@Override
			public SupportLevel srcDescs() {
				return srcDescriptors;
			}

			@Override
			public SupportLevel dstDescs() {
				return dstDescriptors;
			}

			private SupportLevel srcDescriptors;
			private SupportLevel dstDescriptors;
		}
	}

	public interface FeatureSet {
		boolean hasNamespaces();
		MetadataSupport fileMetadata();
		MetadataSupport elementMetadata();
		NameFeature packages();
		NameFeature classes();
		MemberSupport fields();
		MemberSupport methods();
		LocalSupport args();
		LocalSupport vars();
		ElementCommentSupport elementComments();
		boolean hasFileComments();

		default boolean supportsClasses() {
			return classes().srcNames() != SupportLevel.UNSUPPORTED
					|| classes().dstNames() != SupportLevel.UNSUPPORTED;
		}

		default boolean supportsFields() {
			return supports(fields());
		}

		default boolean supportsMethods() {
			return supports(methods());
		}

		/* TODO: Make private in Java 9+ */
		@ApiStatus.Internal
		default boolean supports(MemberSupport members) {
			return members.srcNames() != SupportLevel.UNSUPPORTED
					|| members.dstNames() != SupportLevel.UNSUPPORTED
					|| members.srcDescs() != SupportLevel.UNSUPPORTED
					|| members.dstDescs() != SupportLevel.UNSUPPORTED;
		}

		default boolean supportsArgs() {
			return supports(args());
		}

		default boolean supportsVars() {
			return supports(vars());
		}

		/* TODO: Make private in Java 9+ */
		@ApiStatus.Internal
		default boolean supports(LocalSupport locals) {
			return locals.positions() != SupportLevel.UNSUPPORTED
					|| locals.lvIndices() != SupportLevel.UNSUPPORTED
					|| locals.lvtRowIndices() != SupportLevel.UNSUPPORTED
					|| locals.startOpIndices() != SupportLevel.UNSUPPORTED
					|| locals.endOpIndices() != SupportLevel.UNSUPPORTED
					|| locals.srcNames() != SupportLevel.UNSUPPORTED
					|| locals.dstNames() != SupportLevel.UNSUPPORTED
					|| locals.srcDescs() != SupportLevel.UNSUPPORTED
					|| locals.dstDescs() != SupportLevel.UNSUPPORTED;
		}

		enum MetadataSupport {
			/** No metadata at all. */
			NONE,

			/** Only some select properties.  */
			FIXED,

			/** Arbitrary metadata may be attached. */
			ARBITRARY
		}

		enum SupportLevel {
			REQUIRED,
			OPTIONAL,
			UNSUPPORTED
		}

		interface NameFeature {
			SupportLevel srcNames();
			SupportLevel dstNames();
		}

		interface NameHolder<T> {
			SupportLevel srcNames();
			SupportLevel dstNames();
		}

		interface DescHolder<T> {
			SupportLevel srcDescs();
			SupportLevel dstDescs();
		}

		interface MemberSupport extends NameHolder<MemberSupport>, DescHolder<MemberSupport> {
		}

		interface DescFeature {
			SupportLevel srcDescs();
			SupportLevel dstDescs();
		}

		interface LocalSupport extends NameHolder<LocalSupport>, DescHolder<LocalSupport> {
			SupportLevel positions();
			SupportLevel lvIndices();
			SupportLevel lvtRowIndices();
			SupportLevel startOpIndices();
			SupportLevel endOpIndices();
		}

		enum ElementCommentSupport {
			NAMESPACED,
			SHARED,
			NONE
		}
	}
}

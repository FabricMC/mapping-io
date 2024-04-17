/*
 * Copyright (c) 2024 FabricMC
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

import net.fabricmc.mappingio.format.FeatureSet.DescSupport;
import net.fabricmc.mappingio.format.FeatureSet.ElementCommentSupport;
import net.fabricmc.mappingio.format.FeatureSet.LocalSupport;
import net.fabricmc.mappingio.format.FeatureSet.MemberSupport;
import net.fabricmc.mappingio.format.FeatureSet.MetadataSupport;
import net.fabricmc.mappingio.format.FeatureSet.NameSupport;
import net.fabricmc.mappingio.format.FeatureSet.SupportLevel;
import net.fabricmc.mappingio.format.FeatureSetImpl.DescSupportImpl;
import net.fabricmc.mappingio.format.FeatureSetImpl.LocalSupportImpl;
import net.fabricmc.mappingio.format.FeatureSetImpl.MemberSupportImpl;
import net.fabricmc.mappingio.format.FeatureSetImpl.NameSupportImpl;

@ApiStatus.Experimental
public class FeatureSetBuilder {
	public static FeatureSetBuilder create() {
		return new FeatureSetBuilder(false);
	}

	public static FeatureSetBuilder createFrom(FeatureSet featureSet) {
		return new FeatureSetBuilder(
				featureSet.hasNamespaces(),
				featureSet.fileMetadata(),
				featureSet.elementMetadata(),
				new NameFeatureBuilder(featureSet.packages()),
				new NameFeatureBuilder(featureSet.classes()),
				new MemberSupportBuilder(featureSet.fields()),
				new MemberSupportBuilder(featureSet.methods()),
				new LocalSupportBuilder(featureSet.args()),
				new LocalSupportBuilder(featureSet.vars()),
				featureSet.elementComments(),
				featureSet.hasFileComments());
	}

	FeatureSetBuilder(boolean initWithFullSupport) {
		this(initWithFullSupport,
				initWithFullSupport ? MetadataSupport.ARBITRARY : MetadataSupport.NONE,
				initWithFullSupport ? MetadataSupport.ARBITRARY : MetadataSupport.NONE,
				new NameFeatureBuilder(initWithFullSupport),
				new NameFeatureBuilder(initWithFullSupport),
				new MemberSupportBuilder(initWithFullSupport),
				new MemberSupportBuilder(initWithFullSupport),
				new LocalSupportBuilder(initWithFullSupport),
				new LocalSupportBuilder(initWithFullSupport),
				initWithFullSupport ? ElementCommentSupport.NAMESPACED : ElementCommentSupport.NONE,
				initWithFullSupport);
	}

	FeatureSetBuilder(boolean hasNamespaces, MetadataSupport fileMetadata, MetadataSupport elementMetadata, NameFeatureBuilder packages, NameFeatureBuilder classes, MemberSupportBuilder fields, MemberSupportBuilder methods, LocalSupportBuilder args, LocalSupportBuilder vars, ElementCommentSupport elementComments, boolean hasFileComments) {
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

	public FeatureSetBuilder withNamespaces() {
		this.hasNamespaces = true;
		return this;
	}

	public FeatureSetBuilder withFileMetadata(MetadataSupport supportLevel) {
		this.fileMetadata = supportLevel;
		return this;
	}

	public FeatureSetBuilder withElementMetadata(MetadataSupport supportLevel) {
		this.elementMetadata = supportLevel;
		return this;
	}

	public FeatureSetBuilder withPackages(Consumer<NameFeatureBuilder> featureApplier) {
		featureApplier.accept(packages);
		return this;
	}

	public FeatureSetBuilder withClasses(Consumer<NameFeatureBuilder> featureApplier) {
		featureApplier.accept(classes);
		return this;
	}

	public FeatureSetBuilder withFields(Consumer<MemberSupportBuilder> featureApplier) {
		featureApplier.accept(fields);
		return this;
	}

	public FeatureSetBuilder withMethods(Consumer<MemberSupportBuilder> featureApplier) {
		featureApplier.accept(methods);
		return this;
	}

	public FeatureSetBuilder withArgs(Consumer<LocalSupportBuilder> featureApplier) {
		featureApplier.accept(args);
		return this;
	}

	public FeatureSetBuilder withVars(Consumer<LocalSupportBuilder> featureApplier) {
		featureApplier.accept(vars);
		return this;
	}

	public FeatureSetBuilder withElementComments(ElementCommentSupport supportLevel) {
		this.elementComments = supportLevel;
		return this;
	}

	public FeatureSetBuilder withFileComments() {
		this.hasFileComments = true;
		return this;
	}

	public FeatureSet build() {
		return new FeatureSetImpl(
				hasNamespaces,
				fileMetadata,
				elementMetadata,
				packages.build(),
				classes.build(),
				fields.build(),
				methods.build(),
				args.build(),
				vars.build(),
				elementComments,
				hasFileComments);
	}

	private boolean hasNamespaces;
	private MetadataSupport fileMetadata;
	private MetadataSupport elementMetadata;
	private NameFeatureBuilder packages;
	private NameFeatureBuilder classes;
	private MemberSupportBuilder fields;
	private MemberSupportBuilder methods;
	private LocalSupportBuilder args;
	private LocalSupportBuilder vars;
	private ElementCommentSupport elementComments;
	private boolean hasFileComments;

	static class MemberSupportBuilder {
		MemberSupportBuilder() {
			this(false);
		}

		MemberSupportBuilder(boolean initWithFullSupport) {
			this(new NameFeatureBuilder(initWithFullSupport), new DescFeatureBuilder(initWithFullSupport));
		}

		MemberSupportBuilder(MemberSupport memberSupport) {
			this(new NameFeatureBuilder(memberSupport), new DescFeatureBuilder(memberSupport));
		}

		private MemberSupportBuilder(NameFeatureBuilder names, DescFeatureBuilder descriptors) {
			this.names = names;
			this.descriptors = descriptors;
		}

		public MemberSupportBuilder withSrcNames(SupportLevel srcNameFeature) {
			names.withSrcNames(srcNameFeature);
			return this;
		}

		public MemberSupportBuilder withDstNames(SupportLevel dstNameFeature) {
			names.withDstNames(dstNameFeature);
			return this;
		}

		public MemberSupportBuilder withSrcDescs(SupportLevel supportLevel) {
			descriptors.withSrcDescs(supportLevel);
			return this;
		}

		public MemberSupportBuilder withDstDescs(SupportLevel supportLevel) {
			descriptors.withDstDescs(supportLevel);
			return this;
		}

		public MemberSupport build() {
			return new MemberSupportImpl(names.build(), descriptors.build());
		}

		private NameFeatureBuilder names;
		private DescFeatureBuilder descriptors;
	}

	static class LocalSupportBuilder {
		LocalSupportBuilder() {
			this(false);
		}

		LocalSupportBuilder(boolean initWithFullSupport) {
			this(initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
					initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
					initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
					initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
					initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
					new NameFeatureBuilder(),
					new DescFeatureBuilder());
		}

		LocalSupportBuilder(LocalSupport localSupport) {
			this(localSupport.positions(),
					localSupport.lvIndices(),
					localSupport.lvtRowIndices(),
					localSupport.startOpIndices(),
					localSupport.endOpIndices(),
					new NameFeatureBuilder(localSupport),
					new DescFeatureBuilder(localSupport));
		}

		private LocalSupportBuilder(SupportLevel positions, SupportLevel lvIndices, SupportLevel lvtRowIndices, SupportLevel startOpIndices, SupportLevel endOpIndices, NameFeatureBuilder names, DescFeatureBuilder descriptors) {
			this.positions = positions;
			this.lvIndices = lvIndices;
			this.lvtRowIndices = lvtRowIndices;
			this.startOpIndices = startOpIndices;
			this.endOpIndices = endOpIndices;
			this.names = names;
			this.descriptors = descriptors;
		}

		public LocalSupportBuilder withPositions(SupportLevel positionFeature) {
			this.positions = positionFeature;
			return this;
		}

		public LocalSupportBuilder withLvIndices(SupportLevel lvIndexFeature) {
			this.lvIndices = lvIndexFeature;
			return this;
		}

		public LocalSupportBuilder withLvtRowIndices(SupportLevel lvtRowIndexFeature) {
			this.lvtRowIndices = lvtRowIndexFeature;
			return this;
		}

		public LocalSupportBuilder withStartOpIndices(SupportLevel startOpIndexFeature) {
			this.startOpIndices = startOpIndexFeature;
			return this;
		}

		public LocalSupportBuilder withEndOpIndexSupport(SupportLevel endOpIndexFeature) {
			this.endOpIndices = endOpIndexFeature;
			return this;
		}

		public LocalSupportBuilder withSrcNames(SupportLevel supportLevel) {
			names.withSrcNames(supportLevel);
			return this;
		}

		public LocalSupportBuilder withDstNames(SupportLevel supportLevel) {
			names.withDstNames(supportLevel);
			return this;
		}

		public LocalSupportBuilder withSrcDescs(SupportLevel supportLevel) {
			descriptors.withSrcDescs(supportLevel);
			return this;
		}

		public LocalSupportBuilder withDstDescs(SupportLevel supportLevel) {
			descriptors.withDstDescs(supportLevel);
			return this;
		}

		public LocalSupport build() {
			return new LocalSupportImpl(
					positions,
					lvIndices,
					lvtRowIndices,
					startOpIndices,
					endOpIndices,
					names.build(),
					descriptors.build());
		}

		private SupportLevel positions;
		private SupportLevel lvIndices;
		private SupportLevel lvtRowIndices;
		private SupportLevel startOpIndices;
		private SupportLevel endOpIndices;
		private NameFeatureBuilder names;
		private DescFeatureBuilder descriptors;
	}

	static class NameFeatureBuilder {
		NameFeatureBuilder() {
			this(false);
		}

		NameFeatureBuilder(boolean initWithFullSupport) {
			this(initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
					initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED);
		}

		private NameFeatureBuilder(NameSupport nameFeature) {
			this(nameFeature.srcNames(), nameFeature.dstNames());
		}

		private NameFeatureBuilder(SupportLevel srcNames, SupportLevel dstNames) {
			this.srcNames = srcNames;
			this.dstNames = dstNames;
		}

		public NameFeatureBuilder withSrcNames(SupportLevel srcNameFeature) {
			this.srcNames = srcNameFeature;
			return this;
		}

		public NameFeatureBuilder withDstNames(SupportLevel dstNameFeature) {
			this.dstNames = dstNameFeature;
			return this;
		}

		public NameSupport build() {
			return new NameSupportImpl(srcNames, dstNames);
		}

		private SupportLevel srcNames;
		private SupportLevel dstNames;
	}

	static class DescFeatureBuilder {
		DescFeatureBuilder() {
			this(false);
		}

		DescFeatureBuilder(boolean initWithFullSupport) {
			this(initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED,
					initWithFullSupport ? SupportLevel.OPTIONAL : SupportLevel.UNSUPPORTED);
		}

		private DescFeatureBuilder(DescSupport descFeature) {
			this(descFeature.srcDescs(), descFeature.dstDescs());
		}

		private DescFeatureBuilder(SupportLevel srcDescriptors, SupportLevel dstDescriptors) {
			this.srcDescriptors = srcDescriptors;
			this.dstDescriptors = dstDescriptors;
		}

		public DescFeatureBuilder withSrcDescs(SupportLevel srcDescriptorFeature) {
			this.srcDescriptors = srcDescriptorFeature;
			return this;
		}

		public DescFeatureBuilder withDstDescs(SupportLevel dstDescriptorFeature) {
			this.dstDescriptors = dstDescriptorFeature;
			return this;
		}

		@Override
		protected DescFeatureBuilder clone() {
			return new DescFeatureBuilder(srcDescriptors, dstDescriptors);
		}

		public DescSupport build() {
			return new DescSupportImpl(srcDescriptors, dstDescriptors);
		}

		private SupportLevel srcDescriptors;
		private SupportLevel dstDescriptors;
	}
}

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
import net.fabricmc.mappingio.format.FeatureSet.FeaturePresence;
import net.fabricmc.mappingio.format.FeatureSet.LocalSupport;
import net.fabricmc.mappingio.format.FeatureSet.MemberSupport;
import net.fabricmc.mappingio.format.FeatureSet.MetadataSupport;
import net.fabricmc.mappingio.format.FeatureSet.NameSupport;
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

	public FeatureSetBuilder withNamespaces(boolean value) {
		this.hasNamespaces = value;
		return this;
	}

	public FeatureSetBuilder withFileMetadata(MetadataSupport featurePresence) {
		this.fileMetadata = featurePresence;
		return this;
	}

	public FeatureSetBuilder withElementMetadata(MetadataSupport featurePresence) {
		this.elementMetadata = featurePresence;
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

	public FeatureSetBuilder withElementComments(ElementCommentSupport featurePresence) {
		this.elementComments = featurePresence;
		return this;
	}

	public FeatureSetBuilder withFileComments(boolean value) {
		this.hasFileComments = value;
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

	public static class MemberSupportBuilder {
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

		public MemberSupportBuilder withSrcNames(FeaturePresence featurePresence) {
			names.withSrcNames(featurePresence);
			return this;
		}

		public MemberSupportBuilder withDstNames(FeaturePresence featurePresence) {
			names.withDstNames(featurePresence);
			return this;
		}

		public MemberSupportBuilder withSrcDescs(FeaturePresence featurePresence) {
			descriptors.withSrcDescs(featurePresence);
			return this;
		}

		public MemberSupportBuilder withDstDescs(FeaturePresence featurePresence) {
			descriptors.withDstDescs(featurePresence);
			return this;
		}

		public MemberSupport build() {
			return new MemberSupportImpl(names.build(), descriptors.build());
		}

		private NameFeatureBuilder names;
		private DescFeatureBuilder descriptors;
	}

	public static class LocalSupportBuilder {
		LocalSupportBuilder() {
			this(false);
		}

		LocalSupportBuilder(boolean initWithFullSupport) {
			this(initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT,
					initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT,
					initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT,
					initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT,
					initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT,
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

		private LocalSupportBuilder(FeaturePresence positions, FeaturePresence lvIndices, FeaturePresence lvtRowIndices, FeaturePresence startOpIndices, FeaturePresence endOpIndices, NameFeatureBuilder names, DescFeatureBuilder descriptors) {
			this.positions = positions;
			this.lvIndices = lvIndices;
			this.lvtRowIndices = lvtRowIndices;
			this.startOpIndices = startOpIndices;
			this.endOpIndices = endOpIndices;
			this.names = names;
			this.descriptors = descriptors;
		}

		public LocalSupportBuilder withPositions(FeaturePresence featurePresence) {
			this.positions = featurePresence;
			return this;
		}

		public LocalSupportBuilder withLvIndices(FeaturePresence featurePresence) {
			this.lvIndices = featurePresence;
			return this;
		}

		public LocalSupportBuilder withLvtRowIndices(FeaturePresence featurePresence) {
			this.lvtRowIndices = featurePresence;
			return this;
		}

		public LocalSupportBuilder withStartOpIndices(FeaturePresence featurePresence) {
			this.startOpIndices = featurePresence;
			return this;
		}

		public LocalSupportBuilder withEndOpIndices(FeaturePresence featurePresence) {
			this.endOpIndices = featurePresence;
			return this;
		}

		public LocalSupportBuilder withSrcNames(FeaturePresence featurePresence) {
			names.withSrcNames(featurePresence);
			return this;
		}

		public LocalSupportBuilder withDstNames(FeaturePresence featurePresence) {
			names.withDstNames(featurePresence);
			return this;
		}

		public LocalSupportBuilder withSrcDescs(FeaturePresence featurePresence) {
			descriptors.withSrcDescs(featurePresence);
			return this;
		}

		public LocalSupportBuilder withDstDescs(FeaturePresence featurePresence) {
			descriptors.withDstDescs(featurePresence);
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

		private FeaturePresence positions;
		private FeaturePresence lvIndices;
		private FeaturePresence lvtRowIndices;
		private FeaturePresence startOpIndices;
		private FeaturePresence endOpIndices;
		private NameFeatureBuilder names;
		private DescFeatureBuilder descriptors;
	}

	public static class NameFeatureBuilder {
		NameFeatureBuilder() {
			this(false);
		}

		NameFeatureBuilder(boolean initWithFullSupport) {
			this(initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT,
					initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT);
		}

		private NameFeatureBuilder(NameSupport nameFeature) {
			this(nameFeature.srcNames(), nameFeature.dstNames());
		}

		private NameFeatureBuilder(FeaturePresence srcNames, FeaturePresence dstNames) {
			this.srcNames = srcNames;
			this.dstNames = dstNames;
		}

		public NameFeatureBuilder withSrcNames(FeaturePresence featurePresence) {
			this.srcNames = featurePresence;
			return this;
		}

		public NameFeatureBuilder withDstNames(FeaturePresence featurePresence) {
			this.dstNames = featurePresence;
			return this;
		}

		public NameSupport build() {
			return new NameSupportImpl(srcNames, dstNames);
		}

		private FeaturePresence srcNames;
		private FeaturePresence dstNames;
	}

	public static class DescFeatureBuilder {
		DescFeatureBuilder() {
			this(false);
		}

		DescFeatureBuilder(boolean initWithFullSupport) {
			this(initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT,
					initWithFullSupport ? FeaturePresence.OPTIONAL : FeaturePresence.ABSENT);
		}

		private DescFeatureBuilder(DescSupport descFeature) {
			this(descFeature.srcDescs(), descFeature.dstDescs());
		}

		private DescFeatureBuilder(FeaturePresence srcDescriptors, FeaturePresence dstDescriptors) {
			this.srcDescriptors = srcDescriptors;
			this.dstDescriptors = dstDescriptors;
		}

		public DescFeatureBuilder withSrcDescs(FeaturePresence featurePresence) {
			this.srcDescriptors = featurePresence;
			return this;
		}

		public DescFeatureBuilder withDstDescs(FeaturePresence featurePresence) {
			this.dstDescriptors = featurePresence;
			return this;
		}

		public DescSupport build() {
			return new DescSupportImpl(srcDescriptors, dstDescriptors);
		}

		private FeaturePresence srcDescriptors;
		private FeaturePresence dstDescriptors;
	}
}

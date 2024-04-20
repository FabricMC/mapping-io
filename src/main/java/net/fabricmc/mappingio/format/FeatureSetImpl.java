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

class FeatureSetImpl implements FeatureSet {
	FeatureSetImpl(boolean hasNamespaces, MetadataSupport fileMetadata, MetadataSupport elementMetadata, NameSupport packages, NameSupport classes, MemberSupport fields, MemberSupport methods, LocalSupport args, LocalSupport vars, ElementCommentSupport elementComments, boolean hasFileComments) {
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
	public NameSupport packages() {
		return packages;
	}

	@Override
	public NameSupport classes() {
		return classes;
	}

	@Override
	public MemberSupport fields() {
		return fields;
	}

	@Override
	public MemberSupport methods() {
		return methods;
	}

	@Override
	public LocalSupport args() {
		return args;
	}

	@Override
	public LocalSupport vars() {
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

	private final boolean hasNamespaces;
	private final MetadataSupport fileMetadata;
	private final MetadataSupport elementMetadata;
	private final NameSupport packages;
	private final NameSupport classes;
	private final MemberSupport fields;
	private final MemberSupport methods;
	private final LocalSupport args;
	private final LocalSupport vars;
	private final ElementCommentSupport elementComments;
	private final boolean hasFileComments;

	static class MemberSupportImpl implements MemberSupport {
		MemberSupportImpl(NameSupport names, DescSupport descriptors) {
			this.names = names;
			this.descriptors = descriptors;
		}

		@Override
		public SupportLevel srcNames() {
			return names.srcNames();
		}

		@Override
		public SupportLevel dstNames() {
			return names.dstNames();
		}

		@Override
		public SupportLevel srcDescs() {
			return descriptors.srcDescs();
		}

		@Override
		public SupportLevel dstDescs() {
			return descriptors.dstDescs();
		}

		private final NameSupport names;
		private final DescSupport descriptors;
	}

	static class LocalSupportImpl implements LocalSupport {
		LocalSupportImpl(SupportLevel positions, SupportLevel lvIndices, SupportLevel lvtRowIndices, SupportLevel startOpIndices, SupportLevel endOpIndices, NameSupport names, DescSupport descriptors) {
			this.positions = positions;
			this.lvIndices = lvIndices;
			this.lvtRowIndices = lvtRowIndices;
			this.startOpIndices = startOpIndices;
			this.endOpIndices = endOpIndices;
			this.names = names;
			this.descriptors = descriptors;
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
			return names.srcNames();
		}

		@Override
		public SupportLevel dstNames() {
			return names.dstNames();
		}

		@Override
		public SupportLevel srcDescs() {
			return descriptors.srcDescs();
		}

		@Override
		public SupportLevel dstDescs() {
			return descriptors.dstDescs();
		}

		private final SupportLevel positions;
		private final SupportLevel lvIndices;
		private final SupportLevel lvtRowIndices;
		private final SupportLevel startOpIndices;
		private final SupportLevel endOpIndices;
		private final NameSupport names;
		private final DescSupport descriptors;
	}

	static class NameSupportImpl implements NameSupport {
		NameSupportImpl(SupportLevel srcNames, SupportLevel dstNames) {
			this.srcNames = srcNames;
			this.dstNames = dstNames;
		}

		@Override
		public SupportLevel srcNames() {
			return srcNames;
		}

		@Override
		public SupportLevel dstNames() {
			return dstNames;
		}

		private final SupportLevel srcNames;
		private final SupportLevel dstNames;
	}

	static class DescSupportImpl implements DescSupport {
		DescSupportImpl(SupportLevel srcDescriptors, SupportLevel dstDescriptors) {
			this.srcDescriptors = srcDescriptors;
			this.dstDescriptors = dstDescriptors;
		}

		@Override
		public SupportLevel srcDescs() {
			return srcDescriptors;
		}

		@Override
		public SupportLevel dstDescs() {
			return dstDescriptors;
		}

		private final SupportLevel srcDescriptors;
		private final SupportLevel dstDescriptors;
	}
}

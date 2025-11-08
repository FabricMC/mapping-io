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

public interface FeatureSet {
	boolean hasNamespaces();
	MetadataSupport fileMetadata();
	MetadataSupport elementMetadata();
	PackageSupport packages();
	ClassSupport classes();
	MemberSupport fields();
	MemberSupport methods();
	LocalSupport args();
	LocalSupport vars();
	ElementCommentSupport elementComments();
	boolean hasFileComments();

	default boolean supportsPackages() {
		return packages().srcNames() != FeaturePresence.ABSENT;
	}

	default boolean supportsClasses() {
		return classes().srcNames() != FeaturePresence.ABSENT;
	}

	default boolean supportsFields() {
		return FeatureSetUtil.isSupported(fields());
	}

	default boolean supportsMethods() {
		return FeatureSetUtil.isSupported(methods());
	}

	default boolean supportsArgs() {
		return FeatureSetUtil.isSupported(args());
	}

	default boolean supportsVars() {
		return FeatureSetUtil.isSupported(vars());
	}

	enum MetadataSupport {
		/** No metadata at all. */
		NONE,

		/** Only some select properties.  */
		FIXED,

		/** Arbitrary metadata may be attached. */
		ARBITRARY
	}

	enum FeaturePresence {
		REQUIRED,
		OPTIONAL,
		ABSENT
	}

	interface NameSupport {
		FeaturePresence srcNames();
		FeaturePresence dstNames();
	}

	interface DescSupport {
		FeaturePresence srcDescs();
		FeaturePresence dstDescs();
	}

	interface PackageSupport extends NameSupport {
		boolean hasStructureModification();
	}

	interface ClassSupport extends NameSupport {
		boolean hasRepackaging();
	}

	interface MemberSupport extends NameSupport, DescSupport {
	}

	interface LocalSupport extends NameSupport, DescSupport {
		FeaturePresence positions();
		FeaturePresence lvIndices();
		FeaturePresence lvtRowIndices();
		FeaturePresence startOpIndices();
		FeaturePresence endOpIndices();
	}

	enum ElementCommentSupport {
		NAMESPACED,
		SHARED,
		NONE
	}
}

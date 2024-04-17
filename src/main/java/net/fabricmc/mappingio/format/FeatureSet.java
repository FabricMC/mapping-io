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

import org.jetbrains.annotations.ApiStatus;

public interface FeatureSet {
	boolean hasNamespaces();
	MetadataSupport fileMetadata();
	MetadataSupport elementMetadata();
	NameSupport packages();
	NameSupport classes();
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

	interface NameSupport {
		SupportLevel srcNames();
		SupportLevel dstNames();
	}

	interface DescSupport {
		SupportLevel srcDescs();
		SupportLevel dstDescs();
	}

	interface MemberSupport extends NameSupport, DescSupport {
	}

	interface LocalSupport extends NameSupport, DescSupport {
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

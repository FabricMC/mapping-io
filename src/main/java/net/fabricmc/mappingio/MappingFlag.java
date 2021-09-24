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

package net.fabricmc.mappingio;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum MappingFlag {
	/**
	 * Indication that the visitor may require multiple passes.
	 */
	NEEDS_MULTIPLE_PASSES,
	/**
	 * Requirement that metadata has to be provided in the header.
	 */
	NEEDS_HEADER_METADATA,
	/**
	 * Requirement that an element has to be visited only once within a pass.
	 *
	 * <p>This means that e.g. all members and properties of a class have to be visited after the same single
	 * visitClass invocation and no other visitClass invocation with the same srcName may occur.
	 */
	NEEDS_UNIQUENESS,
	/**
	 * Requirement that source field descriptors have to be supplied.
	 */
	NEEDS_SRC_FIELD_DESC,
	/**
	 * Requirement that source method descriptors have to be supplied.
	 */
	NEEDS_SRC_METHOD_DESC,
	/**
	 * Requirement that destination field descriptors have to be supplied.
	 */
	NEEDS_DST_FIELD_DESC,
	/**
	 * Requirement that destination method descriptors have to be supplied.
	 */
	NEEDS_DST_METHOD_DESC;

	public static final Set<MappingFlag> NONE = Collections.unmodifiableSet(EnumSet.noneOf(MappingFlag.class));
}

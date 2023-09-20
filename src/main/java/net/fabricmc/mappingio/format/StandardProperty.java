/*
 * Copyright (c) 2023 FabricMC
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

import java.util.Set;

import org.jetbrains.annotations.ApiStatus;

public interface StandardProperty {
	Set<MappingFormat> getApplicableFormats();
	boolean isApplicableTo(MappingFormat format);
	String getNameFor(MappingFormat format);

	/**
	 * Used internally by MappingTrees, consistency between JVM sessions
	 * or library versions isn't guaranteed!
	 */
	@ApiStatus.Internal
	String getId();
}

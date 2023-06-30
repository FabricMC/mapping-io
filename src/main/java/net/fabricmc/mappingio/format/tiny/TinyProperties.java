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

package net.fabricmc.mappingio.format.tiny;

/**
 * All standard properties of the Tiny format.
 * Internally, trees use the lowest common denominator.
 */
public final class TinyProperties {
	private TinyProperties() {
	}

	// Tiny v1
	static final String intermediaryCounter = "# INTERMEDIARY COUNTER";
	public static final String NEXT_INTERMEDIARY_CLASS = intermediaryCounter + " class";
	public static final String NEXT_INTERMEDIARY_FIELD = intermediaryCounter + " field";
	public static final String NEXT_INTERMEDIARY_METHOD = intermediaryCounter + " method";

	// Tiny v2
	public static final String NEXT_INTERMEDIARY_CLASS_TINY_2 = "next-intermediary-class";
	public static final String NEXT_INTERMEDIARY_FIELD_TINY_2 = "next-intermediary-field";
	public static final String NEXT_INTERMEDIARY_METHOD_TINY_2 = "next-intermediary-method";
	public static final String MISSING_LVT_INDICES = "missing-lvt-indices";
	public static final String ESCAPED_NAMES = "escaped-names";
}

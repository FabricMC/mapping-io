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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;

public final class StandardProperties {
	private StandardProperties() {
	}

	public static Set<StandardProperty> values() {
		return Collections.unmodifiableSet(values);
	}

	public static StandardProperty getByName(String name) {
		return valuesByName.get(name);
	}

	@ApiStatus.Internal
	public static StandardProperty getById(String id) {
		return valuesById.get(id);
	}

	public static final StandardProperty NEXT_INTERMEDIARY_CLASS;
	public static final StandardProperty NEXT_INTERMEDIARY_FIELD;
	public static final StandardProperty NEXT_INTERMEDIARY_METHOD;
	public static final StandardProperty NEXT_INTERMEDIARY_COMPONENT;
	public static final StandardProperty MISSING_LVT_INDICES;
	public static final StandardProperty ESCAPED_NAMES;
	private static final Set<StandardProperty> values = new HashSet<>();
	private static final Map<String, StandardProperty> valuesByName = new HashMap<>();
	private static final Map<String, StandardProperty> valuesById = new HashMap<>();

	static {
		NEXT_INTERMEDIARY_CLASS = builder("next-intermediary-class")
				.add(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER class")
				.add(MappingFormat.TINY_2_FILE, "next-intermediary-class")
				.build();
		NEXT_INTERMEDIARY_FIELD = builder("next-intermediary-field")
				.add(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER field")
				.add(MappingFormat.TINY_2_FILE, "next-intermediary-field")
				.build();
		NEXT_INTERMEDIARY_METHOD = builder("next-intermediary-method")
				.add(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER method")
				.add(MappingFormat.TINY_2_FILE, "next-intermediary-method")
				.build();
		NEXT_INTERMEDIARY_COMPONENT = builder("next-intermediary-component")
				.add(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER component")
				.add(MappingFormat.TINY_2_FILE, "next-intermediary-component")
				.build();
		MISSING_LVT_INDICES = builder("missing-lvt-indices")
				.add(MappingFormat.TINY_2_FILE, "missing-lvt-indices")
				.build();
		ESCAPED_NAMES = builder("escaped-names")
				.add(MappingFormat.TINY_2_FILE, "escaped-names")
				.build();
	}

	private static PropertyBuilder builder(String id) {
		return new PropertyBuilder(id);
	}

	private static class PropertyBuilder {
		PropertyBuilder(String id) {
			this.id = id;
		}

		PropertyBuilder add(MappingFormat format, String name) {
			nameByFormat.put(format, name);
			return this;
		}

		StandardProperty build() {
			StandardProperty ret = new StandardPropertyImpl(id, new HashMap<>(nameByFormat));
			values.add(ret);
			valuesById.put(id, ret);

			for (String name : nameByFormat.values()) {
				valuesByName.putIfAbsent(name, ret);
			}

			return ret;
		}

		private final String id;
		private final Map<MappingFormat, String> nameByFormat = new HashMap<>(4);
	}

	private static class StandardPropertyImpl implements StandardProperty {
		StandardPropertyImpl(String id, Map<MappingFormat, String> nameByFormat) {
			this.id = id;
			this.nameByFormat = nameByFormat;
		}

		@Override
		public Set<MappingFormat> getApplicableFormats() {
			return nameByFormat.keySet();
		}

		@Override
		public boolean isApplicableTo(MappingFormat format) {
			return nameByFormat.containsKey(format);
		}

		@Override
		public String getNameFor(MappingFormat format) {
			return nameByFormat.get(format);
		}

		@Override
		public String getId() {
			return id;
		}

		private final String id;
		private final Map<MappingFormat, String> nameByFormat;
	}
}

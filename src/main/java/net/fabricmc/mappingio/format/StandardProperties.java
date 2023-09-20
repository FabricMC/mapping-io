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

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.mappingio.MappedElementKind;

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
	public static final StandardProperty MODIFIED_ACCESS;
	public static final StandardProperty IS_STATIC;
	public static final StandardProperty START_LINE_NUMBER;
	public static final StandardProperty END_LINE_NUMBER;
	private static final Set<StandardProperty> values = new HashSet<>();
	private static final Map<String, StandardProperty> valuesByName = new HashMap<>();
	private static final Map<String, StandardProperty> valuesById = new HashMap<>();

	static {
		NEXT_INTERMEDIARY_CLASS = register("next-intermediary-class")
				.addMapping(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER class")
				.addMapping(MappingFormat.TINY_2_FILE, "next-intermediary-class");
		NEXT_INTERMEDIARY_FIELD = register("next-intermediary-field")
				.addMapping(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER field")
				.addMapping(MappingFormat.TINY_2_FILE, "next-intermediary-field");
		NEXT_INTERMEDIARY_METHOD = register("next-intermediary-method")
				.addMapping(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER method")
				.addMapping(MappingFormat.TINY_2_FILE, "next-intermediary-method");
		NEXT_INTERMEDIARY_COMPONENT = register("next-intermediary-component")
				.addMapping(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER component")
				.addMapping(MappingFormat.TINY_2_FILE, "next-intermediary-component");
		MISSING_LVT_INDICES = register("missing-lvt-indices")
				.addMapping(MappingFormat.TINY_2_FILE, "missing-lvt-indices");
		ESCAPED_NAMES = register("escaped-names")
				.addMapping(MappingFormat.TINY_2_FILE, "escaped-names");
		MODIFIED_ACCESS = register("modified-access")
				.addMapping(MappingFormat.ENIGMA_FILE, MappedElementKind.CLASS, "ACC:")
				.addMapping(MappingFormat.ENIGMA_FILE, MappedElementKind.FIELD, "ACC:")
				.addMapping(MappingFormat.ENIGMA_FILE, MappedElementKind.METHOD, "ACC:")
				.addMapping(MappingFormat.ENIGMA_DIR, MappedElementKind.CLASS, "ACC:")
				.addMapping(MappingFormat.ENIGMA_DIR, MappedElementKind.FIELD, "ACC:")
				.addMapping(MappingFormat.ENIGMA_DIR, MappedElementKind.METHOD, "ACC:");
		IS_STATIC = register("is-static")
				.addMapping(MappingFormat.TSRG_2_FILE, MappedElementKind.METHOD, "static");
		START_LINE_NUMBER = register("start-line-number")
				.addMapping(MappingFormat.PROGUARD_FILE, MappedElementKind.METHOD, null);
		END_LINE_NUMBER = register("end-line-number")
				.addMapping(MappingFormat.PROGUARD_FILE, MappedElementKind.METHOD, null);
	}

	private static StandardPropertyImpl register(String id) {
		return new StandardPropertyImpl(id);
	}

	private static class StandardPropertyImpl implements StandardProperty {
		StandardPropertyImpl(String id) {
			this.id = id;
			values.add(this);
			valuesById.put(id, this);
		}

		private StandardPropertyImpl addMapping(MappingFormat format, String name) {
			filePropNameByFormat.put(format, name);
			valuesByName.put(name, this);
			return this;
		}

		private StandardPropertyImpl addMapping(MappingFormat format, MappedElementKind elementKind, String name) {
			propElementKindByFormat.put(format, elementKind);
			elementPropNameByFormat.put(new SimpleEntry<>(format, elementKind), name);
			valuesByName.put(name, this);
			return this;
		}

		@Override
		public boolean isFileProperty() {
			return !filePropNameByFormat.isEmpty();
		}

		@Override
		public boolean isElementProperty() {
			return !elementPropNameByFormat.isEmpty();
		}

		@Override
		public Set<MappingFormat> getApplicableFormats() {
			return filePropNameByFormat.keySet();
		}

		@Override
		public Map<MappingFormat, MappedElementKind> getApplicableElementKinds() {
			return propElementKindByFormat;
		}

		@Override
		public boolean isApplicableTo(MappingFormat format) {
			return filePropNameByFormat.containsKey(format);
		}

		@Override
		public boolean isApplicableTo(MappingFormat format, MappedElementKind elementKind) {
			return elementPropNameByFormat.containsKey(new SimpleEntry<>(format, elementKind));
		}

		@Override
		public String getNameFor(MappingFormat format) {
			return filePropNameByFormat.get(format);
		}

		@Override
		public String getNameFor(MappingFormat format, MappedElementKind elementKind) {
			return elementPropNameByFormat.get(new SimpleEntry<>(format, elementKind));
		}

		@Override
		public String getId() {
			return id;
		}

		private final String id;
		private final Map<MappingFormat, String> filePropNameByFormat = new HashMap<>(4);
		private final Map<Entry<MappingFormat, MappedElementKind>, String> elementPropNameByFormat = new HashMap<>(4);
		private final Map<MappingFormat, MappedElementKind> propElementKindByFormat = new HashMap<>(4);
	}
}

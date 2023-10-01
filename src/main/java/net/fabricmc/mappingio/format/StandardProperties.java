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
import java.util.stream.Collectors;

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
	public static final StandardProperty PARAM_DEST_POS;
	private static final Set<StandardProperty> values = new HashSet<>();
	private static final Map<String, StandardProperty> valuesByName = new HashMap<>();
	private static final Map<String, StandardProperty> valuesById = new HashMap<>();

	static {
		NEXT_INTERMEDIARY_CLASS = register(
			"next-intermediary-class",
			new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER class");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-class");
				}},
			Collections.emptyMap());
		NEXT_INTERMEDIARY_FIELD = register(
			"next-intermediary-field",
			new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER field");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-field");
				}},
			Collections.emptyMap());
		NEXT_INTERMEDIARY_METHOD = register(
			"next-intermediary-method",
			new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER method");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-method");
				}},
			Collections.emptyMap());
		NEXT_INTERMEDIARY_COMPONENT = register(
			"next-intermediary-component",
			new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_FILE, "INTERMEDIARY_COUNTER component");
					put(MappingFormat.TINY_2_FILE, "next-intermediary-component");
				}},
			Collections.emptyMap());
		MISSING_LVT_INDICES = register(
			"missing-lvt-indices",
			new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_2_FILE, "missing-lvt-indices");
				}},
			Collections.emptyMap());
		ESCAPED_NAMES = register(
			"escaped-names",
			new HashMap<MappingFormat, String>() {{
					put(MappingFormat.TINY_2_FILE, "escaped-names");
				}},
			Collections.emptyMap());
		MODIFIED_ACCESS = register(
			"modified-access",
			Collections.emptyMap(),
			new HashMap<Entry<MappingFormat, MappedElementKind>, String>() {{
					put(new SimpleEntry<>(MappingFormat.ENIGMA_FILE, MappedElementKind.CLASS), "ACC:");
					put(new SimpleEntry<>(MappingFormat.ENIGMA_FILE, MappedElementKind.FIELD), "ACC:");
					put(new SimpleEntry<>(MappingFormat.ENIGMA_FILE, MappedElementKind.METHOD), "ACC:");
					put(new SimpleEntry<>(MappingFormat.ENIGMA_DIR, MappedElementKind.CLASS), "ACC:");
					put(new SimpleEntry<>(MappingFormat.ENIGMA_DIR, MappedElementKind.FIELD), "ACC:");
					put(new SimpleEntry<>(MappingFormat.ENIGMA_DIR, MappedElementKind.METHOD), "ACC:");
				}});
		IS_STATIC = register(
			"is-static",
			Collections.emptyMap(),
			new HashMap<Entry<MappingFormat, MappedElementKind>, String>() {{
					put(new SimpleEntry<>(MappingFormat.TSRG_2_FILE, MappedElementKind.METHOD), "static");
				}});
		START_LINE_NUMBER = register(
			"start-line-number",
			Collections.emptyMap(),
			new HashMap<Entry<MappingFormat, MappedElementKind>, String>() {{
					put(new SimpleEntry<>(MappingFormat.PROGUARD_FILE, MappedElementKind.FIELD), null);
					put(new SimpleEntry<>(MappingFormat.PROGUARD_FILE, MappedElementKind.FIELD), null);
				}});
		END_LINE_NUMBER = register(
			"end-line-number",
			Collections.emptyMap(),
			new HashMap<Entry<MappingFormat, MappedElementKind>, String>() {{
					put(new SimpleEntry<>(MappingFormat.PROGUARD_FILE, MappedElementKind.FIELD), null);
					put(new SimpleEntry<>(MappingFormat.PROGUARD_FILE, MappedElementKind.FIELD), null);
				}});
		PARAM_DEST_POS = register(
			"parameter-destination-position",
			Collections.emptyMap(),
			new HashMap<Entry<MappingFormat, MappedElementKind>, String>() {{
					put(new SimpleEntry<>(MappingFormat.MATCH_FILE, MappedElementKind.METHOD_ARG), null);
				}});
	}

	private static StandardProperty register(String id, Map<MappingFormat, String> filePropNameByFormat,
			Map<Entry<MappingFormat, MappedElementKind>, String> elementPropNameByFormat) {
		StandardProperty ret = new StandardPropertyImpl(id, filePropNameByFormat, elementPropNameByFormat);
		values.add(ret);
		valuesById.put(id, ret);

		for (String name : filePropNameByFormat.values()) {
			valuesByName.putIfAbsent(name, ret);
		}

		for (String name : elementPropNameByFormat.values()) {
			valuesByName.putIfAbsent(name, ret);
		}

		return ret;
	}

	static class StandardPropertyImpl implements StandardProperty {
		StandardPropertyImpl(String id, Map<MappingFormat, String> filePropNameByFormat,
				Map<Entry<MappingFormat, MappedElementKind>, String> elementPropNameByFormat) {
			this.id = id;
			this.filePropNameByFormat = filePropNameByFormat;
			this.elementPropNameByFormat = elementPropNameByFormat;
			this.propElementKindByFormat = elementPropNameByFormat.entrySet().stream()
					.map(entry -> entry.getKey())
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
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
		private final Map<MappingFormat, String> filePropNameByFormat;
		private final Map<Entry<MappingFormat, MappedElementKind>, String> elementPropNameByFormat;
		private final Map<MappingFormat, MappedElementKind> propElementKindByFormat;
	}
}

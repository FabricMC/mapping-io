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

/**
 * Internal properties which mapping readers attach while parsing.
 * They are removed again when saving via a {@link net.fabricmc.mappingio.MappingWriter MappingWriter}.
 */
public final class MioTempProperties {
	public static final String MIO_PREFIX = "mio:";

	private static String register(String name) {
		return MIO_PREFIX + name;
	}
}

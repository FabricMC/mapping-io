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

package net.fabricmc.mappingio.i18n;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.format.MappingFormat;

public class I18nTest {
	@Test
	public void mappingFormats() {
		for (MappingFormat format : MappingFormat.values()) {
			String enUsName = format.getName(MioLocale.EN_US);
			assertTrue(format.name.equals(enUsName));

			for (MioLocale locale : MioLocale.values()) {
				String translatedName = format.getName(locale);
				assertFalse(translatedName.startsWith("format."));

				if (locale != MioLocale.EN_US) {
					assertFalse(translatedName.equals(enUsName));
				}
			}
		}
	}
}

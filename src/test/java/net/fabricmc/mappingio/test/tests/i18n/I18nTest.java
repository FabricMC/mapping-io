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

package net.fabricmc.mappingio.test.tests.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.format.MappingFormat;

public class I18nTest {
	private static List<Locale> supportedLocales;

	@BeforeAll
	public static void init() throws Exception {
		URL i18nUrl = I18nTest.class.getResource("/mappingio/lang/");
		Path path = Paths.get(i18nUrl.toURI());

		supportedLocales = Files.walk(path)
				.map(Path::toAbsolutePath)
				.map(Path::toString)
				.filter(name -> name.endsWith(".properties"))
				.map(name -> name.substring(Math.max(0, name.lastIndexOf(File.separatorChar) + 1), name.length() - ".properties".length()))
				.map(tag -> Locale.forLanguageTag(tag))
				.collect(Collectors.toList());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void mappingFormatNames() {
		for (MappingFormat format : MappingFormat.values()) {
			String enUsName = format.translatableName().translate(Locale.US);
			assertEquals(enUsName, format.name);

			for (Locale locale : supportedLocales) {
				String translatedName = format.translatableName().translate(locale);
				assertFalse(translatedName.startsWith("format."), "Translated name for " + format + " in " + locale + " is missing");
			}
		}
	}
}

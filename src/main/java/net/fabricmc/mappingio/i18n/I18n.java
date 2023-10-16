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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class I18n {
	private I18n() {
	}

	public static String translate(String key, MioLocale locale, Object... args) {
		return String.format(translate(key, locale), args);
	}

	public static String translate(String key, MioLocale locale) {
		try {
			return messageBundles.getOrDefault(locale, load(locale)).getString(key);
		} catch (Exception e) {
			System.err.println("Exception while translating key " + key + " to locale " + locale.id + ": " + e.getMessage());
			if (locale == fallbackLocale) return key;

			try {
				return messageBundles.getOrDefault(fallbackLocale, load(fallbackLocale)).getString(key);
			} catch (Exception e2) {
				System.err.println("Exception while translating key " + key + " to fallback locale: " + e2.getMessage());
				return key;
			}
		}
	}

	private static ResourceBundle load(MioLocale locale) {
		ResourceBundle resBundle;
		String resName = String.format("/i18n/%s.properties", locale.id);
		URL resUrl = I18n.class.getResource(resName);

		if (resUrl == null) {
			throw new RuntimeException("Locale resource not found: " + resName);
		}

		try (Reader reader = new InputStreamReader(resUrl.openStream(), StandardCharsets.UTF_8)) {
			resBundle = new PropertyResourceBundle(reader);
			messageBundles.put(locale, resBundle);
			return resBundle;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load " + resName, e);
		}
	}

	private static final MioLocale fallbackLocale = MioLocale.EN_US;
	private static final Map<MioLocale, ResourceBundle> messageBundles = new HashMap<>();
}

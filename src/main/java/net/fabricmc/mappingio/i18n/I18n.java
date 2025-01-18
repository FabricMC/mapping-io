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
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class I18n {
	private I18n() {
	}

	public static String translate(String key, Locale locale, Object... args) {
		return String.format(translate(key, locale), args);
	}

	public static String translate(String key, Locale locale) {
		try {
			return load(locale).getString(key);
		} catch (Exception e) {
			System.err.println("Exception while translating key " + key + " to locale " + locale.toLanguageTag() + ": " + getStackTrace(e));
			if (locale == fallbackLocale) return key;

			try {
				return load(fallbackLocale).getString(key);
			} catch (Exception e2) {
				System.err.println("Exception while translating key " + key + " to fallback locale: " + getStackTrace(e2));
				return key;
			}
		}
	}

	private static ResourceBundle load(Locale locale) {
		ResourceBundle bundle = messageBundles.get(locale);

		if (bundle != null) {
			return bundle;
		}

		bundlesLock.lock();

		try {
			if ((bundle = messageBundles.get(locale)) != null) {
				return bundle;
			}

			return load0(locale);
		} finally {
			bundlesLock.unlock();
		}
	}

	private static ResourceBundle load0(Locale locale) {
		ResourceBundle resBundle;
		String resName = String.format("/mappingio/lang/%s.properties", locale.toLanguageTag().replace('-', '_').toLowerCase(Locale.ROOT));
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

	private static String getStackTrace(Throwable t) {
		if (t == null) return null;

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	private static final Lock bundlesLock = new ReentrantLock();
	private static final Locale fallbackLocale = Locale.US;
	private static final Map<Locale, ResourceBundle> messageBundles = new HashMap<>();
}

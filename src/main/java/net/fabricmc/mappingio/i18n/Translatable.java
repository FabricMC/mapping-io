/*
 * Copyright (c) 2024 FabricMC
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

import java.util.Locale;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface Translatable {
	@ApiStatus.Internal
	static Translatable of(String translationKey) {
		return new TranslatableImpl(translationKey);
	}

	/**
	 * Translates this translatable to the specified locale, with a fallback to en_US.
	 */
	String translate(Locale locale);

	/**
	 * Returns the translation key of this translatable, allowing consumers to provide their own translations
	 * via custom localization facilities.
	 */
	String translationKey();
}

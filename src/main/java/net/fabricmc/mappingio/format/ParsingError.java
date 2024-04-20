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

package net.fabricmc.mappingio.format;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface ParsingError {
	static ParsingError create(Severity severity, String message) {
		return new ParsingError() {
			@Override
			public Severity getSeverity() {
				return severity;
			}

			@Override
			public String getMessage() {
				return message;
			}
		};
	}

	Severity getSeverity();

	String getMessage();

	enum Severity {
		/**
		 * When something's technically wrong but doesn't affect
		 * parsing or the mapping data in any way.
		 */
		INFO,
		/**
		 * When element data is partially missing, but the rest of the element
		 * could still be deciphered and it didn't have to be skipped entirely.
		 * Or when an unknown top-level element is encountered.
		 */
		WARNING,
		/**
		 * An issue so severe that parsing of entire elements had to be skipped.
		 * E.g. a class's/member's source name being absent.
		 */
		ERROR
	}
}

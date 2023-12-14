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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;

public interface ErrorCollector {
	static ErrorCollector create() {
		return new ErrorCollector() {
			@Override
			public void add(Severity severity, String message) throws IOException {
				errors.add(new ParsingError(severity, message));
			}

			@Override
			public List<ParsingError> getErrors() {
				return errors;
			}

			private final List<ParsingError> errors = new ArrayList<>();
		};
	}

	default void addInfo(String message) throws IOException {
		add(Severity.INFO, message);
	}

	default void addWarning(String message) throws IOException {
		add(Severity.WARNING, message);
	}

	default void addError(String message) throws IOException {
		add(Severity.ERROR, message);
	}

	void add(Severity severity, String message) throws IOException;

	List<ParsingError> getErrors();

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

	class ParsingError {
		ParsingError(Severity severity, String message) {
			this.severity = severity;
			this.message = message;
		}

		public Severity getSeverity() {
			return severity;
		}

		public String getMessage() {
			return message;
		}

		private final Severity severity;
		private final String message;
	}

	@ApiStatus.Internal
	class ThrowingErrorCollector implements ErrorCollector {
		public ThrowingErrorCollector(Severity severityToThrowAt) {
			this.severityToThrowAt = severityToThrowAt;
		}

		@Override
		public void add(Severity severity, String message) throws IOException {
			if (severity.compareTo(severityToThrowAt) >= 0) {
				throw new IOException(message);
			}
		}

		@Override
		public List<ParsingError> getErrors() {
			return Collections.emptyList();
		}

		private Severity severityToThrowAt;
	}
}

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

import net.fabricmc.mappingio.format.ParsingError.Severity;

public interface ErrorSink {
	static ErrorSink noOp() {
		return new ErrorSink() {
			@Override
			public void add(Severity severity, String message) {
			}
		};
	}

	/**
	 * Create an ErrorSink that throws an exception when an error
	 * of the specified severity (or higher) is encountered.
	 *
	 * @param severity The severity to throw on.
	 * @return The constructed ErrorSink instance.
	 */
	static ErrorSink throwingOnSeverity(Severity severity) {
		return new ThrowingErrorSink(severity);
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
}

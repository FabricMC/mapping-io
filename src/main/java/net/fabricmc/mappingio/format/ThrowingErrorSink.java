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

import java.io.IOException;

import net.fabricmc.mappingio.format.ParsingError.Severity;

final class ThrowingErrorSink implements ErrorSink {
	ThrowingErrorSink(Severity severityToThrowAt) {
		this.severityToThrowAt = severityToThrowAt;
	}

	@Override
	public void add(Severity severity, String message) throws IOException {
		if (severity.compareTo(severityToThrowAt) >= 0) {
			throw new IOException(message);
		}
	}

	private final Severity severityToThrowAt;
}

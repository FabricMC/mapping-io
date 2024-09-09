/*
 * Copyright (c), Data Geekery GmbH, contact@datageekery.com
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

package net.fabricmc.mappingio.lib.jool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import net.fabricmc.mappingio.lib.jool.fi.lang.CheckedRunnable;

/**
 * Improved interoperability between checked exceptions and Java 8.
 *
 * <p>Checked exceptions are one of Java's biggest flaws. Due to backwards-compatibility, we're inheriting all the checked
 * exception trouble back from JDK 1.0. This becomes even more obvious when using lambda expressions, most of which are
 * not allowed to throw checked exceptions.
 *
 * <p>This library tries to ease some pain and wraps / unwraps a variety of API elements from the JDK 8 to improve
 * interoperability with checked exceptions.
 *
 * @author Lukas Eder
 */
public class Unchecked {
	private Unchecked() { }

	/**
	 * A {@link Consumer} that wraps any {@link Throwable} in a {@link RuntimeException}.
	 */
	public static final Consumer<Throwable> THROWABLE_TO_RUNTIME_EXCEPTION = t -> {
		if (t instanceof Error) {
			throw (Error) t;
		}

		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}

		if (t instanceof IOException) {
			throw new UncheckedIOException((IOException) t);
		}

		// [#230] Clients will not expect needing to handle this.
		if (t instanceof InterruptedException) {
			Thread.currentThread().interrupt();
		}

		throw new RuntimeException(t);
	};

	/**
	 * Wrap a {@link CheckedRunnable} in a {@link Runnable}.
	 *
	 * <p>Example:
	 * <pre><code>
	 * new Thread(Unchecked.runnable(() -> {
	 *     throw new Exception("Cannot run this thread");
	 * })).start();
	 * </code></pre>
	 */
	public static Runnable runnable(CheckedRunnable runnable) {
		return runnable(runnable, THROWABLE_TO_RUNTIME_EXCEPTION);
	}

	/**
	 * Wrap a {@link CheckedRunnable} in a {@link Runnable} with a custom handler for checked exceptions.
	 *
	 * <p>Example:
	 * <pre><code>
	 * new Thread(Unchecked.runnable(
	 *     () -> {
	 *         throw new Exception("Cannot run this thread");
	 *     },
	 *     e -> {
	 *         throw new IllegalStateException(e);
	 *     }
	 * )).start();
	 * </code></pre>
	 */
	public static Runnable runnable(CheckedRunnable runnable, Consumer<Throwable> handler) {
		return () -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				handler.accept(e);

				throw new IllegalStateException("Exception handler must throw a RuntimeException", e);
			}
		};
	}
}

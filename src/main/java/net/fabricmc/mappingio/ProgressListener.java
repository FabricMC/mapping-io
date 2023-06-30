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

package net.fabricmc.mappingio;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public abstract class ProgressListener {
	protected ProgressListener(LogLevel logLevel) {
		this.logLevel = logLevel;
		this.forwarder = this instanceof Forwarder ? (Forwarder) this : new Forwarder(logLevel, this);
	}

	/**
	 * Sets the progress listener's total amount of steps and provides a name for the job.
	 * If the total amount of steps isn't known beforehand, {@code totalWork} must be set to -1.
	 * Can only be called once.
	 */
	protected abstract void init(int totalWork, String title);

	/**
	 * Indicates the start of a new step being processed, and optionally gives it a name.
	 */
	protected abstract void startStep(LogLevel logLevel, @Nullable String stepName);

	/**
	 * Updates the name of the current step.
	 */
	protected abstract void updateMessage(LogLevel logLevel, @Nullable String stepName);

	/**
	 * Indicates that all steps have finished processing.
	 * After that point, further method invocations are illegal.
	 */
	protected abstract void finish();

	/**
	 * Determines the granularity of progress reports the progress listener wishes to receive.
	 */
	public enum LogLevel {
		FILES,
		CLASSES,
		MEMBERS;

		public boolean allows(LogLevel logLevel) {
			if (logLevel.compareTo(this) <= 0) {
				return true;
			}

			return false;
		}
	}

	/**
	 * Class which forwards only applicable log levels to the passed receiver,
	 * and automatically checks for illegal calls.
	 */
	@ApiStatus.Internal
	public final class Forwarder extends ProgressListener {
		private Forwarder(LogLevel logLevel, ProgressListener receiver) {
			super(logLevel);

			this.receiver = receiver;
		}

		@Override
		public void init(int totalWork, String title) {
			assertNotFinished();

			if (initialized && receiver != NOP) {
				throw new RuntimeException("Progress listener can only be initialized once!");
			}

			receiver.init(totalWork, title);
			initialized = true;
		}

		@Override
		public void startStep(LogLevel logLevel, @Nullable String stepName) {
			assertNotFinished();

			if (this.logLevel.allows(logLevel)) {
				receiver.startStep(logLevel, stepName);
			}
		}

		@Override
		public void updateMessage(LogLevel logLevel, @Nullable String stepName) {
			assertNotFinished();

			if (this.logLevel.allows(logLevel)) {
				receiver.updateMessage(logLevel, stepName);
			}
		}

		@Override
		public void finish() {
			assertNotFinished();
			receiver.finish();
			finished = true;
		}

		private void assertNotFinished() {
			if (finished && receiver != NOP) {
				throw new RuntimeException("Illegal method invocation, progress listener has already finished!");
			}
		}

		private final ProgressListener receiver;
		private boolean initialized;
		private boolean finished;
	}

	/**
	 * No-op progress listener that ignores all events.
	 */
	public static final ProgressListener NOP = new ProgressListener(LogLevel.FILES) {
		@Override
		public void init(int totalWork, String title) {
		}

		@Override
		public void startStep(LogLevel logLevel, @Nullable String stepName) {
		}

		@Override
		public void updateMessage(LogLevel logLevel, @Nullable String stepName) {
		}

		@Override
		public void finish() {
		}
	};

	/**
	 * Finest log level the progress listener accepts.
	 */
	public final LogLevel logLevel;

	/**
	 * Public-facing progress listener interface for progress passers to interact with,
	 * which automatically checks for illegal calls and only forwards the applicable
	 * log levels to the actual {@link ProgressListener}.
	 */
	@ApiStatus.Internal
	public final Forwarder forwarder;
}

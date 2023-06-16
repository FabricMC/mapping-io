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

import org.jetbrains.annotations.Nullable;

public abstract class ProgressListener {
	protected ProgressListener(LogLevel logLevel) {
		this.logLevel = logLevel;
		this.forwarder = new Forwarder(logLevel, this);
	}

	/**
	 * Sets the progress listener's total amount of steps and provides a name of the progressing job.
	 * Set totalWork to -1 if you don't know how many steps there will be.
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
	 */
	protected abstract void finish();

	public enum LogLevel {
		FILES,
		CLASSES,
		MEMBERS,
		LOCALS_AND_COMMENTS;

		public boolean allows(LogLevel logLevel) {
			if (logLevel.compareTo(this) <= 0) {
				return true;
			}

			return false;
		}
	}

	public final class Forwarder extends ProgressListener {
		private Forwarder(LogLevel logLevel, ProgressListener receiver) {
			super(logLevel);
			this.receiver = receiver;
		}

		@Override
		public void init(int totalWork, String title) {
			receiver.init(totalWork, title);
		}

		@Override
		public void startStep(LogLevel logLevel, @Nullable String stepName) {
			if (this.logLevel.allows(logLevel)) {
				receiver.startStep(logLevel, stepName);
			}
		}

		@Override
		public void updateMessage(LogLevel logLevel, @Nullable String stepName) {
			if (this.logLevel.allows(logLevel)) {
				receiver.updateMessage(logLevel, stepName);
			}
		}

		@Override
		public void finish() {
			receiver.finish();
		}

		private final ProgressListener receiver;
	}

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
	public final LogLevel logLevel;
	public final Forwarder forwarder;
}

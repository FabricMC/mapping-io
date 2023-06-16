/*
 * Copyright (c) 2021 FabricMC
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

package net.fabricmc.mappingio.format.enigma;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.ProgressListener;
import net.fabricmc.mappingio.ProgressListener.LogLevel;

public final class EnigmaDirReader {
	public static void read(Path dir, MappingVisitor visitor, ProgressListener progressListener) throws IOException {
		read(dir, MappingUtil.NS_SOURCE_FALLBACK, MappingUtil.NS_TARGET_FALLBACK, visitor, progressListener);
	}

	public static void read(Path dir, String sourceNs, String targetNs, MappingVisitor visitor, ProgressListener progressListener) throws IOException {
		ProgressListener delegatingProgressListener = new ProgressListener(progressListener.logLevel) {
			@Override
			public void init(int totalWork, String title) {
			}

			@Override
			public void startStep(LogLevel logLevel, @Nullable String message) {
				progressListener.forwarder.updateMessage(logLevel, message);
			}

			@Override
			public void updateMessage(LogLevel logLevel, @Nullable String message) {
				progressListener.forwarder.updateMessage(logLevel, message);
			}

			@Override
			public void finish() {
			}
		};

		List<Path> files = Files.walk(dir)
				.filter(file -> !Files.isDirectory(file))
				.filter(file -> file.toString().endsWith("." + DIR_FILE_EXT))
				.collect(Collectors.toList());

		progressListener.forwarder.init(files.size(), "Reading Enigma directory");

		for (Path file : files) {
			progressListener.forwarder.startStep(LogLevel.FILES, dir.relativize(file).toString());
			EnigmaFileReader.read(Files.newBufferedReader(file), sourceNs, targetNs, visitor, delegatingProgressListener);
		}

		visitor.visitEnd();
		progressListener.forwarder.finish();
	}

	static final String DIR_FILE_EXT = "mapping"; // non-plural form unlike ENIGMA_FILE
}

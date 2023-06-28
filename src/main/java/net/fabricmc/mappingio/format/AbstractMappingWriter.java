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
import java.io.Writer;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.ProgressListener;
import net.fabricmc.mappingio.ProgressListener.LogLevel;

/**
 * A MappingWriter which automatically:
 * <ul>
 * <li>takes care of the underlying progress listener,</li>
 * <li>returns {@code true} or {@code false} for the visit methods, depending on whether the current format supports the operation,</li>
 * <li>stubs most other visit methods as well, so writers which don't support certain visits don't have empty methods cluttering the file, and</li>
 * <li>closes the writer in {@link #close()}.</li>
 * </ul>
 */
@ApiStatus.Internal
public abstract class AbstractMappingWriter implements MappingWriter {
	public AbstractMappingWriter(MappingFormat format, Writer writer, ProgressListener progressListener, String taskTitle) {
		this.format = format;
		this.writer = writer;
		this.progressListener = progressListener;
		this.taskTitle = taskTitle;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
	}

	@Override
	public boolean visitContent(int classCount, int fieldCount, int methodCount, int methodArgCount, int methodVarCount, int commentCount, int metadataCount) throws IOException {
		int totalWork = 0;

		if (progressListener.logLevel.allows(LogLevel.CLASSES)) {
			if (metadataCount > 0) totalWork += metadataCount;
			if (classCount > 0) totalWork += classCount;
		}

		if (progressListener.logLevel.allows(LogLevel.MEMBERS)) {
			if (fieldCount > 0) totalWork += fieldCount;
			if (methodCount > 0) totalWork += methodCount;
		}

		progressListener.forwarder.init(totalWork > 0 ? totalWork : -1, taskTitle);
		progressListenerInitialized = true;
		return true;
	}

	@Override
	public void visitMetadata(String key, String value) throws IOException {
		if (!progressListenerInitialized) return;

		progressListener.forwarder.startStep(LogLevel.CLASSES, "Writing metadata");
	}

	@Override
	public boolean visitClass(String srcName) throws IOException {
		progressListener.forwarder.startStep(LogLevel.CLASSES, "Writing class: " + srcName);
		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) throws IOException {
		progressListener.forwarder.startStep(LogLevel.MEMBERS, "Writing field: " + srcName);
		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) throws IOException {
		progressListener.forwarder.startStep(LogLevel.MEMBERS, "Writing method: " + srcName);
		return true;
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException {
		return format.supportsArgs;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
		return format.supportsLocals;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
	}

	@Override
	public boolean visitEnd() throws IOException {
		progressListener.forwarder.finish();
		return true;
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	protected final MappingFormat format;
	protected final ProgressListener progressListener;
	protected final String taskTitle;
	protected Writer writer;
	private boolean progressListenerInitialized;
}

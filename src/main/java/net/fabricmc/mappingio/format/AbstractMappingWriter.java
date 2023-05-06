package net.fabricmc.mappingio.format;

import java.io.IOException;
import java.io.Writer;

import org.jetbrains.annotations.ApiStatus;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.ProgressListener;
import net.fabricmc.mappingio.ProgressListener.LogLevel;

/**
 * MappingWriter base class that automatically takes care of the underlying progress listener.
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
	public boolean visitContent(int classCount, int fieldCount, int methodCount, int methodArgCount, int methodVarCount, int commentCount, int metadataCount) throws IOException {
		int totalWork = 0;
		if (progressListener.logLevel.allows(LogLevel.CLASSES) && classCount > 0) totalWork = classCount;
		if (progressListener.logLevel.allows(LogLevel.MEMBERS)) {
			if (fieldCount > 0) totalWork += fieldCount;
			if (methodCount > 0) totalWork += methodCount;
			if (metadataCount > 0) totalWork += metadataCount;
		}
		if (progressListener.logLevel.allows(LogLevel.LOCALS_AND_COMMENTS)) {
			if (format.supportsArgs && methodArgCount > 0) totalWork += methodArgCount;
			if (format.supportsLocals && methodArgCount > 0) totalWork += methodVarCount;
			if (format.supportsComments && commentCount > 0) totalWork += commentCount;
		}

		progressListener.forwarder.init(totalWork > 0 ? totalWork : -1, taskTitle);
		progressListenerInitialized = true;
		return true;
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
		if (!format.supportsArgs) return false;

		progressListener.forwarder.startStep(LogLevel.LOCALS_AND_COMMENTS, "Writing method arg: " + srcName);
		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
		if (!format.supportsLocals) return false;

		progressListener.forwarder.startStep(LogLevel.LOCALS_AND_COMMENTS, "Writing method var: " + srcName);
		return true;
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
		if (!format.supportsComments) return;

		progressListener.forwarder.startStep(LogLevel.LOCALS_AND_COMMENTS, "Writing comment");
	}

	@Override
	public void visitMetadata(String key, String value) throws IOException {
		if (!progressListenerInitialized) return;

		progressListener.forwarder.startStep(LogLevel.MEMBERS, "Writing metadata");
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

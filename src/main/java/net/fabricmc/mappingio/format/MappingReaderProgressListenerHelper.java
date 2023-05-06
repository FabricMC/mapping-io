package net.fabricmc.mappingio.format;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.ProgressListener;
import net.fabricmc.mappingio.ProgressListener.LogLevel;

/**
 * Helper for MappingReaders which takes care of forwarding the current step to the progress listener.
 */
@ApiStatus.Internal
public class MappingReaderProgressListenerHelper {
	public MappingReaderProgressListenerHelper(ProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	public void init(int totalWork, String taskTitle) {
		progressListener.forwarder.init(-1, taskTitle);
		progressListenerInitialized = true;
	}

	public void readFile(@Nullable String name) {
		progressListener.forwarder.startStep(LogLevel.FILES, "Reading file" + (name == null ? "" : ": " + name));
	}

	public void readClass(@Nullable String name) {
		progressListener.forwarder.startStep(LogLevel.CLASSES, "Reading class" + (name == null ? "" : ": " + name));
	}

	public void readField(@Nullable String name) {
		progressListener.forwarder.startStep(LogLevel.MEMBERS, "Reading field" + (name == null ? "" : ": " + name));
	}

	public void readMethod(@Nullable String name) {
		progressListener.forwarder.startStep(LogLevel.MEMBERS, "Reading method" + (name == null ? "" : ": " + name));
	}

	public void readMethodArg(@Nullable Integer pos, @Nullable String name) {
		progressListener.forwarder.startStep(LogLevel.LOCALS_AND_COMMENTS, "Reading method arg"
				+ (pos == null ? "" : " " + pos)
				+ (name == null ? "" : ": " + name));
	}

	public void readMethodVar(@Nullable String name) {
		progressListener.forwarder.startStep(LogLevel.LOCALS_AND_COMMENTS, "Reading method var" + (name == null ? "" : ": " + name));
	}

	public void readComment() {
		progressListener.forwarder.startStep(LogLevel.LOCALS_AND_COMMENTS, "Reading comment");
	}

	public void readMetadata() {
		if (!progressListenerInitialized) return;
		progressListener.forwarder.startStep(LogLevel.MEMBERS, "Reading metadata");
	}

	public void finish() {
		progressListener.forwarder.finish();
	}

	protected final ProgressListener progressListener;
	protected boolean progressListenerInitialized;
}

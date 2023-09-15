package net.fabricmc.mappingio.format;

import java.util.Set;

import org.jetbrains.annotations.ApiStatus;

public interface StandardProperty {
	Set<MappingFormat> getApplicableFormats();
	boolean isApplicableTo(MappingFormat format);
	String getNameFor(MappingFormat format);

	/**
	 * Used internally by MappingTrees, consistency between JVM sessions
	 * or library versions isn't guaranteed!
	 */
	@ApiStatus.Internal
	String getId();
}

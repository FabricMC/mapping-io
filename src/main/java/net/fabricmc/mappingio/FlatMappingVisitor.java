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

package net.fabricmc.mappingio;

import static net.fabricmc.mappingio.MappingUtil.toArray;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.RegularAsFlatMappingVisitor;

/**
 * A mapping visitor that provides the entire data for a given element within a single visit call.
 */
public interface FlatMappingVisitor {
	default Set<MappingFlag> getFlags() {
		return MappingFlag.NONE;
	}

	default void reset() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determine whether the header (namespaces, metadata if part of the header) should be visited.
	 *
	 * @return {@code true} if the header is to be visited, {@code false} otherwise.
	 */
	default boolean visitHeader() throws IOException {
		return true;
	}

	void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException;

	default void visitMetadata(String key, @Nullable String value) throws IOException { }

	/**
	 * Determine whether the mapping content (classes and anything below, metadata if not part of the header) should be visited.
	 *
	 * @return {@code true} if content is to be visited, {@code false} otherwise.
	 */
	default boolean visitContent() throws IOException {
		return true;
	}

	boolean visitClass(String srcName, @Nullable String[] dstNames) throws IOException;
	/**
	 * @deprecated Use {@link #visitClassComment(String, String[], String, CommentStyle)} instead.
	 */
	@Deprecated
	void visitClassComment(String srcName, @Nullable String[] dstNames, String comment) throws IOException;
	default void visitClassComment(String srcName, @Nullable String[] dstNames, String comment, CommentStyle style) throws IOException {
		visitClassComment(srcName, dstNames, comment);
	}

	boolean visitField(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs) throws IOException;
	/**
	 * @deprecated Use {@link #visitFieldComment(String, String, String, String[], String[], String[], String, CommentStyle)} instead.
	 */
	@Deprecated
	void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs,
			String comment) throws IOException;
	default void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs,
			String comment, CommentStyle style) throws IOException {
		visitFieldComment(srcClsName, srcName, srcDesc, dstClsNames, dstNames, dstDescs, comment);
	}

	boolean visitMethod(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs) throws IOException;
	/**
	 * @deprecated Use {@link #visitMethodComment(String, String, String, String[], String[], String[], String, CommentStyle)} instead.
	 */
	@Deprecated
	void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs,
			String comment) throws IOException;
	default void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String[] dstClsNames, @Nullable String[] dstNames, @Nullable String[] dstDescs,
			String comment, CommentStyle style) throws IOException {
		visitMethodComment(srcClsName, srcName, srcDesc, dstClsNames, dstNames, dstDescs, comment);
	}

	boolean visitMethodArg(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames,
			@Nullable String[] dstMethodDescs, String[] dstNames) throws IOException;
	/**
	 * @deprecated Use {@link #visitMethodArgComment(String, String, String, int, int, String, String[], String[], String[], String[], String, CommentStyle)} instead.
	 */
	@Deprecated
	void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames,
			@Nullable String[] dstMethodDescs, @Nullable String[] dstNames,
			String comment) throws IOException;
	default void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames,
			@Nullable String[] dstMethodDescs, @Nullable String[] dstNames,
			String comment, CommentStyle style) throws IOException {
		visitMethodArgComment(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcName, dstClsNames, dstMethodNames, dstMethodDescs, dstNames, comment);
	}

	boolean visitMethodVar(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames,
			@Nullable String[] dstMethodDescs, String[] dstNames) throws IOException;
	/**
	 * @deprecated Use {@link #visitMethodVarComment(String, String, String, int, int, int, int, String, String[], String[], String[], String[], String, CommentStyle)} instead.
	 */
	void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames,
			@Nullable String[] dstMethodDescs, @Nullable String[] dstNames,
			String comment) throws IOException;
	default void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String[] dstClsNames, @Nullable String[] dstMethodNames,
			@Nullable String[] dstMethodDescs, @Nullable String[] dstNames,
			String comment, CommentStyle style) throws IOException {
		visitMethodVarComment(srcClsName, srcMethodName, srcMethodDesc, lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName, dstClsNames, dstMethodNames, dstMethodDescs, dstNames, comment);
	}

	/**
	 * Finish the visitation pass.
	 *
	 * @return {@code true} if the visitation pass is final, {@code false} if it should be started over.
	 */
	default boolean visitEnd() throws IOException {
		return true;
	}

	// regular <-> flat visitor adaptation methods

	default MappingVisitor asRegularVisitor() {
		return new FlatAsRegularMappingVisitor(this);
	}

	static FlatMappingVisitor fromRegularVisitor(MappingVisitor visitor) {
		return new RegularAsFlatMappingVisitor(visitor);
	}

	// convenience visit methods without extra dst context

	default boolean visitField(String srcClsName, String srcName, @Nullable String srcDesc, String[] dstNames) throws IOException {
		Objects.requireNonNull(dstNames);
		return visitField(srcClsName, srcName, srcDesc, null, dstNames, null);
	}

	default boolean visitMethod(String srcClsName, String srcName, @Nullable String srcDesc, String[] dstNames) throws IOException {
		Objects.requireNonNull(dstNames);
		return visitMethod(srcClsName, srcName, srcDesc, null, dstNames, null);
	}

	default boolean visitMethodArg(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName,
			String[] dstNames) throws IOException {
		Objects.requireNonNull(dstNames);
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcName,
				null, null, null,
				dstNames);
	}

	default boolean visitMethodVar(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			String[] dstNames) throws IOException {
		Objects.requireNonNull(dstNames);
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName,
				null, null, null,
				dstNames);
	}

	// convenience / potentially higher efficiency visit methods for only one dst name

	default boolean visitClass(String srcName, String dstName) throws IOException {
		return visitClass(srcName, toArray(dstName));
	}

	/**
	 * @deprecated Use {@link #visitClassComment(String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitClassComment(String srcName, String comment) throws IOException {
		visitClassComment(srcName, comment, CommentStyle.HTML);
	}

	/**
	 * @deprecated Use {@link #visitClassComment(String, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitClassComment(String srcName, @Nullable String dstName, String comment) throws IOException {
		visitClassComment(srcName, dstName, comment, CommentStyle.HTML);
	}

	default void visitClassComment(String srcName, String comment, CommentStyle style) throws IOException {
		visitClassComment(srcName, (String) null, comment, style);
	}

	default void visitClassComment(String srcName, @Nullable String dstName, String comment, CommentStyle style) throws IOException {
		visitClassComment(srcName, toArray(dstName), comment, style);
	}

	default boolean visitField(String srcClsName, String srcName, @Nullable String srcDesc,
			String dstName) throws IOException {
		return visitField(srcClsName, srcName, srcDesc,
				null, dstName, null);
	}

	default boolean visitField(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String dstClsName, String dstName, @Nullable String dstDesc) throws IOException {
		return visitField(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc));
	}

	/**
	 * @deprecated Use {@link #visitFieldComment(String, String, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			String comment) throws IOException {
		visitFieldComment(srcClsName, srcName, srcDesc, comment, CommentStyle.HTML);
	}

	/**
	 * @deprecated Use {@link #visitFieldComment(String, String, String, String, String, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String dstClsName, @Nullable String dstName, @Nullable String dstDesc,
			String comment) throws IOException {
		visitFieldComment(srcClsName, srcName, srcDesc, dstClsName, dstName, dstDesc, comment, CommentStyle.HTML);
	}

	default void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			String comment, CommentStyle style) throws IOException {
		visitFieldComment(srcClsName, srcName, srcDesc,
				(String) null, null, null,
				comment, style);
	}

	default void visitFieldComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String dstClsName, @Nullable String dstName, @Nullable String dstDesc,
			String comment, CommentStyle style) throws IOException {
		visitFieldComment(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc),
				comment, style);
	}

	default boolean visitMethod(String srcClsName, String srcName, @Nullable String srcDesc,
			String dstName) throws IOException {
		return visitMethod(srcClsName, srcName, srcDesc,
				null, dstName, null);
	}

	default boolean visitMethod(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String dstClsName, String dstName, @Nullable String dstDesc) throws IOException {
		return visitMethod(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc));
	}

	/**
	 * @deprecated Use {@link #visitMethodComment(String, String, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			String comment) throws IOException {
		visitMethodComment(srcClsName, srcName, srcDesc, comment, CommentStyle.HTML);
	}

	/**
	 * @deprecated Use {@link #visitMethodComment(String, String, String, String, String, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String dstClsName, @Nullable String dstName, @Nullable String dstDesc,
			String comment) throws IOException {
		visitMethodComment(srcClsName, srcName, srcDesc, dstClsName, dstName, dstDesc, comment, CommentStyle.HTML);
	}

	default void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			String comment, CommentStyle style) throws IOException {
		visitMethodComment(srcClsName, srcName, srcDesc,
				(String) null, null, null,
				comment, style);
	}

	default void visitMethodComment(String srcClsName, String srcName, @Nullable String srcDesc,
			@Nullable String dstClsName, @Nullable String dstName, @Nullable String dstDesc,
			String comment, CommentStyle style) throws IOException {
		visitMethodComment(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc),
				comment, style);
	}

	default boolean visitMethodArg(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName, String dstName) throws IOException {
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcName,
				null, null, null, dstName);
	}

	default boolean visitMethodArg(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String dstClsName, @Nullable String dstMethodName,
			@Nullable String dstMethodDesc, String dstName) throws IOException {
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstName));
	}

	/**
	 * @deprecated Use {@link #visitMethodArgComment(String, String, String, int, int, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName, String comment) throws IOException {
		visitMethodArgComment(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcName, comment, CommentStyle.HTML);
	}

	/**
	 * @deprecated Use {@link #visitMethodArgComment(String, String, String, int, int, String, String, String, String, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String dstClsName, @Nullable String dstMethodName,
			@Nullable String dstMethodDesc, @Nullable String dstName,
			String comment) throws IOException {
		visitMethodArgComment(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcName, dstClsName, dstMethodName, dstMethodDesc, dstName, comment, CommentStyle.HTML);
	}

	default void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName, String comment, CommentStyle style) throws IOException {
		visitMethodArgComment(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcName,
				(String) null, null, null, null,
				comment, style);
	}

	default void visitMethodArgComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int argPosition, int lvIndex, @Nullable String srcName,
			@Nullable String dstClsName, @Nullable String dstMethodName,
			@Nullable String dstMethodDesc, @Nullable String dstName,
			String comment, CommentStyle style) throws IOException {
		visitMethodArgComment(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstName),
				comment, style);
	}

	default boolean visitMethodVar(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			String dstName) throws IOException {
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName,
				null, null, null, dstName);
	}

	default boolean visitMethodVar(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String dstClsName, @Nullable String dstMethodName, @Nullable String dstMethodDesc,
			String dstName) throws IOException {
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstName));
	}

	/**
	 * @deprecated Use {@link #visitMethodVarComment(String, String, String, int, int, int, int, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			String comment) throws IOException {
		visitMethodVarComment(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName,
				comment, CommentStyle.HTML);
	}

	/**
	 * @deprecated Use {@link #visitMethodVarComment(String, String, String, int, int, int, int, String, String, String, String, String, String, CommentStyle)} instead.
	 */
	@Deprecated
	default void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String dstClsName, @Nullable String dstMethodName, @Nullable String dstMethodDesc,
			@Nullable String dstName, String comment) throws IOException {
		visitMethodVarComment(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName,
				dstClsName, dstMethodName, dstMethodDesc, dstName,
				comment, CommentStyle.HTML);
	}

	default void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			String comment, CommentStyle style) throws IOException {
		visitMethodVarComment(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName,
				(String) null, null, null, null,
				comment, style);
	}

	default void visitMethodVarComment(String srcClsName, String srcMethodName, @Nullable String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName,
			@Nullable String dstClsName, @Nullable String dstMethodName, @Nullable String dstMethodDesc,
			@Nullable String dstName, String comment, CommentStyle style) throws IOException {
		visitMethodVarComment(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstName),
				comment, style);
	}
}

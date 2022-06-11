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
import java.util.Set;

import net.fabricmc.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.mappingio.adapter.RegularAsFlatMappingVisitor;

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
	 * @return true if the header is to be visited, false otherwise
	 */
	default boolean visitHeader() throws IOException {
		return true;
	}

	void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException;

	default void visitMetadata(String key, String value) throws IOException { }

	/**
	 * Determine whether the mapping content (classes and anything below, metadata if not part of the header) should be visited.
	 *
	 * @return true if content is to be visited, false otherwise
	 */
	default boolean visitContent() throws IOException {
		return true;
	}

	boolean visitClass(String srcName, String[] dstNames) throws IOException;
	void visitClassComment(String srcName, String[] dstNames, String comment) throws IOException;

	boolean visitField(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException;
	void visitFieldComment(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs,
			String comment) throws IOException;

	boolean visitMethod(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException;
	void visitMethodComment(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs,
			String comment) throws IOException;

	boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstArgNames) throws IOException;
	void visitMethodArgComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstArgNames,
			String comment) throws IOException;

	boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstVarNames) throws IOException;
	void visitMethodVarComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstVarNames,
			String comment) throws IOException;

	/**
	 * Finish the visitation pass.
	 * @return true if the visitation pass is final, false if it should be started over
	 */
	default boolean visitEnd() throws IOException {
		return true;
	}

	// regular <-> flat visitor adaptation methods

	default MappingVisitor asMethodVisitor() {
		return new FlatAsRegularMappingVisitor(this);
	}

	static FlatMappingVisitor fromMethodVisitor(MappingVisitor visitor) {
		return new RegularAsFlatMappingVisitor(visitor);
	}

	// convenience visit methods without extra dst context

	default boolean visitField(String srcClsName, String srcName, String srcDesc, String[] dstNames) throws IOException {
		return visitField(srcClsName, srcName, srcDesc, null, dstNames, null);
	}

	default boolean visitMethod(String srcClsName, String srcName, String srcDesc, String[] dstNames) throws IOException {
		return visitMethod(srcClsName, srcName, srcDesc, null, dstNames, null);
	}

	default boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String[] dstArgNames) throws IOException {
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcArgName,
				null, null, null,
				dstArgNames);
	}

	default boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String[] dstVarNames) throws IOException {
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName,
				null, null, null,
				dstVarNames);
	}

	// convenience / potentially higher efficiency visit methods for only one dst name

	default boolean visitClass(String srcName, String dstName) throws IOException {
		return visitClass(srcName, toArray(dstName));
	}

	default void visitClassComment(String srcName, String comment) throws IOException {
		visitClassComment(srcName, (String) null, comment);
	}

	default void visitClassComment(String srcName, String dstName, String comment) throws IOException {
		visitClassComment(srcName, toArray(dstName), comment);
	}

	default boolean visitField(String srcClsName, String srcName, String srcDesc,
			String dstName) throws IOException {
		return visitField(srcClsName, srcName, srcDesc,
				null, dstName, null);
	}

	default boolean visitField(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc) throws IOException {
		return visitField(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc));
	}

	default void visitFieldComment(String srcClsName, String srcName, String srcDesc,
			String comment) throws IOException {
		visitFieldComment(srcClsName, srcName, srcDesc,
				(String) null, null, null,
				comment);
	}

	default void visitFieldComment(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc,
			String comment) throws IOException {
		visitFieldComment(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc),
				comment);
	}

	default boolean visitMethod(String srcClsName, String srcName, String srcDesc,
			String dstName) throws IOException {
		return visitMethod(srcClsName, srcName, srcDesc,
				null, dstName, null);
	}

	default boolean visitMethod(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc) throws IOException {
		return visitMethod(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc));
	}

	default void visitMethodComment(String srcClsName, String srcName, String srcDesc,
			String comment) throws IOException {
		visitMethodComment(srcClsName, srcName, srcDesc,
				(String) null, null, null,
				comment);
	}

	default void visitMethodComment(String srcClsName, String srcName, String srcDesc,
			String dstClsName, String dstName, String dstDesc,
			String comment) throws IOException {
		visitMethodComment(srcClsName, srcName, srcDesc,
				toArray(dstClsName), toArray(dstName), toArray(dstDesc),
				comment);
	}

	default boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String dstArgName) throws IOException {
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcArgName,
				null, null, null, dstArgName);
	}

	default boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstArgName) throws IOException {
		return visitMethodArg(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcArgName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstArgName));
	}

	default void visitMethodArgComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String comment) throws IOException {
		visitMethodArgComment(srcClsName, srcMethodName, srcMethodDesc,
				argPosition, lvIndex, srcArgName,
				(String) null, null, null, null,
				comment);
	}

	default void visitMethodArgComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int argPosition, int lvIndex, String srcArgName,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstArgName,
			String comment) throws IOException {
		visitMethodArgComment(srcClsName, srcMethodName, srcMethodDesc, argPosition, lvIndex, srcArgName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstArgName),
				comment);
	}

	default boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String dstVarName) throws IOException {
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName,
				null, null, null, dstVarName);
	}

	default boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstVarName) throws IOException {
		return visitMethodVar(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstVarName));
	}

	default void visitMethodVarComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String comment) throws IOException {
		visitMethodVarComment(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName,
				(String) null, null, null, null,
				comment);
	}

	default void visitMethodVarComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String dstClsName, String dstMethodName, String dstMethodDesc, String dstVarName,
			String comment) throws IOException {
		visitMethodVarComment(srcClsName, srcMethodName, srcMethodDesc,
				lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName,
				toArray(dstClsName), toArray(dstMethodName), toArray(dstMethodDesc), toArray(dstVarName),
				comment);
	}
}

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

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Visitor with order implied context and consecutive dst name visits.
 *
 * <p>The visitation order is as follows (omitting visit prefixes for brevity, lowercase for cross references):
 * <ul><li>overall: header -> content -> End -> overall
 * <li>header: Header -> Namespaces [-> Metadata]*
 * <li>content: Content [-> class|Metadata]*
 * <li>class: Class [-> DstName]* -> ElementContent [-> field|method|Comment]*
 * <li>field: Field [-> DstName|DstDesc]* -> ElementContent [-> Comment]
 * <li>method: Method [-> DstName|DstDesc]* -> ElementContent [-> arg|var|Comment]*
 * <li>arg: Arg [-> DstName]* -> ElementContent [-> Comment]
 * <li>var: Var [-> DstName]* -> ElementContent [-> Comment]
 * </ul>
 *
 * <p>The elements with a skip-return (Header/Content/Class/Field/Method/Arg/Var/ElementContent) abort processing the
 * remainder of their associated item in the above listing if requested by a {@code true} return value. For example
 * skipping in Class does neither DstName nor ElementContent, but continues with another class or End.
 *
 * <p>Returning {@code false} in End requests another complete visitation pass if the flag
 * {@link MappingFlag#NEEDS_MULTIPLE_PASSES} is provided, otherwise the behavior is unspecified. This is used for
 * visitors that first have to acquire some overall mapping knowledge before being able to perform their task.
 * Subsequent visitation passes need to use the same namespaces and data, only a new independent visitation may use
 * something else after a {@link #reset()}.
 *
 * <p>The same element may be visited more than once unless the flags contain {@link MappingFlag#NEEDS_UNIQUENESS}.
 */
public interface MappingVisitor {
	default Set<MappingFlag> getFlags() {
		return MappingFlag.NONE;
	}

	/**
	 * Reset the visitor including any chained visitors to allow for another independent visit (excluding visitEnd=false).
	 */
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

	boolean visitClass(String srcName) throws IOException;
	boolean visitField(String srcName, String srcDesc) throws IOException;
	boolean visitMethod(String srcName, String srcDesc) throws IOException;
	boolean visitMethodArg(int argPosition, int lvIndex, String srcName) throws IOException;
	boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException;

	/**
	 * Finish the visitation pass.
	 * @return true if the visitation pass is final, false if it should be started over
	 */
	default boolean visitEnd() throws IOException {
		return true;
	}

	/**
	 * Destination name for the current element.
	 *
	 * @param namespace namespace index, index into the dstNamespaces List in {@link #visitNamespaces}
	 * @param name destination name
	 */
	void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException;

	default void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException { }

	/**
	 * Determine whether the element content (comment, sub-elements) should be visited.
	 *
	 * <p>Called after visiting the target itself (e.g. visitClass for targetKind=class), its dst names and descs, but
	 * before any child elements or the comment.
	 *
	 * <p>This is also a notification about all available dst names having been passed on.
	 *
	 * @return true if the contents are to be visited, false otherwise
	 */
	default boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		return true;
	}

	/**
	 * Comment for the specified element (last content-visited or any parent).
	 *
	 * @param comment comment as a potentially multi-line string
	 */
	void visitComment(MappedElementKind targetKind, String comment) throws IOException;
}

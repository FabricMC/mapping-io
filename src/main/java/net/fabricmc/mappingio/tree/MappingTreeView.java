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

package net.fabricmc.mappingio.tree;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import net.fabricmc.mappingio.MappingVisitor;

public interface MappingTreeView {
	String getSrcNamespace();
	List<String> getDstNamespaces();

	int getMaxNamespaceId();
	int getMinNamespaceId();

	int getNamespaceId(String name);
	String getNamespaceName(int id);

	Collection<Entry<String, String>> getMetadata();
	String getMetadata(String key);

	Collection<? extends ClassMappingView> getClasses();
	ClassMappingView getClass(String srcName);
	ClassMappingView getClass(String name, int namespace);

	/**
	 * @see MappingTreeView#getField(String, String, String, int)
	 */
	FieldMappingView getField(String srcOwnerName, String srcName, String srcDesc);

	/**
	 * @param desc Nullable.
	 */
	FieldMappingView getField(String ownerName, String name, String desc, int namespace);

	/**
	 * @see MappingTreeView#getMethod(String, String, String, int)
	 */
	MethodMappingView getMethod(String srcOwnerName, String srcName, String srcDesc);

	/**
	 * @param desc Nullable. Can be either complete desc or parameter-only desc.
	 */
	MethodMappingView getMethod(String ownerName, String name, String desc, int namespace);

	void accept(MappingVisitor visitor) throws IOException;

	String mapClassName(String name, int namespace);
	String mapClassName(String name, int srcNamespace, int dstNamespace);

	String mapDesc(CharSequence desc, int namespace);
	String mapDesc(CharSequence desc, int srcNamespace, int dstNamespace);
	String mapDesc(CharSequence desc, int start, int end, int namespace);
	String mapDesc(CharSequence desc, int start, int end, int srcNamespace, int dstNamespace);

	interface ElementMappingView {
		MappingTreeView getTree();

		String getSrcName();
		String getDstName(int namespace);

		default String getName(int namespace) {
			if (namespace < 0) {
				return getSrcName();
			} else {
				return getDstName(namespace);
			}
		}

		default String getName(String namespace) {
			int nsId = getTree().getNamespaceId(namespace);

			if (nsId == NULL_NAMESPACE_ID) {
				return null;
			} else {
				return getName(nsId);
			}
		}

		String getComment();
	}

	interface ClassMappingView extends ElementMappingView {
		Collection<? extends FieldMappingView> getFields();

		/**
		 * @see MappingTreeView#getField(String, String, String, int)
		 */
		FieldMappingView getField(String srcName, String srcDesc);

		/**
		 * @see MappingTreeView#getField(String, String, String, int)
		 */
		FieldMappingView getField(String name, String desc, int namespace);

		Collection<? extends MethodMappingView> getMethods();

		/**
		 * @see MappingTreeView#getMethod(String, String, String, int)
		 */
		MethodMappingView getMethod(String srcName, String srcDesc);

		/**
		 * @see MappingTreeView#getMethod(String, String, String, int)
		 */
		MethodMappingView getMethod(String name, String desc, int namespace);
	}

	interface MemberMappingView extends ElementMappingView {
		ClassMappingView getOwner();
		String getSrcDesc();

		/**
		 * @deprecated Please use {@link MemberMappingView#getDesc(int)}.
		 */
		default String getDstDesc(int namespace) {
			return getTree().mapDesc(getSrcDesc(), namespace);
		}

		default String getDesc(int namespace) {
			if (namespace < 0) {
				return getSrcDesc();
			} else {
				return getTree().mapDesc(getSrcDesc(), namespace);
			}
		}

		default String getDesc(String namespace) {
			int nsId = getTree().getNamespaceId(namespace);

			if (nsId == NULL_NAMESPACE_ID) {
				return null;
			} else {
				return getDesc(nsId);
			}
		}
	}

	interface FieldMappingView extends MemberMappingView { }

	interface MethodMappingView extends MemberMappingView {
		Collection<? extends MethodArgMappingView> getArgs();
		MethodArgMappingView getArg(int argPosition, int lvIndex, String srcName);

		Collection<? extends MethodVarMappingView> getVars();
		MethodVarMappingView getVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName);
	}

	interface MethodArgMappingView extends ElementMappingView {
		MethodMappingView getMethod();
		int getArgPosition();
		int getLvIndex();
	}

	interface MethodVarMappingView extends ElementMappingView {
		MethodMappingView getMethod();
		int getLvtRowIndex();
		int getLvIndex();
		int getStartOpIdx();
	}

	int SRC_NAMESPACE_ID = -1;
	int MIN_NAMESPACE_ID = SRC_NAMESPACE_ID;
	int NULL_NAMESPACE_ID = -2;
}

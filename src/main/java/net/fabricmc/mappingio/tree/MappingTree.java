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

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Mutable mapping tree.
 *
 * <p>All returned collections are to be assumed unmodifiable, unless explicitly stated otherwise.
 */
public interface MappingTree extends MappingTreeView {
	/**
	 * Sets the tree's and all of its contained elements' source namespace.
	 *
	 * @return The previous source namespace, if present.
	 */
	@Nullable
	String setSrcNamespace(String namespace);

	/**
	 * @return The previous destination namespaces.
	 */
	List<String> setDstNamespaces(List<String> namespaces);

	/**
	 * @return A modifiable list of all metadata entries currently present in the tree.
	 * The list's order is equal to the order in which the entries have been originally added.
	 */
	@Override
	List<? extends MetadataEntry> getMetadata();

	/**
	 * @return An unmodifiable list of all currently present metadata entries whose key is equal to the passed one.
	 * The list's order is equal to the order in which the entries have been originally added.
	 */
	@Override
	List<? extends MetadataEntry> getMetadata(String key);

	void addMetadata(MetadataEntry entry);

	/**
	 * Removes all metadata entries whose key is equal to the passed one.
	 *
	 * @return Whether or not any entries have been removed.
	 */
	boolean removeMetadata(String key);

	@Override
	Collection<? extends ClassMapping> getClasses();
	@Override
	@Nullable
	ClassMapping getClass(String srcName);

	@Override
	@Nullable
	default ClassMapping getClass(String name, int namespace) {
		return (ClassMapping) MappingTreeView.super.getClass(name, namespace);
	}

	/**
	 * Merges a class mapping into the tree.
	 *
	 * @return The {@link ClassMapping} instance present in the tree after the merge has occurred.
	 * May or may not be the passed instance.
	 */
	ClassMapping addClass(ClassMapping cls);

	/**
	 * Removes a class mapping from the tree.
	 *
	 * @return The removed class mapping, if any.
	 */
	@Nullable
	ClassMapping removeClass(String srcName);

	@Override
	@Nullable
	default FieldMapping getField(String srcClsName, String srcName, @Nullable String srcDesc) {
		return (FieldMapping) MappingTreeView.super.getField(srcClsName, srcName, srcDesc);
	}

	@Override
	@Nullable
	default FieldMapping getField(String clsName, String name, @Nullable String desc, int namespace) {
		return (FieldMapping) MappingTreeView.super.getField(clsName, name, desc, namespace);
	}

	@Override
	@Nullable
	default MethodMapping getMethod(String srcClsName, String srcName, @Nullable String srcDesc) {
		return (MethodMapping) MappingTreeView.super.getMethod(srcClsName, srcName, srcDesc);
	}

	@Override
	@Nullable
	default MethodMapping getMethod(String clsName, String name, @Nullable String desc, int namespace) {
		return (MethodMapping) MappingTreeView.super.getMethod(clsName, name, desc, namespace);
	}

	interface MetadataEntry extends MetadataEntryView {
	}

	interface ElementMapping extends ElementMappingView {
		@Override
		MappingTree getTree();

		void setDstName(String name, int namespace);
		void setComment(String comment);
	}

	interface ClassMapping extends ElementMapping, ClassMappingView {
		@Override
		Collection<? extends FieldMapping> getFields();
		@Override
		@Nullable
		FieldMapping getField(String srcName, @Nullable String srcDesc);

		@Override
		@Nullable
		default FieldMapping getField(String name, @Nullable String desc, int namespace) {
			return (FieldMapping) ClassMappingView.super.getField(name, desc, namespace);
		}

		/**
		 * Merges a field mapping into the class.
		 *
		 * @return The {@link FieldMapping} instance present in the parent {@link ClassMapping} after the merge has occurred.
		 * May or may not be the passed instance.
		 */
		FieldMapping addField(FieldMapping field);

		/**
		 * Removes a field mapping from the class.
		 *
		 * @return The removed field mapping, if any.
		 */
		@Nullable
		FieldMapping removeField(String srcName, @Nullable String srcDesc);

		@Override
		Collection<? extends MethodMapping> getMethods();
		@Override
		@Nullable
		MethodMapping getMethod(String srcName, @Nullable String srcDesc);

		@Override
		@Nullable
		default MethodMapping getMethod(String name, @Nullable String desc, int namespace) {
			return (MethodMapping) ClassMappingView.super.getMethod(name, desc, namespace);
		}

		/**
		 * Merges a method mapping into the class.
		 *
		 * @return The {@link MethodMapping} instance present in the parent {@link ClassMapping} after the merge has occurred.
		 * May or may not be the passed instance.
		 */
		MethodMapping addMethod(MethodMapping method);

		/**
		 * Removes a method mapping from the class.
		 *
		 * @return The removed method mapping, if any.
		 */
		@Nullable
		MethodMapping removeMethod(String srcName, @Nullable String srcDesc);
	}

	interface MemberMapping extends ElementMapping, MemberMappingView {
		@Override
		ClassMapping getOwner();
		void setSrcDesc(String desc);
	}

	interface FieldMapping extends MemberMapping, FieldMappingView { }

	interface MethodMapping extends MemberMapping, MethodMappingView {
		@Override
		Collection<? extends MethodArgMapping> getArgs();
		@Override
		@Nullable
		MethodArgMapping getArg(int argPosition, int lvIndex, @Nullable String srcName);
		MethodArgMapping addArg(MethodArgMapping arg);

		/**
		 * Removes an argument mapping from the method.
		 *
		 * @return The removed argument mapping, if any.
		 */
		@Nullable
		MethodArgMapping removeArg(int argPosition, int lvIndex, @Nullable String srcName);

		@Override
		Collection<? extends MethodVarMapping> getVars();
		@Override
		@Nullable
		MethodVarMapping getVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName);

		/**
		 * Merges a variable mapping into the method.
		 *
		 * @return The {@link MethodVarMapping} instance present in the parent {@link MethodMapping} after the merge has occurred.
		 * May or may not be the passed instance.
		 */
		MethodVarMapping addVar(MethodVarMapping var);

		/**
		 * Removes a variable mapping from the method.
		 *
		 * @return The removed variable mapping, if any.
		 */
		@Nullable
		MethodVarMapping removeVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName);
	}

	interface MethodArgMapping extends ElementMapping, MethodArgMappingView {
		@Override
		MethodMapping getMethod();
		void setArgPosition(int position);
		void setLvIndex(int index);
	}

	interface MethodVarMapping extends ElementMapping, MethodVarMappingView {
		@Override
		MethodMapping getMethod();
		void setLvtRowIndex(int index);
		void setLvIndex(int lvIndex, int startOpIdx, int endOpIdx);
	}
}

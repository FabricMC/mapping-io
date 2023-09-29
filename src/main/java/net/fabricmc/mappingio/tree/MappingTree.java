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

public interface MappingTree extends MappingTreeView {
	String setSrcNamespace(String namespace);
	List<String> setDstNamespaces(List<String> namespaces);

	/**
	 * @return A modifiable list of all metadata entries currently present in the tree.
	 * The list's order is equal to the order in which the entries have been originally added.
	 */
	@Override
	List<? extends MetadataEntryView> getMetadata();

	/**
	 * @return An unmodifiable list of all metadata entries currently present
	 * in the tree whose key is equal to the passed one.
	 * The list's order is equal to the order in which the entries have been originally added.
	 */
	@Override
	List<? extends MetadataEntryView> getMetadata(String key);

	void addMetadata(MetadataEntryView entry);

	/**
	 * Removes all metadata entries whose key is equal to the passed one.
	 * @return Whether or not any entries have been removed.
	 */
	boolean removeMetadata(String key);

	@Override
	Collection<? extends ClassMapping> getClasses();
	@Override
	ClassMapping getClass(String srcName);

	@Override
	default ClassMapping getClass(String name, int namespace) {
		return (ClassMapping) MappingTreeView.super.getClass(name, namespace);
	}

	ClassMapping addClass(ClassMapping cls);
	ClassMapping removeClass(String srcName);

	@Override
	default FieldMapping getField(String srcOwnerName, String srcName, String srcDesc) {
		return (FieldMapping) MappingTreeView.super.getField(srcOwnerName, srcName, srcDesc);
	}

	@Override
	default FieldMapping getField(String ownerName, String name, String desc, int namespace) {
		return (FieldMapping) MappingTreeView.super.getField(ownerName, name, desc, namespace);
	}

	@Override
	default MethodMapping getMethod(String srcOwnerName, String srcName, String srcDesc) {
		return (MethodMapping) MappingTreeView.super.getMethod(srcOwnerName, srcName, srcDesc);
	}

	@Override
	default MethodMapping getMethod(String ownerName, String name, String desc, int namespace) {
		return (MethodMapping) MappingTreeView.super.getMethod(ownerName, name, desc, namespace);
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
		FieldMapping getField(String srcName, String srcDesc);

		@Override
		default FieldMapping getField(String name, String desc, int namespace) {
			return (FieldMapping) ClassMappingView.super.getField(name, desc, namespace);
		}

		FieldMapping addField(FieldMapping field);
		FieldMapping removeField(String srcName, String srcDesc);

		@Override
		Collection<? extends MethodMapping> getMethods();
		@Override
		MethodMapping getMethod(String srcName, String srcDesc);

		@Override
		default MethodMapping getMethod(String name, String desc, int namespace) {
			return (MethodMapping) ClassMappingView.super.getMethod(name, desc, namespace);
		}

		MethodMapping addMethod(MethodMapping method);
		MethodMapping removeMethod(String srcName, String srcDesc);
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
		MethodArgMapping getArg(int argPosition, int lvIndex, String srcName);
		MethodArgMapping addArg(MethodArgMapping arg);
		MethodArgMapping removeArg(int argPosition, int lvIndex, String srcName);

		@Override
		Collection<? extends MethodVarMapping> getVars();
		@Override
		MethodVarMapping getVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName);
		MethodVarMapping addVar(MethodVarMapping var);
		MethodVarMapping removeVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName);
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

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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.CommentStyle;
import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.adapter.OuterClassNamePropagator;

/**
 * Mutable mapping tree.
 *
 * <p>All returned collections are to be assumed unmodifiable, unless explicitly stated otherwise.
 */
public interface MappingTree extends MappingTreeView {
	/**
	 * Sets the tree's and all of its contained elements' source namespace name.
	 *
	 * <p>If the passed namespace name equals an existing destination namespace's name,
	 * implementors may choose to switch the two namespaces around, analogous to {@link MappingSourceNsSwitch}.
	 * This has to be made clear in the implementation's documentation.
	 *
	 * @implSpec If switching with an existing destination namespace is requested, but not supported, an {@link UnsupportedOperationException} must be thrown.
	 *
	 * @return The previous source namespace name, if present.
	 */
	@Nullable
	String setSrcNamespace(String namespace);

	/**
	 * Sets the tree's and all of its contained elements' destination namespace names.
	 *
	 * <p>Can be used to reorder and/or drop destination namespaces, analogous to {@link MappingDstNsReorder}.
	 *
	 * <p>Implementors may allow switching with the source namespace as well, analogous to {@link MappingSourceNsSwitch}.
	 * This has to be made clear in the implementation's documentation.
	 *
	 * @implSpec If switching with the source namespace is requested, but not supported, an {@link UnsupportedOperationException} must be thrown.
	 *
	 * @return The previous destination namespaces.
	 * @throws IllegalArgumentException If the passed namespace names contain duplicates.
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
	 * @return Whether any entries have been removed.
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

	/**
	 * Searches for inner classes whose effective destination name contains outer classes referenced via their source name,
	 * scans the tree for potential mappings for these enclosing classes, and applies the latters' destination names
	 * to the formers' fully qualified name.
	 *
	 * <p>For example, it takes a class {@code class_1$class_2} that doesn't have a mapping,
	 * tries to find {@code class_1}, which let's say has the mapping {@code SomeClass},
	 * and changes the former's destination name to {@code SomeClass$class_2}.
	 *
	 * <p>Equivalent of {@link OuterClassNamePropagator}, but more efficient
	 * since the tree's existing class map can be reused.
	 *
	 * @param processRemappedDstNames Whether already remapped destination names should also get their unmapped outer classes replaced.
	 */
	default void propagateOuterClassNames(boolean processRemappedDstNames) {
		propagateOuterClassNames(getSrcNamespace(), getDstNamespaces(), processRemappedDstNames);
	}

	/**
	 * Searches for inner classes whose effective destination name contains outer classes referenced via their source name,
	 * scans the tree for potential mappings for these enclosing classes, and applies the latters' destination names
	 * to the formers' fully qualified name.
	 *
	 * <p>For example, it takes a class {@code class_1$class_2} that doesn't have a mapping,
	 * tries to find {@code class_1}, which let's say has the mapping {@code SomeClass},
	 * and changes the former's destination name to {@code SomeClass$class_2}.
	 *
	 * <p>Equivalent of {@link OuterClassNamePropagator}, but more efficient
	 * since the tree's existing class map can be reused.
	 *
	 * @param srcNamespace The namespace where the original/unmapped outer class names originate from.
	 * @param dstNamespaces The namespaces where outer class names shall be propagated.
	 * @param processRemappedDstNames Whether already remapped destination names should also get their unmapped outer classes replaced.
	 */
	default void propagateOuterClassNames(String srcNamespace, Collection<String> dstNamespaces, boolean processRemappedDstNames) {
		int srcNsId = getNamespaceId(srcNamespace);
		if (srcNsId == NULL_NAMESPACE_ID) throw new IllegalArgumentException("Namespace " + srcNsId + " does not exist");

		for (ClassMapping cls : getClasses()) {
			for (String dstNs : dstNamespaces) {
				int dstNsId = getNamespaceId(dstNs);
				if (dstNsId == SRC_NAMESPACE_ID) throw new UnsupportedOperationException("Cannot change source names");
				if (dstNsId == NULL_NAMESPACE_ID) throw new IllegalArgumentException("Destination namespace " + dstNs + " does not exist");
				if (dstNsId == srcNsId) continue;

				String srcName = cls.getName(srcNsId);
				if (srcName == null) continue;
				String dstName = cls.getName(dstNsId);

				int idx = srcName.lastIndexOf('$');
				if (idx == -1) continue;

				if (!processRemappedDstNames && dstName != null && !dstName.equals(srcName)) {
					continue;
				}

				String[] srcParts = srcName.split(Pattern.quote("$"));
				String[] dstParts = dstName == null ? srcParts : dstName.split(Pattern.quote("$"));
				assert dstParts.length == srcParts.length;

				for (int pos = srcParts.length - 2; pos >= 0; pos--) {
					String outerSrcName = String.join("$", Arrays.copyOfRange(srcParts, 0, pos + 1));

					if (dstName != null && !dstParts[pos].equals(srcParts[pos])) {
						// That part already has a different mapping
						continue;
					}

					ClassMapping outerCls = getClass(outerSrcName, srcNsId);
					String outerDstName;

					if (outerCls != null && (outerDstName = outerCls.getName(dstNsId)) != null && !outerDstName.equals(outerSrcName)) {
						cls.setDstName(outerDstName + "$" + String.join("$", Arrays.copyOfRange(dstParts, pos + 1, dstParts.length)), dstNsId);
						break;
					}
				}
			}
		}
	}

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

		/**
		 * Sets the HTML-style comment of this mapping.
		 * @param comment The comment.
		 */
		void setComment(String comment);

		/**
		 * Sets the comment of this mapping.
		 * @param comment The comment.
		 * @param style   The comment style.
		 */
		default void setComment(String comment, CommentStyle style) {
			setComment(comment);
		}
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

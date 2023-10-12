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

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappingVisitor;

public interface MappingTreeView {
	String getSrcNamespace();
	List<String> getDstNamespaces();

	/**
	 * Get the maximum available namespace ID (exclusive).
	 */
	default int getMaxNamespaceId() {
		return getDstNamespaces().size();
	}

	/**
	 * Get the minimum available namespace ID (inclusive).
	 */
	default int getMinNamespaceId() {
		return MIN_NAMESPACE_ID;
	}

	default int getNamespaceId(String namespace) {
		if (namespace.equals(getSrcNamespace())) {
			return SRC_NAMESPACE_ID;
		}

		int ret = getDstNamespaces().indexOf(namespace);

		return ret >= 0 ? ret : NULL_NAMESPACE_ID;
	}

	default String getNamespaceName(int id) {
		if (id < 0) return getSrcNamespace();

		return getDstNamespaces().get(id);
	}

	List<? extends MetadataEntryView> getMetadata();
	List<? extends MetadataEntryView> getMetadata(String key);

	Collection<? extends ClassMappingView> getClasses();
	ClassMappingView getClass(String srcName);
	default ClassMappingView getClass(String name, int namespace) {
		if (namespace < 0) return getClass(name);

		for (ClassMappingView cls : getClasses()) {
			if (name.equals(cls.getDstName(namespace))) return cls;
		}

		return null;
	}

	/**
	 * @see MappingTreeView#getField(String, String, String, int)
	 */
	default FieldMappingView getField(String srcClsName, String srcName, @Nullable String srcDesc) {
		ClassMappingView owner = getClass(srcClsName);
		return owner != null ? owner.getField(srcName, srcDesc) : null;
	}

	default FieldMappingView getField(String clsName, String name, @Nullable String desc, int namespace) {
		ClassMappingView owner = getClass(clsName, namespace);
		return owner != null ? owner.getField(name, desc, namespace) : null;
	}

	/**
	 * @see MappingTreeView#getMethod(String, String, String, int)
	 */
	default MethodMappingView getMethod(String srcClsName, String srcName, @Nullable String srcDesc) {
		ClassMappingView owner = getClass(srcClsName);
		return owner != null ? owner.getMethod(srcName, srcDesc) : null;
	}

	/**
	 * @param desc Can be either complete desc or parameter-only desc.
	 */
	default MethodMappingView getMethod(String clsName, String name, @Nullable String desc, int namespace) {
		ClassMappingView owner = getClass(clsName, namespace);
		return owner != null ? owner.getMethod(name, desc, namespace) : null;
	}

	default void accept(MappingVisitor visitor) throws IOException {
		accept(visitor, VisitOrder.createByInputOrder());
	}

	void accept(MappingVisitor visitor, VisitOrder order) throws IOException;

	default String mapClassName(String name, int namespace) {
		return mapClassName(name, SRC_NAMESPACE_ID, namespace);
	}

	default String mapClassName(String name, int srcNamespace, int dstNamespace) {
		assert name.indexOf('.') < 0;

		if (srcNamespace == dstNamespace) return name;

		ClassMappingView cls = getClass(name, srcNamespace);
		if (cls == null) return name;

		String ret = cls.getName(dstNamespace);

		return ret != null ? ret : name;
	}

	default String mapDesc(CharSequence desc, int namespace) {
		return mapDesc(desc, 0, desc.length(), SRC_NAMESPACE_ID, namespace);
	}

	default String mapDesc(CharSequence desc, int srcNamespace, int dstNamespace) {
		return mapDesc(desc, 0, desc.length(), srcNamespace, dstNamespace);
	}

	default String mapDesc(CharSequence desc, int start, int end, int namespace) {
		return mapDesc(desc, start, end, SRC_NAMESPACE_ID, namespace);
	}

	default String mapDesc(CharSequence desc, int start, int end, int srcNamespace, int dstNamespace) {
		if (srcNamespace == dstNamespace) return desc.subSequence(start, end).toString();

		StringBuilder ret = null;
		int copyOffset = start;
		int offset = start;

		while (offset < end) {
			char c = desc.charAt(offset++);

			if (c == 'L') {
				int idEnd = offset; // current identifier end, exclusive

				while (idEnd < end) {
					c = desc.charAt(idEnd);
					if (c == ';') break;
					idEnd++;
				}

				if (idEnd >= end) throw new IllegalArgumentException("invalid descriptor: "+desc.subSequence(start, end));

				String cls = desc.subSequence(offset, idEnd).toString();
				String mappedCls = mapClassName(cls, srcNamespace, dstNamespace);

				if (mappedCls != null && !mappedCls.equals(cls)) {
					if (ret == null) ret = new StringBuilder(end - start);

					ret.append(desc, copyOffset, offset);
					ret.append(mappedCls);
					copyOffset = idEnd;
				}

				offset = idEnd + 1;
			}
		}

		if (ret == null) return desc.subSequence(start, end).toString();

		ret.append(desc, copyOffset, end);

		return ret.toString();
	}

	interface MetadataEntryView {
		String getKey();
		String getValue();
	}

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
		FieldMappingView getField(String srcName, @Nullable String srcDesc);

		/**
		 * @see MappingTreeView#getField(String, String, String, int)
		 */
		default FieldMappingView getField(String name, @Nullable String desc, int namespace) {
			if (namespace < 0) return getField(name, desc);

			for (FieldMappingView field : getFields()) {
				if (!name.equals(field.getDstName(namespace))) continue;
				String mDesc;
				if (desc != null && (mDesc = field.getDesc(namespace)) != null && !desc.equals(mDesc)) continue;

				return field;
			}

			return null;
		}

		Collection<? extends MethodMappingView> getMethods();

		/**
		 * @see MappingTreeView#getMethod(String, String, String, int)
		 */
		MethodMappingView getMethod(String srcName, @Nullable String srcDesc);

		/**
		 * @see MappingTreeView#getMethod(String, String, String, int)
		 */
		default MethodMappingView getMethod(String name, @Nullable String desc, int namespace) {
			if (namespace < 0) return getMethod(name, desc);

			for (MethodMappingView method : getMethods()) {
				if (!name.equals(method.getDstName(namespace))) continue;

				String mDesc;
				if (desc != null && (mDesc = method.getDesc(namespace)) != null && !desc.equals(mDesc) && !(desc.endsWith(")") && mDesc.startsWith(desc))) continue;

				return method;
			}

			return null;
		}
	}

	interface MemberMappingView extends ElementMappingView {
		ClassMappingView getOwner();
		String getSrcDesc();

		default String getDstDesc(int namespace) {
			String srcDesc = getSrcDesc();

			return srcDesc != null ? getTree().mapDesc(srcDesc, namespace) : null;
		}

		default String getDesc(int namespace) {
			String srcDesc = getSrcDesc();

			if (namespace < 0 || srcDesc == null) {
				return srcDesc;
			} else {
				return getTree().mapDesc(srcDesc, namespace);
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
		MethodArgMappingView getArg(int argPosition, int lvIndex, @Nullable String srcName);

		Collection<? extends MethodVarMappingView> getVars();
		MethodVarMappingView getVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName);
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
		int getEndOpIdx();
	}

	int SRC_NAMESPACE_ID = -1;
	int MIN_NAMESPACE_ID = SRC_NAMESPACE_ID;
	int NULL_NAMESPACE_ID = -2;
}

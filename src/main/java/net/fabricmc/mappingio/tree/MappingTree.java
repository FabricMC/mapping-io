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

	default int getMaxNamespaceId() {
		return getDstNamespaces().size();
	}
	default int getMinNamespaceId() {
		return MappingTreeView.MIN_NAMESPACE_ID;
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

	void addMetadata(String key, String value);
	String removeMetadata(String key);

	Collection<? extends ClassMapping> getClasses();
	ClassMapping getClass(String srcName);

	default ClassMapping getClass(String name, int namespace) {
		if (namespace < 0) return getClass(name);

		for (ClassMapping cls : getClasses()) {
			if (name.equals(cls.getDstName(namespace))) return cls;
		}

		return null;
	}

	ClassMapping addClass(ClassMapping cls);
	ClassMapping removeClass(String srcName);

	default FieldMapping getField(String srcOwnerName, String srcName, String srcDesc) {
		ClassMapping owner = getClass(srcOwnerName);

		return owner != null ? owner.getField(srcName, srcDesc) : null;
	}

	default FieldMapping getField(String ownerName, String name, String desc, int namespace) {
		ClassMapping owner = getClass(ownerName, namespace);

		return owner != null ? owner.getField(name, desc, namespace) : null;
	}

	default MethodMapping getMethod(String srcOwnerName, String srcName, String srcDesc) {
		ClassMapping owner = getClass(srcOwnerName);

		return owner != null ? owner.getMethod(srcName, srcDesc) : null;
	}

	default MethodMapping getMethod(String ownerName, String name, String desc, int namespace) {
		ClassMapping owner = getClass(ownerName, namespace);

		return owner != null ? owner.getMethod(name, desc, namespace) : null;
	}

	default String mapClassName(String name, int namespace) {
		return mapClassName(name, SRC_NAMESPACE_ID, namespace);
	}

	default String mapClassName(String name, int srcNamespace, int dstNamespace) {
		assert name.indexOf('.') < 0;

		if (srcNamespace == dstNamespace) return name;

		ClassMapping cls = getClass(name, srcNamespace);
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

	interface ElementMapping extends ElementMappingView {
		MappingTree getTree();

		void setDstName(int namespace, String name);
		void setComment(String comment);
	}

	interface ClassMapping extends ElementMapping, ClassMappingView {
		Collection<? extends FieldMapping> getFields();
		FieldMapping getField(String srcName, String srcDesc);

		default FieldMapping getField(String name, String desc, int namespace) {
			if (namespace < 0) return getField(name, desc);

			for (FieldMapping field : getFields()) {
				if (!name.equals(field.getDstName(namespace))) continue;
				String mDesc;
				if (desc != null && (mDesc = field.getDesc(namespace)) != null && !desc.equals(mDesc)) continue;

				return field;
			}

			return null;
		}

		FieldMapping addField(FieldMapping field);
		FieldMapping removeField(String srcName, String srcDesc);

		Collection<? extends MethodMapping> getMethods();
		MethodMapping getMethod(String srcName, String srcDesc);

		default MethodMapping getMethod(String name, String desc, int namespace) {
			if (namespace < 0) return getMethod(name, desc);

			for (MethodMapping method : getMethods()) {
				if (!name.equals(method.getDstName(namespace))) continue;
				String mDesc;
				if (desc != null && (mDesc = method.getDesc(namespace)) != null && !desc.equals(mDesc)) continue;

				return method;
			}

			return null;
		}

		MethodMapping addMethod(MethodMapping method);
		MethodMapping removeMethod(String srcName, String srcDesc);
	}

	interface MemberMapping extends ElementMapping, MemberMappingView {
		ClassMapping getOwner();
	}

	interface FieldMapping extends MemberMapping, FieldMappingView { }

	interface MethodMapping extends MemberMapping, MethodMappingView {
		Collection<? extends MethodArgMapping> getArgs();
		MethodArgMapping getArg(int argPosition, int lvIndex, String srcName);
		MethodArgMapping addArg(MethodArgMapping arg);
		MethodArgMapping removeArg(int argPosition, int lvIndex, String srcName);

		Collection<? extends MethodVarMapping> getVars();
		MethodVarMapping getVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName);
		MethodVarMapping addVar(MethodVarMapping var);
		MethodVarMapping removeVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName);
	}

	interface MethodArgMapping extends ElementMapping, MethodArgMappingView {
		MethodMapping getMethod();
	}

	interface MethodVarMapping extends ElementMapping, MethodVarMappingView {
		MethodMapping getMethod();
	}
}

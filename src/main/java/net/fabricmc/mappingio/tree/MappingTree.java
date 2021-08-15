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
import java.util.Map;

import net.fabricmc.mappingio.MappingVisitor;

public interface MappingTree {
	String getSrcNamespace();
	String setSrcNamespace(String namespace);
	List<String> getDstNamespaces();
	List<String> setDstNamespaces(List<String> namespaces);

	default int getNamespaceId(String namespace) {
		if (namespace.equals(getSrcNamespace())) {
			return SRC_NAMESPACE_ID;
		}

		int ret = getDstNamespaces().indexOf(namespace);

		return ret >= 0 ? ret : NULL_NAMESPACE_ID;
	}

	/**
	 * Determine the maximum available namespace ID (exclusive).
	 */
	default int getMaxNamespaceId() {
		return getDstNamespaces().size();
	}

	default String getNamespaceName(int id) {
		if (id < 0) return getSrcNamespace();

		return getDstNamespaces().get(id);
	}

	Collection<Map.Entry<String, String>> getMetadata();
	String getMetadata(String key);
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

	default MethodMapping getMethod(String srcOwnerName, String srcName, String srcDesc) {
		ClassMapping owner = getClass(srcOwnerName);

		return owner != null ? owner.getMethod(srcName, srcDesc) : null;
	}

	void accept(MappingVisitor visitor) throws IOException;

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

	public interface ElementMapping {
		MappingTree getTree();

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

		void setDstName(int namespace, String name);
		String getComment();
		void setComment(String comment);
	}

	public interface ClassMapping extends ElementMapping {
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
				if (desc != null && (mDesc = method.getDesc(namespace)) != null && !desc.equals(mDesc)
						&& !(desc.endsWith(")") && mDesc.startsWith(desc))) continue;

				return method;
			}

			return null;
		}

		MethodMapping addMethod(MethodMapping method);
		MethodMapping removeMethod(String srcName, String srcDesc);
	}

	public interface MemberMapping extends ElementMapping {
		ClassMapping getOwner();
		String getSrcDesc();

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

	public interface FieldMapping extends MemberMapping { }

	public interface MethodMapping extends MemberMapping {
		Collection<? extends MethodArgMapping> getArgs();
		MethodArgMapping getArg(int argPosition, int lvIndex, String srcName);
		MethodArgMapping addArg(MethodArgMapping arg);
		MethodArgMapping removeArg(int argPosition, int lvIndex, String srcName);

		Collection<? extends MethodVarMapping> getVars();
		MethodVarMapping getVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName);
		MethodVarMapping addVar(MethodVarMapping var);
		MethodVarMapping removeVar(int lvtRowIndex, int lvIndex, int startOpIdx, String srcName);
	}

	public interface MethodArgMapping extends ElementMapping {
		MethodMapping getMethod();
		int getArgPosition();
		int getLvIndex();
	}

	public interface MethodVarMapping extends ElementMapping {
		MethodMapping getMethod();
		int getLvtRowIndex();
		int getLvIndex();
		int getStartOpIdx();
	}

	int SRC_NAMESPACE_ID = -1;
	int MIN_NAMESPACE_ID = SRC_NAMESPACE_ID;
	int NULL_NAMESPACE_ID = -2;
}

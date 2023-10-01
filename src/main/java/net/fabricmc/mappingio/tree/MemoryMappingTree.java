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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;

public final class MemoryMappingTree implements VisitableMappingTree {
	public MemoryMappingTree() {
		this(false);
	}

	public MemoryMappingTree(boolean indexByDstNames) {
		this.indexByDstNames = indexByDstNames;
	}

	public MemoryMappingTree(MappingTree src) {
		if (src instanceof MemoryMappingTree) {
			indexByDstNames = ((MemoryMappingTree) src).indexByDstNames;
		}

		setSrcNamespace(src.getSrcNamespace());
		setDstNamespaces(src.getDstNamespaces());

		for (MetadataEntry entry : src.getMetadata()) {
			addMetadata(entry);
		}

		for (ClassMapping cls : src.getClasses()) {
			addClass(cls);
		}
	}

	public void setIndexByDstNames(boolean indexByDstNames) {
		if (indexByDstNames == this.indexByDstNames) return;

		if (!indexByDstNames) {
			classesByDstNames = null;
		} else if (dstNamespaces != null) {
			initClassesByDstNames();
		}

		this.indexByDstNames = indexByDstNames;
	}

	@SuppressWarnings("unchecked")
	private void initClassesByDstNames() {
		classesByDstNames = new Map[dstNamespaces.size()];

		for (int i = 0; i < classesByDstNames.length; i++) {
			classesByDstNames[i] = new HashMap<String, ClassEntry>(classesBySrcName.size());
		}

		for (ClassEntry cls : classesBySrcName.values()) {
			for (int i = 0; i < cls.dstNames.length; i++) {
				String dstName = cls.dstNames[i];
				if (dstName != null) classesByDstNames[i].put(dstName, cls);
			}
		}
	}

	public void setHierarchyInfoProvider(HierarchyInfoProvider<?> provider) {
		hierarchyInfo = provider;

		if (provider != null) {
			propagateNames(provider);
		}
	}

	@Override
	public String getSrcNamespace() {
		return srcNamespace;
	}

	@Override
	public String setSrcNamespace(String namespace) {
		String ret = srcNamespace;
		srcNamespace = namespace;

		return ret;
	}

	@Override
	public List<String> getDstNamespaces() {
		return dstNamespaces;
	}

	@Override
	public List<String> setDstNamespaces(List<String> namespaces) {
		if (!classesBySrcName.isEmpty()) { // classes present, update existing dstNames
			int newSize = namespaces.size();
			int[] nameMap = new int[newSize];

			for (int i = 0; i < newSize; i++) {
				String newNs = namespaces.get(i);

				if (newNs.equals(srcNamespace)) {
					throw new IllegalArgumentException("can't use the same namespace for src and dst");
				} else {
					int oldNsIdx = dstNamespaces.indexOf(newNs);
					nameMap[i] = oldNsIdx;
				}
			}

			boolean useResize = true;

			for (int i = 0; i < newSize; i++) {
				int src = nameMap[i];

				if (src != i && (src >= 0 || i >= dstNamespaces.size())) { // not a 1:1 copy with potential null extension
					useResize = false;
					break;
				}
			}

			if (useResize) {
				resizeDstNames(newSize);
			} else {
				updateDstNames(nameMap);
			}
		}

		List<String> ret = dstNamespaces;
		dstNamespaces = namespaces;

		if (indexByDstNames) {
			initClassesByDstNames();
		}

		return ret;
	}

	private void resizeDstNames(int newSize) {
		for (ClassEntry cls : classesBySrcName.values()) {
			cls.resizeDstNames(newSize);

			for (FieldEntry field : cls.getFields()) {
				field.resizeDstNames(newSize);
			}

			for (MethodEntry method : cls.getMethods()) {
				method.resizeDstNames(newSize);

				for (MethodArgEntry arg : method.getArgs()) {
					arg.resizeDstNames(newSize);
				}

				for (MethodVarEntry var : method.getVars()) {
					var.resizeDstNames(newSize);
				}
			}
		}
	}

	private void updateDstNames(int[] nameMap) {
		for (ClassEntry cls : classesBySrcName.values()) {
			cls.updateDstNames(nameMap);

			for (FieldEntry field : cls.getFields()) {
				field.updateDstNames(nameMap);
			}

			for (MethodEntry method : cls.getMethods()) {
				method.updateDstNames(nameMap);

				for (MethodArgEntry arg : method.getArgs()) {
					arg.updateDstNames(nameMap);
				}

				for (MethodVarEntry var : method.getVars()) {
					var.updateDstNames(nameMap);
				}
			}
		}
	}

	@Override
	public List<? extends MetadataEntry> getMetadata() {
		return metadata;
	}

	@Override
	public List<? extends MetadataEntry> getMetadata(String key) {
		return Collections.unmodifiableList(metadata.stream()
				.filter(entry -> entry.getKey().equals(key))
				.collect(Collectors.toList()));
	}

	@Override
	public void addMetadata(MetadataEntry entry) {
		metadata.add(entry);
	}

	@Override
	public boolean removeMetadata(String key) {
		return metadata.removeIf(entry -> entry.getKey().equals(key));
	}

	@Override
	public Collection<? extends ClassMapping> getClasses() {
		return classesBySrcName.values();
	}

	@Override
	public ClassMapping getClass(String srcName) {
		return classesBySrcName.get(srcName);
	}

	@Override
	public ClassMapping getClass(String name, int namespace) {
		if (namespace < 0 || !indexByDstNames) {
			return VisitableMappingTree.super.getClass(name, namespace);
		} else {
			return classesByDstNames[namespace].get(name);
		}
	}

	@Override
	public ClassMapping addClass(ClassMapping cls) {
		ClassEntry entry = cls instanceof ClassEntry && cls.getTree() == this ? (ClassEntry) cls : new ClassEntry(this, cls, getSrcNsEquivalent(cls));
		ClassEntry ret = classesBySrcName.putIfAbsent(cls.getSrcName(), entry);

		if (ret != null) {
			ret.copyFrom(entry, false);
			entry = ret;
		}

		if (indexByDstNames) {
			for (int i = 0; i < entry.dstNames.length; i++) {
				String dstName = entry.dstNames[i];
				if (dstName != null) classesByDstNames[i].put(dstName, entry);
			}
		}

		return entry;
	}

	private int getSrcNsEquivalent(ElementMapping mapping) {
		int ret = mapping.getTree().getNamespaceId(srcNamespace);
		if (ret == NULL_NAMESPACE_ID) throw new UnsupportedOperationException("can't find source namespace in referenced mapping tree");

		return ret;
	}

	@Override
	public ClassMapping removeClass(String srcName) {
		ClassEntry ret = classesBySrcName.remove(srcName);

		if (ret != null && indexByDstNames) {
			for (int i = 0; i < ret.dstNames.length; i++) {
				String dstName = ret.dstNames[i];
				if (dstName != null) classesByDstNames[i].remove(dstName);
			}
		}

		return ret;
	}

	@Override
	public void accept(MappingVisitor visitor, VisitOrder order) throws IOException {
		do {
			if (visitor.visitHeader()) {
				visitor.visitNamespaces(srcNamespace, dstNamespaces);
				List<MetadataEntry> metadataToVisit = metadata;

				if (visitor.getFlags().contains(MappingFlag.NEEDS_METADATA_UNIQUENESS)) {
					metadataToVisit = new LinkedList<>();
					Set<String> addedKeys = new HashSet<>();

					// Iterate last-to-first to construct a list of each key's latest occurrence.
					for (int i = metadata.size() - 1; i >= 0; i--) {
						MetadataEntry entry = metadata.get(i);

						if (!addedKeys.contains(entry.getKey())) {
							addedKeys.add(entry.getKey());
							metadataToVisit.add(0, entry);
						}
					}
				}

				for (MetadataEntry entry : metadataToVisit) {
					visitor.visitMetadata(entry.getKey(), entry.getValue());
				}
			}

			if (visitor.visitContent()) {
				Set<MappingFlag> flags = visitor.getFlags();
				boolean supplyFieldDstDescs = flags.contains(MappingFlag.NEEDS_DST_FIELD_DESC);
				boolean supplyMethodDstDescs = flags.contains(MappingFlag.NEEDS_DST_METHOD_DESC);

				for (ClassEntry cls : order.sortClasses(classesBySrcName.values())) {
					cls.accept(visitor, order, supplyFieldDstDescs, supplyMethodDstDescs);
				}
			}
		} while (!visitor.visitEnd());
	}

	@Override
	public void reset() {
		currentEntry = null;
		currentClass = null;
		currentMethod = null;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) {
		srcNsMap = SRC_NAMESPACE_ID;
		dstNameMap = new int[dstNamespaces.size()];

		if (this.srcNamespace != null) { // ns already set, try to merge
			if (!srcNamespace.equals(this.srcNamespace)) {
				srcNsMap = this.dstNamespaces.indexOf(srcNamespace);
				if (srcNsMap < 0) throw new UnsupportedOperationException("can't merge with disassociated src namespace"); // srcNamespace must already be present
			}

			int newDstNamespaces = 0;

			for (int i = 0; i < dstNameMap.length; i++) {
				String dstNs = dstNamespaces.get(i);
				int idx;

				if (dstNs.equals(srcNamespace)) {
					idx = -1;
				} else {
					idx = this.dstNamespaces.indexOf(dstNs);

					if (idx < 0) {
						if (dstNs.equals(this.srcNamespace)) throw new UnsupportedOperationException("can't merge with existing src namespace in new dst namespaces");
						if (newDstNamespaces == 0) this.dstNamespaces = new ArrayList<>(this.dstNamespaces);

						idx = this.dstNamespaces.size();
						this.dstNamespaces.add(dstNs);
						newDstNamespaces++;
					}
				}

				dstNameMap[i] = idx;
			}

			if (newDstNamespaces > 0) {
				int newSize = this.dstNamespaces.size();
				resizeDstNames(newSize);

				if (indexByDstNames) {
					classesByDstNames = Arrays.copyOf(classesByDstNames, newSize);

					for (int i = newSize - newDstNamespaces; i < classesByDstNames.length; i++) {
						classesByDstNames[i] = new HashMap<String, ClassEntry>(classesBySrcName.size());
					}
				}
			}
		} else {
			this.srcNamespace = srcNamespace;
			this.dstNamespaces = dstNamespaces;

			for (int i = 0; i < dstNameMap.length; i++) {
				dstNameMap[i] = dstNamespaces.get(i).equals(srcNamespace) ? -1 : i;
			}

			if (indexByDstNames) {
				initClassesByDstNames();
			}
		}
	}

	@Override
	public void visitMetadata(String key, String value) {
		MetadataEntryImpl entry = new MetadataEntryImpl(key, value);
		addMetadata(entry);
	}

	@Override
	public boolean visitClass(String srcName) {
		currentMethod = null;

		ClassEntry cls = (ClassEntry) getClass(srcName, srcNsMap);

		if (cls == null) {
			if (srcNsMap >= 0) { // tree-side srcName unknown
				cls = new ClassEntry(this, null);
				cls.setDstName(srcName, srcNsMap);
			} else {
				cls = new ClassEntry(this, srcName);
				classesBySrcName.put(srcName, cls);
			}
		}

		currentEntry = currentClass = cls;

		return true;
	}

	@Override
	public boolean visitField(String srcName, String srcDesc) {
		if (currentClass == null) throw new UnsupportedOperationException("Tried to visit field before owning class");

		currentMethod = null;

		FieldEntry field = currentClass.getField(srcName, srcDesc, srcNsMap);

		if (field == null) {
			if (srcNsMap >= 0) { // tree-side srcName unknown, can't create new entry directly
				field = (FieldEntry) queuePendingMember(srcName, srcDesc, true);
			} else {
				field = new FieldEntry(currentClass, srcName, srcDesc);
				field = currentClass.addField(field);
			}
		} else if (srcDesc != null && field.srcDesc == null) {
			field.setSrcDesc(mapDesc(srcDesc, srcNsMap, SRC_NAMESPACE_ID)); // assumes the class mapping is already sufficiently present..
		}

		currentEntry = field;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, String srcDesc) {
		if (currentClass == null) throw new UnsupportedOperationException("Tried to visit method before owning class");

		MethodEntry method = currentClass.getMethod(srcName, srcDesc, srcNsMap);

		if (method == null) {
			if (srcNsMap >= 0) { // tree-side srcName unknown, can't create new entry directly
				method = (MethodEntry) queuePendingMember(srcName, srcDesc, false);
			} else {
				method = new MethodEntry(currentClass, srcName, srcDesc);
				method = currentClass.addMethod(method);
			}
		} else if (srcDesc != null && (method.srcDesc == null || method.srcDesc.endsWith(")") && !srcDesc.endsWith(")"))) {
			method.setSrcDesc(mapDesc(srcDesc, srcNsMap, SRC_NAMESPACE_ID)); // assumes the class mapping is already sufficiently present..
		}

		currentEntry = currentMethod = method;

		return true;
	}

	private MemberEntry<?> queuePendingMember(String name, String desc, boolean isField) {
		if (pendingMembers == null) pendingMembers = new HashMap<>();
		GlobalMemberKey key = new GlobalMemberKey(currentClass, name, desc, isField);
		MemberEntry<?> member = pendingMembers.get(key);

		if (member == null) {
			if (isField) {
				member = new FieldEntry(currentClass, null, desc);
			} else {
				member = new MethodEntry(currentClass, null, desc);
			}

			pendingMembers.put(key, member);
		}

		if (srcNsMap >= 0) {
			member.setDstName(name, srcNsMap);
		}

		return member;
	}

	private void addPendingMember(MemberEntry<?> member) {
		String name = member.getName(srcNsMap);

		if (name == null) {
			return;
		}

		String desc = member.getDesc(srcNsMap);

		if (member.getKind() == MappedElementKind.FIELD) {
			FieldEntry field = member.getOwner().getField(name, desc);

			if (field == null) {
				member.srcName = name;
				member.setSrcDesc(desc);
			} else { // copy remaining data
				field.copyFrom((FieldEntry) member, false);
			}
		} else {
			MethodEntry method = member.getOwner().getMethod(name, desc);

			if (method == null) {
				member.srcName = name;
				member.setSrcDesc(desc);
			} else { // copy remaining data
				method.copyFrom((MethodEntry) member, false);
			}
		}
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, String srcName) {
		if (currentMethod == null) throw new UnsupportedOperationException("Tried to visit method argument before owning method");

		MethodArgEntry arg = currentMethod.getArg(argPosition, lvIndex, srcName);

		if (arg == null) {
			arg = new MethodArgEntry(currentMethod, argPosition, lvIndex, srcName);
			arg = currentMethod.addArg(arg);
		} else {
			if (argPosition >= 0 && arg.argPosition < 0) arg.setArgPosition(argPosition);
			if (lvIndex >= 0 && arg.lvIndex < 0) arg.setLvIndex(lvIndex);

			if (srcName != null) {
				assert !srcName.isEmpty();
				arg.setSrcName(srcName);
			}
		}

		currentEntry = arg;

		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) {
		if (currentMethod == null) throw new UnsupportedOperationException("Tried to visit method variable before owning method");

		MethodVarEntry var = currentMethod.getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);

		if (var == null) {
			var = new MethodVarEntry(currentMethod, lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
			var = currentMethod.addVar(var);
		} else {
			if (lvtRowIndex >= 0 && var.lvtRowIndex < 0) var.setLvtRowIndex(lvtRowIndex);
			if (lvIndex >= 0 && startOpIdx >= 0 && (var.lvIndex < 0 || var.startOpIdx < 0)) var.setLvIndex(lvIndex, startOpIdx, endOpIdx);

			if (srcName != null) {
				assert !srcName.isEmpty();
				var.setSrcName(srcName);
			}
		}

		currentEntry = var;

		return true;
	}

	@Override
	public boolean visitEnd() {
		currentEntry = null;
		currentClass = null;
		currentMethod = null;

		if (pendingMembers != null) {
			for (MemberEntry<?> member : pendingMembers.values()) {
				addPendingMember(member);
			}

			pendingMembers = null;
		}

		if (hierarchyInfo != null) {
			propagateNames(hierarchyInfo);
		}

		return true;
	}

	private <T> void propagateNames(HierarchyInfoProvider<T> provider) {
		int nsId = getNamespaceId(provider.getNamespace());
		if (nsId == NULL_NAMESPACE_ID) return;

		Set<MethodEntry> processed = Collections.newSetFromMap(new IdentityHashMap<>());

		for (ClassEntry cls : classesBySrcName.values()) {
			for (MethodEntry method : cls.getMethods()) {
				String name = method.getName(nsId);
				if (name == null || name.startsWith("<")) continue; // missing name, <clinit> or <init>
				if (!processed.add(method)) continue;

				T hierarchy = provider.getMethodHierarchy(method);
				if (provider.getHierarchySize(hierarchy) <= 1) continue;

				Collection<? extends MethodMapping> hierarchyMethods = provider.getHierarchyMethods(hierarchy, this);
				if (hierarchyMethods.size() <= 1) continue;

				String[] dstNames = new String[dstNamespaces.size()];
				int rem = dstNames.length;

				nameGatherLoop: for (MethodMapping m : hierarchyMethods) {
					for (int i = 0; i < dstNames.length; i++) {
						if (dstNames[i] != null) continue;

						String curName = m.getDstName(i);

						if (curName != null) {
							dstNames[i] = curName;
							if (--rem == 0) break nameGatherLoop;
						}
					}
				}

				for (MethodMapping m : hierarchyMethods) {
					processed.add((MethodEntry) m);

					for (int i = 0; i < dstNames.length; i++) {
						String curName = dstNames[i];

						if (curName != null) {
							m.setDstName(curName, i);
						}
					}
				}
			}
		}
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
		namespace = dstNameMap[namespace];

		if (currentEntry == null) throw new UnsupportedOperationException("Tried to visit mapped name before owner");

		if (namespace < 0) {
			if (name.equals(currentEntry.getSrcName())) return;

			switch (currentEntry.getKind()) {
			case CLASS:
				assert currentClass == currentEntry;

				if (currentClass.srcName == null) {
					currentClass.srcName = name;
				} else {
					throw new UnsupportedOperationException("can't change src name for "+currentEntry.getKind());
				}

				break;
			case METHOD_ARG:
				((MethodArgEntry) currentEntry).setSrcName(name);
				break;
			case METHOD_VAR:
				((MethodVarEntry) currentEntry).setSrcName(name);
				break;
			default:
				throw new UnsupportedOperationException("can't change src name for "+currentEntry.getKind());
			}
		} else {
			currentEntry.setDstName(name, namespace);
		}
	}

	@Override
	public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
		return targetKind != MappedElementKind.CLASS || currentClass.getSrcName() != null; // reject classes that never received a src name
	}

	@Override
	public void visitComment(MappedElementKind targetKind, String comment) {
		Entry<?> entry;

		switch (targetKind) {
		case CLASS:
			entry = currentClass;
			break;
		case METHOD:
			entry = currentMethod;
			break;
		default:
			entry = currentEntry;
		}

		if (entry == null) throw new UnsupportedOperationException("Tried to visit comment before owning target");
		entry.setComment(comment);
	}

	abstract static class Entry<T extends Entry<T>> implements ElementMapping {
		protected Entry(MemoryMappingTree tree, String srcName) {
			this.srcName = srcName;
			this.dstNames = new String[tree.dstNamespaces.size()];
		}

		protected Entry(MemoryMappingTree tree, ElementMapping src, int srcNsEquivalent) {
			this(tree, src.getName(srcNsEquivalent));

			for (int i = 0; i < dstNames.length; i++) {
				int dstNsEquivalent = src.getTree().getNamespaceId(tree.dstNamespaces.get(i));

				if (dstNsEquivalent != NULL_NAMESPACE_ID) {
					setDstName(src.getDstName(dstNsEquivalent), i);
				}
			}

			setComment(src.getComment());
		}

		public abstract MappedElementKind getKind();

		@Override
		public final String getSrcName() {
			return srcName;
		}

		@Override
		public final String getDstName(int namespace) {
			return dstNames[namespace];
		}

		@Override
		public void setDstName(String name, int namespace) {
			dstNames[namespace] = name;
		}

		void resizeDstNames(int newSize) {
			dstNames = Arrays.copyOf(dstNames, newSize);
		}

		void updateDstNames(int[] map) {
			String[] newDstNames = new String[map.length];

			for (int i = 0; i < map.length; i++) {
				int src = map[i];

				if (src >= 0) {
					newDstNames[i] = dstNames[src];
				}
			}

			dstNames = newDstNames;
		}

		@Override
		public final String getComment() {
			return comment;
		}

		@Override
		public final void setComment(String comment) {
			this.comment = comment;
		}

		protected final boolean acceptElement(MappingVisitor visitor, String[] dstDescs) throws IOException {
			MappedElementKind kind = getKind();

			for (int i = 0; i < dstNames.length; i++) {
				String dstName = dstNames[i];

				if (dstName != null) visitor.visitDstName(kind, i, dstName);
			}

			if (dstDescs != null) {
				for (int i = 0; i < dstDescs.length; i++) {
					String dstDesc = dstDescs[i];

					if (dstDesc != null) visitor.visitDstDesc(kind, i, dstDesc);
				}
			}

			if (!visitor.visitElementContent(kind)) {
				return false;
			}

			if (comment != null) visitor.visitComment(kind, comment);

			return true;
		}

		protected void copyFrom(T o, boolean replace) {
			for (int i = 0; i < dstNames.length; i++) {
				if (o.dstNames[i] != null && (replace || dstNames[i] == null)) {
					dstNames[i] = o.dstNames[i];
				}
			}

			if (o.comment != null && (replace || comment == null)) {
				comment = o.comment;
			}

			// TODO: copy args+vars
		}

		protected String srcName;
		protected String[] dstNames;
		protected String comment;
	}

	static final class ClassEntry extends Entry<ClassEntry> implements ClassMapping {
		ClassEntry(MemoryMappingTree tree, String srcName) {
			super(tree, srcName);

			this.tree = tree;
		}

		ClassEntry(MemoryMappingTree tree, ClassMapping src, int srcNsEquivalent) {
			super(tree, src, srcNsEquivalent);

			this.tree = tree;

			for (FieldMapping field : src.getFields()) {
				addField(field);
			}

			for (MethodMapping method : src.getMethods()) {
				addMethod(method);
			}
		}

		@Override
		public MappedElementKind getKind() {
			return MappedElementKind.CLASS;
		}

		@Override
		public MemoryMappingTree getTree() {
			return tree;
		}

		@Override
		public void setDstName(String name, int namespace) {
			if (tree.indexByDstNames) {
				String oldName = dstNames[namespace];

				if (!Objects.equals(name, oldName)) {
					Map<String, ClassEntry> map = tree.classesByDstNames[namespace];
					if (oldName != null) map.remove(oldName);

					if (name != null) {
						map.put(name, this);
					} else {
						map.remove(oldName);
					}
				}
			}

			super.setDstName(name, namespace);
		}

		@Override
		public Collection<FieldEntry> getFields() {
			if (fields == null) return Collections.emptyList();

			return fields.values();
		}

		@Override
		public FieldEntry getField(String srcName, String srcDesc) {
			return getMember(srcName, srcDesc, fields, flags, FLAG_HAS_ANY_FIELD_DESC, FLAG_MISSES_ANY_FIELD_DESC);
		}

		@Override
		public FieldEntry getField(String name, String desc, int namespace) {
			return (FieldEntry) ClassMapping.super.getField(name, desc, namespace);
		}

		@Override
		public FieldEntry addField(FieldMapping field) {
			FieldEntry entry = field instanceof FieldEntry && field.getOwner() == this ? (FieldEntry) field : new FieldEntry(this, field, tree.getSrcNsEquivalent(field));

			if (fields == null) fields = new LinkedHashMap<>();

			return addMember(entry, fields, FLAG_HAS_ANY_FIELD_DESC, FLAG_MISSES_ANY_FIELD_DESC);
		}

		@Override
		public FieldEntry removeField(String srcName, String srcDesc) {
			FieldEntry ret = getField(srcName, srcDesc);
			if (ret != null) fields.remove(ret.key);

			return ret;
		}

		@Override
		public Collection<MethodEntry> getMethods() {
			if (methods == null) return Collections.emptyList();

			return methods.values();
		}

		@Override
		public MethodEntry getMethod(String srcName, String srcDesc) {
			return getMember(srcName, srcDesc, methods, flags, FLAG_HAS_ANY_METHOD_DESC, FLAG_MISSES_ANY_METHOD_DESC);
		}

		@Override
		public MethodEntry getMethod(String name, String desc, int namespace) {
			return (MethodEntry) ClassMapping.super.getMethod(name, desc, namespace);
		}

		@Override
		public MethodEntry addMethod(MethodMapping method) {
			MethodEntry entry = method instanceof MethodEntry && method.getOwner() == this ? (MethodEntry) method : new MethodEntry(this, method, tree.getSrcNsEquivalent(method));

			if (methods == null) methods = new LinkedHashMap<>();

			return addMember(entry, methods, FLAG_HAS_ANY_METHOD_DESC, FLAG_MISSES_ANY_METHOD_DESC);
		}

		@Override
		public MethodEntry removeMethod(String srcName, String srcDesc) {
			MethodEntry ret = getMethod(srcName, srcDesc);
			if (ret != null) methods.remove(ret.key);

			return ret;
		}

		private static <T extends MemberEntry<T>> T getMember(String srcName, String srcDesc, Map<MemberKey, T> map, int flags, int flagHasAny, int flagMissesAny) {
			if (map == null) return null;

			boolean hasAnyDesc = (flags & flagHasAny) != 0;
			boolean missedAnyDesc = (flags & flagMissesAny) != 0;

			if (srcDesc == null) { // null desc
				if (missedAnyDesc) { // may have full match [no desc] -> [no desc]
					T ret = map.get(new MemberKey(srcName, null));
					if (ret != null) return ret;
				}

				if (hasAnyDesc) { // may have name match [no desc] -> [full desc/partial desc]
					for (T entry : map.values()) {
						if (entry.srcName.equals(srcName)) return entry;
					}
				}
			} else if (srcDesc.endsWith(")")) { // parameter-only desc
				if (missedAnyDesc) { // may have full match [partial desc] -> [partial desc]
					T ret = map.get(new MemberKey(srcName, srcDesc));
					if (ret != null) return ret;

					ret = map.get(new MemberKey(srcName, null));
					if (ret != null) return ret;
				}

				if (hasAnyDesc) { // may have partial-desc match [partial desc] -> [full desc]
					for (T entry : map.values()) {
						if (entry.srcName.equals(srcName)
								&& entry.srcDesc.startsWith(srcDesc)) {
							return entry;
						}
					}
				}
			} else { // regular desc
				if (hasAnyDesc) { // may have full match [full desc] -> [full desc]
					T ret = map.get(new MemberKey(srcName, srcDesc));
					if (ret != null) return ret;
				}

				if (missedAnyDesc) { // may have name/partial-desc match [full desc] -> [no desc/partial desc]
					T ret = map.get(new MemberKey(srcName, null));
					if (ret != null) return ret;

					if (srcDesc.indexOf(')') >= 0) {
						for (T entry : map.values()) {
							if (entry.srcName.equals(srcName)
									&& srcDesc.startsWith(entry.srcDesc)) { // entry.srcDesc can't be null here
								return entry;
							}
						}
					}
				}
			}

			return null;
		}

		private <T extends MemberEntry<T>> T addMember(T entry, Map<MemberKey, T> map, int flagHasAny, int flagMissesAny) {
			T ret = map.putIfAbsent(entry.key, entry);

			if (ret != null) { // same desc
				ret.copyFrom(entry, false);

				return ret;
			} else if (entry.srcDesc != null && !entry.srcDesc.endsWith(")")) { // may have replaced desc-less
				flags |= flagHasAny;

				if ((flags & flagMissesAny) != 0) {
					ret = map.remove(new MemberKey(srcName, null));

					if (ret != null) { // compatible entry exists, copy desc + extra content
						ret.key = entry.key;
						ret.srcDesc = entry.srcDesc;
						map.put(ret.key, ret);
						ret.copyFrom(entry, false);
						entry = ret;
					}
				}

				return entry;
			} else { // entry.srcDesc == null, may have replaced desc-containing
				if ((flags & flagHasAny) != 0) {
					for (T prevEntry : map.values()) {
						if (prevEntry != entry && prevEntry.srcName.equals(srcName) && (entry.srcDesc == null || prevEntry.srcDesc.startsWith(entry.srcDesc))) {
							map.remove(entry.key);
							prevEntry.copyFrom(entry, false);

							return prevEntry;
						}
					}
				}

				flags |= flagMissesAny;

				return entry;
			}
		}

		void accept(MappingVisitor visitor, VisitOrder order, boolean supplyFieldDstDescs, boolean supplyMethodDstDescs) throws IOException {
			if (visitor.visitClass(srcName) && acceptElement(visitor, null)) {
				boolean methodsFirst = order.isMethodsFirst() && fields != null && methods != null;

				if (!methodsFirst && fields != null) {
					for (FieldEntry field : order.sortFields(fields.values())) {
						field.accept(visitor, supplyFieldDstDescs);
					}
				}

				if (methods != null) {
					for (MethodEntry method : order.sortMethods(methods.values())) {
						method.accept(visitor, order, supplyMethodDstDescs);
					}
				}

				if (methodsFirst) {
					for (FieldEntry field : order.sortFields(fields.values())) {
						field.accept(visitor, supplyFieldDstDescs);
					}
				}
			}
		}

		@Override
		protected void copyFrom(ClassEntry o, boolean replace) {
			super.copyFrom(o, replace);

			if (o.fields != null) {
				for (FieldEntry oField : o.fields.values()) {
					FieldEntry field = getField(oField.srcName, oField.srcDesc);

					if (field == null) { // missing
						addField(oField);
					} else {
						if (oField.srcDesc != null && field.srcDesc == null) { // extra location info
							fields.remove(field.key);
							field.key = oField.key;
							field.srcDesc = oField.srcDesc;
							fields.put(field.key, field);

							flags |= FLAG_HAS_ANY_FIELD_DESC;
						}

						field.copyFrom(oField, replace);
					}
				}
			}

			if (o.methods != null) {
				for (MethodEntry oMethod : o.methods.values()) {
					MethodEntry method = getMethod(oMethod.srcName, oMethod.srcDesc);

					if (method == null) { // missing
						addMethod(oMethod);
					} else {
						if (oMethod.srcDesc != null && method.srcDesc == null) { // extra location info
							methods.remove(method.key);
							method.key = oMethod.key;
							method.srcDesc = oMethod.srcDesc;
							methods.put(method.key, method);

							flags |= FLAG_HAS_ANY_METHOD_DESC;
						}

						method.copyFrom(oMethod, replace);
					}
				}
			}
		}

		@Override
		public String toString() {
			return srcName;
		}

		private static final byte FLAG_HAS_ANY_FIELD_DESC = 1;
		private static final byte FLAG_MISSES_ANY_FIELD_DESC = 2;
		private static final byte FLAG_HAS_ANY_METHOD_DESC = 4;
		private static final byte FLAG_MISSES_ANY_METHOD_DESC = 8;

		protected final MemoryMappingTree tree;
		private Map<MemberKey, FieldEntry> fields = null;
		private Map<MemberKey, MethodEntry> methods = null;
		private byte flags;
	}

	abstract static class MemberEntry<T extends MemberEntry<T>> extends Entry<T> implements MemberMapping {
		protected MemberEntry(ClassEntry owner, String srcName, String srcDesc) {
			super(owner.tree, srcName);

			this.owner = owner;
			this.srcDesc = srcDesc;
			this.key = new MemberKey(srcName, srcDesc);
		}

		protected MemberEntry(ClassEntry owner, MemberMapping src, int srcNsEquivalent) {
			super(owner.tree, src, srcNsEquivalent);

			this.owner = owner;
			this.srcDesc = src.getDesc(srcNsEquivalent);
			this.key = new MemberKey(srcName, srcDesc);
		}

		@Override
		public MappingTree getTree() {
			return owner.tree;
		}

		@Override
		public final ClassEntry getOwner() {
			return owner;
		}

		@Override
		public final String getSrcDesc() {
			return srcDesc;
		}

		protected final boolean acceptMember(MappingVisitor visitor, boolean supplyDstDescs) throws IOException {
			String[] dstDescs;

			if (!supplyDstDescs || srcDesc == null) {
				dstDescs = null;
			} else {
				MappingTree tree = owner.tree;
				dstDescs = new String[tree.getDstNamespaces().size()];

				for (int i = 0; i < dstDescs.length; i++) {
					dstDescs[i] = tree.mapDesc(srcDesc, i);
				}
			}

			return acceptElement(visitor, dstDescs);
		}

		protected final ClassEntry owner;
		protected String srcDesc;
		MemberKey key;
	}

	static final class FieldEntry extends MemberEntry<FieldEntry> implements FieldMapping {
		FieldEntry(ClassEntry owner, String srcName, String srcDesc) {
			super(owner, srcName, srcDesc);
		}

		FieldEntry(ClassEntry owner, FieldMapping src, int srcNsEquivalent) {
			super(owner, src, srcNsEquivalent);
		}

		@Override
		public MappedElementKind getKind() {
			return MappedElementKind.FIELD;
		}

		@Override
		public void setSrcDesc(String desc) {
			if (Objects.equals(desc, srcDesc)) return;

			MemberKey newKey = new MemberKey(srcName, desc);
			if (owner.fields.containsKey(newKey)) throw new IllegalArgumentException("conflicting name+desc after changing desc to "+desc+" for "+this);

			owner.fields.remove(key);
			srcDesc = desc;
			key = newKey;
			owner.fields.put(newKey, this);

			if (desc != null) {
				owner.flags |= ClassEntry.FLAG_HAS_ANY_FIELD_DESC;
			} else {
				owner.flags |= ClassEntry.FLAG_MISSES_ANY_FIELD_DESC;
			}
		}

		void accept(MappingVisitor visitor, boolean supplyDstDescs) throws IOException {
			if (visitor.visitField(srcName, srcDesc)) {
				acceptMember(visitor, supplyDstDescs);
			}
		}

		@Override
		public String toString() {
			return String.format("%s;;%s", srcName, srcDesc);
		}
	}

	static final class MethodEntry extends MemberEntry<MethodEntry> implements MethodMapping {
		MethodEntry(ClassEntry owner, String srcName, String srcDesc) {
			super(owner, srcName, srcDesc);
		}

		MethodEntry(ClassEntry owner, MethodMapping src, int srcNsEquivalent) {
			super(owner, src, srcNsEquivalent);

			for (MethodArgMapping arg : src.getArgs()) {
				addArg(arg);
			}

			for (MethodVarMapping var : src.getVars()) {
				addVar(var);
			}
		}

		@Override
		public MappedElementKind getKind() {
			return MappedElementKind.METHOD;
		}

		@Override
		public void setSrcDesc(String desc) {
			if (Objects.equals(desc, srcDesc)) return;

			MemberKey newKey = new MemberKey(srcName, desc);
			if (owner.methods.containsKey(newKey)) throw new IllegalArgumentException("conflicting name+desc after changing desc to "+desc+" for "+this);

			owner.methods.remove(key);
			srcDesc = desc;
			key = newKey;
			owner.methods.put(newKey, this);

			if (desc != null && !desc.endsWith(")")) {
				owner.flags |= ClassEntry.FLAG_HAS_ANY_METHOD_DESC;
			} else {
				owner.flags |= ClassEntry.FLAG_MISSES_ANY_METHOD_DESC;
			}
		}

		@Override
		public Collection<MethodArgEntry> getArgs() {
			if (args == null) return Collections.emptyList();

			return args;
		}

		@Override
		public MethodArgEntry getArg(int argPosition, int lvIndex, String srcName) {
			if (args == null) return null;

			if (argPosition >= 0 || lvIndex >= 0) {
				for (MethodArgEntry entry : args) {
					if (argPosition >= 0 && entry.argPosition == argPosition
							|| lvIndex >= 0 && entry.lvIndex == lvIndex) {
						return entry;
					}
				}
			}

			if (srcName != null) {
				for (MethodArgEntry entry : args) {
					if (srcName.equals(entry.srcName)
							&& (argPosition < 0 || entry.argPosition < 0)
							&& (lvIndex < 0 || entry.lvIndex < 0)) {
						return entry;
					}
				}
			}

			return null;
		}

		@Override
		public MethodArgEntry addArg(MethodArgMapping arg) {
			MethodArgEntry entry = arg instanceof MethodArgEntry && arg.getMethod() == this ? (MethodArgEntry) arg : new MethodArgEntry(this, arg, owner.tree.getSrcNsEquivalent(arg));
			MethodArgEntry prev = getArg(arg.getArgPosition(), arg.getLvIndex(), arg.getSrcName());

			if (prev == null) {
				if (args == null) args = new ArrayList<>();
				args.add(entry);
			} else {
				updateArg(prev, entry, false);
			}

			return entry;
		}

		private void updateArg(MethodArgEntry existing, MethodArgEntry toAdd, boolean replace) {
			if (toAdd.argPosition >= 0 && existing.argPosition < 0) existing.setArgPosition(toAdd.argPosition);
			if (toAdd.lvIndex >= 0 && existing.lvIndex < 0) existing.setLvIndex(toAdd.getLvIndex());

			existing.copyFrom(toAdd, replace);
		}

		@Override
		public MethodArgEntry removeArg(int argPosition, int lvIndex, String srcName) {
			MethodArgEntry ret = getArg(argPosition, lvIndex, srcName);
			if (ret != null) args.remove(ret);

			return ret;
		}

		@Override
		public Collection<MethodVarEntry> getVars() {
			if (vars == null) return Collections.emptyList();

			return vars;
		}

		@Override
		public MethodVarEntry getVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) {
			if (vars == null) return null;

			if (lvtRowIndex >= 0) {
				boolean hasMissing = false;

				for (MethodVarEntry entry : vars) {
					if (entry.lvtRowIndex == lvtRowIndex) {
						return entry;
					} else if (entry.lvtRowIndex < 0) {
						hasMissing = true;
					}
				}

				if (!hasMissing) return null;
			}

			if (lvIndex >= 0) {
				boolean hasMissing = false;
				MethodVarEntry bestMatch = null;

				for (MethodVarEntry entry : vars) {
					// skip otherwise mismatched candidates
					if (lvtRowIndex >= 0 && entry.lvtRowIndex >= 0 && lvtRowIndex != entry.lvtRowIndex // different lvtRowIndex
							|| srcName != null && entry.srcName != null && !srcName.equals(entry.srcName)) { // different srcName
						continue;
					}

					if (entry.lvIndex != lvIndex) {
						if (entry.lvIndex < 0) hasMissing = true;
						continue;
					}

					if (startOpIdx >= 0 && endOpIdx >= 0 && entry.startOpIdx >= 0 && entry.endOpIdx >= 0) { // full ranges on both
						if (startOpIdx >= entry.endOpIdx || endOpIdx <= entry.startOpIdx) { // non-overlapping op idx ranges
							continue;
						} else { // full match
							return entry;
						}
					}

					if (endOpIdx >= 0 && entry.startOpIdx >= 0 && endOpIdx <= entry.startOpIdx
							|| entry.endOpIdx >= 0 && startOpIdx >= 0 && entry.endOpIdx <= startOpIdx) {
						// incompatible full range on one side
						continue;
					}

					if (startOpIdx < 0 || startOpIdx == entry.startOpIdx) {
						return entry;
					}

					if (bestMatch == null
							|| entry.startOpIdx >= 0 && Math.abs(entry.startOpIdx - startOpIdx) < Math.abs(bestMatch.startOpIdx - startOpIdx)) {
						bestMatch = entry;
					}
				}

				if (!hasMissing || bestMatch != null) return bestMatch;
			}

			if (srcName != null) {
				for (MethodVarEntry entry : vars) {
					if (srcName.equals(entry.srcName)
							&& (lvtRowIndex < 0 || entry.lvtRowIndex < 0)
							&& (lvIndex < 0 || entry.lvIndex < 0)) {
						return entry;
					}
				}
			}

			return null;
		}

		@Override
		public MethodVarEntry addVar(MethodVarMapping var) {
			MethodVarEntry entry = var instanceof MethodVarEntry && var.getMethod() == this ? (MethodVarEntry) var : new MethodVarEntry(this, var, owner.tree.getSrcNsEquivalent(var));
			MethodVarEntry prev = getVar(var.getLvtRowIndex(), var.getLvIndex(), var.getStartOpIdx(), var.getEndOpIdx(), var.getSrcName());

			if (prev == null) {
				if (vars == null) vars = new ArrayList<>();
				vars.add(entry);
			} else {
				updateVar(prev, entry, false);
			}

			return entry;
		}

		private void updateVar(MethodVarEntry existing, MethodVarEntry toAdd, boolean replace) {
			if (toAdd.lvtRowIndex >= 0 && existing.lvtRowIndex < 0) existing.setLvtRowIndex(toAdd.lvtRowIndex);

			if (toAdd.lvIndex >= 0 && toAdd.startOpIdx >= 0 && (existing.lvIndex < 0 || existing.startOpIdx < 0)) {
				existing.setLvIndex(toAdd.lvIndex, toAdd.startOpIdx, toAdd.endOpIdx);
			}

			existing.copyFrom(toAdd, replace);
		}

		@Override
		public MethodVarEntry removeVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) {
			MethodVarEntry ret = getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
			if (ret != null) vars.remove(ret);

			return ret;
		}

		void accept(MappingVisitor visitor, VisitOrder order, boolean supplyDstDescs) throws IOException {
			if (visitor.visitMethod(srcName, srcDesc) && acceptMember(visitor, supplyDstDescs)) {
				boolean varsFirst = order.isMethodVarsFirst() && args != null && vars != null;

				if (!varsFirst && args != null) {
					for (MethodArgEntry arg : order.sortMethodArgs(args)) {
						arg.accept(visitor);
					}
				}

				if (vars != null) {
					for (MethodVarEntry var : order.sortMethodVars(vars)) {
						var.accept(visitor);
					}
				}

				if (varsFirst) {
					for (MethodArgEntry arg : order.sortMethodArgs(args)) {
						arg.accept(visitor);
					}
				}
			}
		}

		@Override
		protected void copyFrom(MethodEntry o, boolean replace) {
			super.copyFrom(o, replace);

			if (o.args != null) {
				for (MethodArgEntry oArg : o.args) {
					MethodArgEntry arg = getArg(oArg.argPosition, oArg.lvIndex, oArg.srcName);

					if (arg == null) { // missing
						addArg(oArg);
					} else {
						updateArg(arg, oArg, replace);
					}
				}
			}

			if (o.vars != null) {
				for (MethodVarEntry oVar : o.vars) {
					MethodVarEntry var = getVar(oVar.lvtRowIndex, oVar.lvIndex, oVar.startOpIdx, oVar.endOpIdx, oVar.srcName);

					if (var == null) { // missing
						addVar(oVar);
					} else {
						updateVar(var, oVar, replace);
					}
				}
			}
		}

		@Override
		public String toString() {
			return String.format("%s%s", srcName, srcDesc);
		}

		private List<MethodArgEntry> args = null;
		private List<MethodVarEntry> vars = null;
	}

	static final class MethodArgEntry extends Entry<MethodArgEntry> implements MethodArgMapping {
		MethodArgEntry(MethodEntry method, int argPosition, int lvIndex, String srcName) {
			super(method.owner.tree, srcName);

			this.method = method;
			this.argPosition = argPosition;
			this.lvIndex = lvIndex;
		}

		MethodArgEntry(MethodEntry method, MethodArgMapping src, int srcNsEquivalent) {
			super(method.owner.tree, src, srcNsEquivalent);

			this.method = method;
			this.argPosition = src.getArgPosition();
			this.lvIndex = src.getLvIndex();
		}

		@Override
		public MappingTree getTree() {
			return method.owner.tree;
		}

		@Override
		public MappedElementKind getKind() {
			return MappedElementKind.METHOD_ARG;
		}

		@Override
		public MethodEntry getMethod() {
			return method;
		}

		@Override
		public int getArgPosition() {
			return argPosition;
		}

		@Override
		public void setArgPosition(int position) {
			this.argPosition = position;
		}

		@Override
		public int getLvIndex() {
			return lvIndex;
		}

		@Override
		public void setLvIndex(int index) {
			this.lvIndex = index;
		}

		public void setSrcName(String name) {
			this.srcName = name;
		}

		void accept(MappingVisitor visitor) throws IOException {
			if (visitor.visitMethodArg(argPosition, lvIndex, srcName)) {
				acceptElement(visitor, null);
			}
		}

		@Override
		protected void copyFrom(MethodArgEntry o, boolean replace) {
			super.copyFrom(o, replace);

			if (o.srcName != null && (replace || srcName == null)) {
				srcName = o.srcName;
			}
		}

		@Override
		public String toString() {
			return String.format("%d/%d:%s", argPosition, lvIndex, srcName);
		}

		private final MethodEntry method;
		private int argPosition;
		private int lvIndex;
	}

	static final class MethodVarEntry extends Entry<MethodVarEntry> implements MethodVarMapping {
		MethodVarEntry(MethodEntry method, int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName) {
			super(method.owner.tree, srcName);

			this.method = method;
			this.lvtRowIndex = lvtRowIndex;
			this.lvIndex = lvIndex;
			this.startOpIdx = startOpIdx;
			this.endOpIdx = endOpIdx;
		}

		MethodVarEntry(MethodEntry method, MethodVarMapping src, int srcNs) {
			super(method.owner.tree, src, srcNs);

			this.method = method;
			this.lvtRowIndex = src.getLvtRowIndex();
			this.lvIndex = src.getLvIndex();
			this.startOpIdx = src.getStartOpIdx();
			this.endOpIdx = src.getEndOpIdx();
		}

		@Override
		public MappingTree getTree() {
			return method.owner.tree;
		}

		@Override
		public MappedElementKind getKind() {
			return MappedElementKind.METHOD_VAR;
		}

		@Override
		public MethodEntry getMethod() {
			return method;
		}

		@Override
		public int getLvtRowIndex() {
			return lvtRowIndex;
		}

		@Override
		public void setLvtRowIndex(int index) {
			this.lvtRowIndex = index;
		}

		@Override
		public int getLvIndex() {
			return lvIndex;
		}

		@Override
		public int getStartOpIdx() {
			return startOpIdx;
		}

		@Override
		public int getEndOpIdx() {
			return endOpIdx;
		}

		@Override
		public void setLvIndex(int lvIndex, int startOpIdx, int endOpIdx) {
			this.lvIndex = lvIndex;
			this.startOpIdx = startOpIdx;
			this.endOpIdx = endOpIdx;
		}

		public void setSrcName(String name) {
			this.srcName = name;
		}

		void accept(MappingVisitor visitor) throws IOException {
			if (visitor.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName)) {
				acceptElement(visitor, null);
			}
		}

		@Override
		protected void copyFrom(MethodVarEntry o, boolean replace) {
			super.copyFrom(o, replace);

			if (o.srcName != null && (replace || srcName == null)) {
				srcName = o.srcName;
			}
		}

		@Override
		public String toString() {
			return String.format("%d/%d@%d-%d:%s", lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
		}

		private final MethodEntry method;
		private int lvtRowIndex;
		private int lvIndex;
		private int startOpIdx;
		private int endOpIdx;
	}

	static final class MemberKey {
		MemberKey(String name, String desc) {
			this.name = name;
			this.desc = desc;

			if (desc == null) {
				hash = name.hashCode();
			} else {
				hash = name.hashCode() * 257 + desc.hashCode();
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != MemberKey.class) return false;

			MemberKey o = (MemberKey) obj;

			return name.equals(o.name) && Objects.equals(desc, o.desc);
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public String toString() {
			return String.format("%s.%s", name, desc);
		}

		private final String name;
		private final String desc;
		private final int hash;
	}

	static final class MetadataEntryImpl implements MetadataEntry {
		MetadataEntryImpl(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) return true;

			if (!(other instanceof MetadataEntryImpl)) {
				return false;
			}

			MetadataEntryImpl entry = (MetadataEntryImpl) other;

			return this.key.equals(entry.key) && this.value.equals(entry.value);
		}

		@Override
		public int hashCode() {
			return key.hashCode() | value.hashCode();
		}

		final String key;
		final String value;
	}

	static final class GlobalMemberKey {
		GlobalMemberKey(ClassEntry owner, String name, String desc, boolean isField) {
			this.owner = owner;
			this.name = name;
			this.desc = desc;
			this.isField = isField;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != GlobalMemberKey.class) return false;

			GlobalMemberKey o = (GlobalMemberKey) obj;

			return owner == o.owner && name.equals(o.name) && Objects.equals(desc, o.desc) && isField == o.isField;
		}

		@Override
		public int hashCode() {
			int ret = owner.hashCode() * 31 + name.hashCode();
			if (desc != null) ret |= desc.hashCode();
			if (isField) ret++;

			return ret;
		}

		@Override
		public String toString() {
			return String.format("%s.%s.%s", owner, name, desc);
		}

		private final ClassEntry owner;
		private final String name;
		private final String desc;
		private final boolean isField;
	}

	private boolean indexByDstNames;
	private String srcNamespace;
	private List<String> dstNamespaces = Collections.emptyList();
	private final List<MetadataEntry> metadata = new ArrayList<>();
	private final Map<String, ClassEntry> classesBySrcName = new LinkedHashMap<>();
	private Map<String, ClassEntry>[] classesByDstNames;

	private HierarchyInfoProvider<?> hierarchyInfo;

	private int srcNsMap;
	private int[] dstNameMap;
	private Entry<?> currentEntry;
	private ClassEntry currentClass;
	private MethodEntry currentMethod;
	private Map<GlobalMemberKey, MemberEntry<?>> pendingMembers;
}

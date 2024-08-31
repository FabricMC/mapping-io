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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingFlag;
import net.fabricmc.mappingio.MappingVisitor;

/**
 * {@link VisitableMappingTree} implementation that stores all data in memory.
 */
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

	/**
	 * Whether or not to index classes by their destination names, in addition to their source names.
	 *
	 * <p>Trades higher memory consumption for faster lookups by destination name.
	 */
	public void setIndexByDstNames(boolean indexByDstNames) {
		assertNotInVisitPass();
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

	@ApiStatus.Experimental
	public void setHierarchyInfoProvider(@Nullable HierarchyInfoProvider<?> provider) {
		hierarchyInfo = provider;

		if (provider != null) {
			propagateNames(provider);
		}
	}

	@Override
	@Nullable
	public String getSrcNamespace() {
		return srcNamespace;
	}

	@Override
	@Nullable
	public String setSrcNamespace(String namespace) {
		assertNotInVisitPass();
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
		assertNotInVisitPass();

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
		return classesView;
	}

	@Override
	@Nullable
	public ClassMapping getClass(String srcName) {
		return classesBySrcName.get(srcName);
	}

	@Override
	@Nullable
	public ClassMapping getClass(String name, int namespace) {
		if (namespace < 0 || !indexByDstNames) {
			return VisitableMappingTree.super.getClass(name, namespace);
		} else {
			return classesByDstNames[namespace].get(name);
		}
	}

	@Override
	public ClassMapping addClass(ClassMapping cls) {
		assertNotInVisitPass();
		ClassEntry entry = cls instanceof ClassEntry && cls.getTree() == this ? (ClassEntry) cls : new ClassEntry(this, cls, getSrcNsEquivalent(cls));
		ClassEntry ret = classesBySrcName.putIfAbsent(cls.getSrcName(), entry);

		if (ret != null) {
			ret.copyFrom(entry, true);
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
	@Nullable
	public ClassMapping removeClass(String srcName) {
		assertNotInVisitPass();
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
				Collection<MetadataEntry> metadataToVisit = metadata;

				if (visitor.getFlags().contains(MappingFlag.NEEDS_METADATA_UNIQUENESS)) {
					Deque<MetadataEntry> uniqueMetadata = new ArrayDeque<>();
					Set<String> addedKeys = new HashSet<>();

					// Iterate last-to-first to construct a list of each key's latest occurrence.
					for (int i = metadata.size() - 1; i >= 0; i--) {
						MetadataEntry entry = metadata.get(i);

						if (!addedKeys.contains(entry.getKey())) {
							addedKeys.add(entry.getKey());
							uniqueMetadata.addFirst(entry);
						}
					}

					metadataToVisit = uniqueMetadata;
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
		inVisitPass = false;
		srcNsMap = SRC_NAMESPACE_ID;
		dstNameMap = null;
		currentEntry = null;
		currentClass = null;
		currentMethod = null;
		pendingClasses = null;
		pendingMembers = null;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) {
		inVisitPass = true;
		srcNsMap = SRC_NAMESPACE_ID;
		dstNameMap = new int[dstNamespaces.size()];

		if (this.srcNamespace != null) { // ns already set, try to merge
			if (!srcNamespace.equals(this.srcNamespace)) {
				srcNsMap = this.dstNamespaces.indexOf(srcNamespace);

				if (srcNsMap < 0) {
					reset();
					throw new IllegalArgumentException("can't merge with disassociated src namespace"); // srcNamespace must already be present
				}
			}

			int newDstNamespaces = 0;

			for (int i = 0; i < dstNameMap.length; i++) {
				String dstNs = dstNamespaces.get(i);
				int idx;

				if (dstNs.equals(this.srcNamespace)) {
					idx = SRC_NAMESPACE_ID;
				} else if (dstNs.equals(srcNamespace)) {
					reset();
					throw new IllegalArgumentException("namespace \"" + srcNamespace + "\" is present on both source and destination side simultaneously");
				} else {
					idx = this.dstNamespaces.indexOf(dstNs);

					if (idx < 0) {
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
				if (dstNamespaces.get(i).equals(srcNamespace)) {
					reset();
					throw new IllegalArgumentException("namespace \"" + srcNamespace + "\" is present on both source and destination side simultaneously");
				}

				dstNameMap[i] = i;
			}

			if (indexByDstNames) {
				initClassesByDstNames();
			}
		}
	}

	@Override
	public void visitMetadata(String key, @Nullable String value) {
		MetadataEntryImpl entry = new MetadataEntryImpl(key, value);
		metadata.add(entry);
	}

	@Override
	public boolean visitClass(String srcName) {
		currentMethod = null;

		ClassEntry cls = (ClassEntry) getClass(srcName, srcNsMap);

		if (cls == null) {
			if (srcNsMap >= 0) { // tree-side srcName unknown
				cls = queuePendingClass(srcName);
			} else {
				cls = new ClassEntry(this, srcName);
				classesBySrcName.put(srcName, cls);
			}
		}

		currentEntry = currentClass = cls;

		return true;
	}

	@Override
	public boolean visitField(String srcName, @Nullable String srcDesc) {
		if (currentClass == null) throw new UnsupportedOperationException("Tried to visit field before owning class");

		currentMethod = null;

		FieldEntry field = currentClass.getField(srcName, srcDesc, srcNsMap);

		if (field == null) {
			if (srcNsMap >= 0) { // tree-side srcName unknown, can't create new entry directly
				field = (FieldEntry) queuePendingMember(srcName, srcDesc, true);
			} else {
				field = new FieldEntry(currentClass, srcName, srcDesc);
				field = currentClass.addFieldInternal(field);
			}
		} else if (srcDesc != null && field.srcDesc == null) {
			if (srcNsMap >= 0) {
				// delay descriptor computation until all classes have been supplied
				queuePendingMember(srcName, srcDesc, true).setSrcName(field.getSrcName());
			} else {
				field.setSrcDescInternal(srcDesc);
			}
		}

		currentEntry = field;

		return true;
	}

	@Override
	public boolean visitMethod(String srcName, @Nullable String srcDesc) {
		if (currentClass == null) throw new UnsupportedOperationException("Tried to visit method before owning class");

		MethodEntry method = currentClass.getMethod(srcName, srcDesc, srcNsMap);

		if (method == null) {
			if (srcNsMap >= 0) { // tree-side srcName unknown, can't create new entry directly
				method = (MethodEntry) queuePendingMember(srcName, srcDesc, false);
			} else {
				method = new MethodEntry(currentClass, srcName, srcDesc);
				method = currentClass.addMethodInternal(method);
			}
		} else if (isValidDescriptor(srcDesc, true) && !isValidDescriptor(method.srcDesc, true)) {
			if (srcNsMap >= 0) {
				// delay descriptor computation until all classes have been supplied
				queuePendingMember(srcName, srcDesc, false).setSrcName(method.getSrcName());
			} else {
				method.setSrcDescInternal(srcDesc);
			}
		}

		currentEntry = currentMethod = method;

		return true;
	}

	private ClassEntry queuePendingClass(String name) {
		if (pendingClasses == null) pendingClasses = new HashMap<>();
		ClassEntry cls = pendingClasses.get(name);

		if (cls == null) {
			cls = new ClassEntry(this, null);
			pendingClasses.put(name, cls);
		}

		assert srcNsMap >= 0;
		cls.setDstNameInternal(name, srcNsMap);

		return cls;
	}

	private MemberEntry<?> queuePendingMember(String name, @Nullable String desc, boolean isField) {
		if (pendingMembers == null) pendingMembers = new HashMap<>();
		GlobalMemberKey key = new GlobalMemberKey(currentClass, name, desc, isField);
		MemberEntry<?> member = pendingMembers.get(key);

		if (member == null) {
			if (isField) {
				member = new FieldEntry(currentClass, null, desc); // we're misusing the srcDesc field to store the dstDesc (as there is no dstDesc field)
			} else {
				member = new MethodEntry(currentClass, null, desc);
			}

			pendingMembers.put(key, member);
		}

		assert srcNsMap >= 0;
		member.setDstNameInternal(name, srcNsMap);

		return member;
	}

	private void addPendingClass(ClassEntry cls) {
		if (cls.isSrcNameMissing()) {
			return;
		}

		String srcName = cls.getSrcName();
		ClassEntry existing = classesBySrcName.get(srcName);

		if (existing == null) {
			classesBySrcName.put(srcName, cls);
		} else { // copy remaining data
			existing.copyFrom(cls, true);
		}
	}

	private void addPendingMember(MemberEntry<?> member) {
		if (member.isSrcNameMissing() || member.getOwner().isSrcNameMissing()) {
			return;
		}

		// Make sure the owner reference is pointing to an in-tree entry
		ClassEntry owner = classesBySrcName.get(member.getOwner().getSrcName());
		member.setOwner(owner);
		boolean isField = member.getKind() == MappedElementKind.FIELD;
		String srcName = member.getSrcName();
		String dstDesc = member.getSrcDesc(); // pending members' srcDesc is actually their dst desc
		String srcDesc = null;

		if (isValidDescriptor(dstDesc, !isField)) {
			srcDesc = mapDesc(dstDesc, srcNsMap, SRC_NAMESPACE_ID);
		}

		member.setSrcDescInternal(srcDesc);

		if (isField) {
			FieldEntry queuedField = (FieldEntry) member;
			FieldEntry existingField = owner.getField(srcName, srcDesc);

			if (existingField == null) {
				owner.addFieldInternal(queuedField);
			} else { // copy remaining data
				existingField.copyFrom(queuedField, true);
			}
		} else {
			MethodEntry queuedMethod = (MethodEntry) member;
			MethodEntry existingMethod = owner.getMethod(srcName, srcDesc);

			if (existingMethod == null) {
				owner.addMethodInternal(queuedMethod);
			} else { // copy remaining data
				existingMethod.copyFrom(queuedMethod, true);
			}
		}
	}

	@Override
	public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) {
		if (currentMethod == null) throw new UnsupportedOperationException("Tried to visit method argument before owning method");

		MethodArgEntry arg = currentMethod.getArg(argPosition, lvIndex, srcName);

		if (arg == null) {
			arg = new MethodArgEntry(currentMethod, argPosition, lvIndex, srcName);
			arg = currentMethod.addArgInternal(arg);
		} else {
			if (argPosition >= 0 && arg.argPosition < 0) arg.setArgPositionInternal(argPosition);
			if (lvIndex >= 0 && arg.lvIndex < 0) arg.setLvIndexInternal(lvIndex);

			if (srcName != null) {
				assert !srcName.isEmpty();
				arg.setSrcName(srcName);
			}
		}

		currentEntry = arg;

		return true;
	}

	@Override
	public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
		if (currentMethod == null) throw new UnsupportedOperationException("Tried to visit method variable before owning method");

		MethodVarEntry var = currentMethod.getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);

		if (var == null) {
			var = new MethodVarEntry(currentMethod, lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
			var = currentMethod.addVarInternal(var);
		} else {
			if (lvtRowIndex >= 0 && var.lvtRowIndex < 0) var.setLvtRowIndexInternal(lvtRowIndex);
			if (lvIndex >= 0 && startOpIdx >= 0 && (var.lvIndex < 0 || var.startOpIdx < 0)) var.setLvIndexInternal(lvIndex, startOpIdx, endOpIdx);

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
		// TODO: Don't discard pending elements which are still missing their tree-side src names, upcoming visit passes might provide them
		if (pendingClasses != null) {
			for (ClassEntry cls : pendingClasses.values()) {
				addPendingClass(cls);
			}

			pendingClasses = null;
		}

		if (pendingMembers != null) {
			for (MemberEntry<?> member : pendingMembers.values()) {
				addPendingMember(member);
			}

			pendingMembers = null;
		}

		reset();

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
			if (name.equals(currentEntry.getSrcNameUnchecked())) return;

			switch (currentEntry.getKind()) {
			case CLASS:
				assert currentClass == currentEntry;
			case FIELD:
			case METHOD:
				if (currentEntry.isSrcNameMissing()) {
					currentEntry.setSrcName(name);
					return;
				}

				break;
			case METHOD_ARG:
				((MethodArgEntry) currentEntry).setSrcName(name);
				return;
			case METHOD_VAR:
				((MethodVarEntry) currentEntry).setSrcName(name);
				return;
			}

			throw new UnsupportedOperationException("can't change src name for "+currentEntry.getKind());
		} else {
			currentEntry.setDstNameInternal(name, namespace);
		}
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
		entry.setCommentInternal(comment);
	}

	private static boolean isValidDescriptor(String descriptor, boolean possiblyMethod) {
		if (descriptor == null) {
			return false;
		}

		if (possiblyMethod && descriptor.endsWith(")")) {
			return false; // Parameter-only descriptor (Proguard?)
		}

		return true;
	}

	void assertNotInVisitPass() {
		if (inVisitPass) {
			throw new UnsupportedOperationException("Attempted illegal tree interaction via tree-API during an ongoing visitation pass");
		}
	}

	abstract static class Entry<T extends Entry<T>> implements ElementMapping {
		protected Entry(MemoryMappingTree tree, String srcName) {
			this.tree = tree;
			this.srcName = srcName;
			this.dstNames = new String[tree.dstNamespaces.size()];
		}

		protected Entry(MemoryMappingTree tree, ElementMapping src, int srcNsEquivalent) {
			this(tree, src.getName(srcNsEquivalent));

			for (int i = 0; i < dstNames.length; i++) {
				int dstNsEquivalent = src.getTree().getNamespaceId(tree.dstNamespaces.get(i));

				if (dstNsEquivalent != NULL_NAMESPACE_ID) {
					setDstNameInternal(src.getDstName(dstNsEquivalent), i);
				}
			}

			setCommentInternal(src.getComment());
		}

		public abstract MappedElementKind getKind();

		final boolean isSrcNameMissing() {
			return srcName == null;
		}

		String getSrcNameUnchecked() {
			return srcName;
		}

		@Override
		public final String getSrcName() {
			if (!missingSrcNameAllowed) {
				assertSrcNamePresent();
			}

			return srcName;
		}

		protected final void assertSrcNamePresent() {
			if (isSrcNameMissing()) {
				throw new UnsupportedOperationException("Attempted illegal interaction with a pending entry still missing its tree-side source name");
			}
		}

		void setSrcName(String name) {
			if (!missingSrcNameAllowed && name == null) {
				throw new UnsupportedOperationException("Source name cannot be null");
			}

			srcName = name;
		}

		@Override
		@Nullable
		public final String getDstName(int namespace) {
			return dstNames[namespace];
		}

		@Override
		public final void setDstName(String name, int namespace) {
			tree.assertNotInVisitPass();
			setDstNameInternal(name, namespace);
		}

		void setDstNameInternal(String name, int namespace) {
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
		@Nullable
		public final String getComment() {
			return comment;
		}

		@Override
		public final void setComment(String comment) {
			tree.assertNotInVisitPass();
			setCommentInternal(comment);
		}

		void setCommentInternal(String comment) {
			this.comment = comment;
		}

		protected final boolean acceptElement(MappingVisitor visitor, @Nullable String[] dstDescs) throws IOException {
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
		}

		private final boolean missingSrcNameAllowed = getKind().level > MappedElementKind.METHOD.level; // args and vars
		protected final MemoryMappingTree tree;
		private String srcName;
		protected String[] dstNames;
		protected String comment;
	}

	static final class ClassEntry extends Entry<ClassEntry> implements ClassMapping {
		ClassEntry(MemoryMappingTree tree, String srcName) {
			super(tree, srcName);
		}

		ClassEntry(MemoryMappingTree tree, ClassMapping src, int srcNsEquivalent) {
			super(tree, src, srcNsEquivalent);

			for (FieldMapping field : src.getFields()) {
				addFieldInternal(field);
			}

			for (MethodMapping method : src.getMethods()) {
				addMethodInternal(method);
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
		void setDstNameInternal(String name, int namespace) {
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

			super.setDstNameInternal(name, namespace);
		}

		@Override
		public Collection<FieldEntry> getFields() {
			if (fields == null) return Collections.emptyList();

			return fieldsView;
		}

		@Override
		@Nullable
		public FieldEntry getField(String srcName, @Nullable String srcDesc) {
			return getMember(srcName, srcDesc, fields, flags, FLAG_HAS_ANY_FIELD_DESC, FLAG_MISSES_ANY_FIELD_DESC);
		}

		@Override
		@Nullable
		public FieldEntry getField(String name, @Nullable String desc, int namespace) {
			return (FieldEntry) ClassMapping.super.getField(name, desc, namespace);
		}

		@Override
		public FieldEntry addField(FieldMapping field) {
			tree.assertNotInVisitPass();
			return addFieldInternal(field);
		}

		FieldEntry addFieldInternal(FieldMapping field) {
			FieldEntry entry = field instanceof FieldEntry && field.getOwner() == this ? (FieldEntry) field : new FieldEntry(this, field, tree.getSrcNsEquivalent(field));

			if (fields == null) {
				fields = new LinkedHashMap<>();
				fieldsView = Collections.unmodifiableCollection(fields.values());
			}

			return addMember(entry, fields, FLAG_HAS_ANY_FIELD_DESC, FLAG_MISSES_ANY_FIELD_DESC);
		}

		@Override
		@Nullable
		public FieldEntry removeField(String srcName, @Nullable String srcDesc) {
			tree.assertNotInVisitPass();

			FieldEntry ret = getField(srcName, srcDesc);
			if (ret != null) fields.remove(ret.getKey());

			return ret;
		}

		@Override
		public Collection<MethodEntry> getMethods() {
			if (methods == null) return Collections.emptyList();

			return methodsView;
		}

		@Override
		@Nullable
		public MethodEntry getMethod(String srcName, @Nullable String srcDesc) {
			return getMember(srcName, srcDesc, methods, flags, FLAG_HAS_ANY_METHOD_DESC, FLAG_MISSES_ANY_METHOD_DESC);
		}

		@Override
		@Nullable
		public MethodEntry getMethod(String name, @Nullable String desc, int namespace) {
			return (MethodEntry) ClassMapping.super.getMethod(name, desc, namespace);
		}

		@Override
		public MethodEntry addMethod(MethodMapping method) {
			tree.assertNotInVisitPass();
			return addMethodInternal(method);
		}

		MethodEntry addMethodInternal(MethodMapping method) {
			MethodEntry entry = method instanceof MethodEntry && method.getOwner() == this ? (MethodEntry) method : new MethodEntry(this, method, tree.getSrcNsEquivalent(method));

			if (methods == null) {
				methods = new LinkedHashMap<>();
				methodsView = Collections.unmodifiableCollection(methods.values());
			}

			return addMember(entry, methods, FLAG_HAS_ANY_METHOD_DESC, FLAG_MISSES_ANY_METHOD_DESC);
		}

		@Override
		@Nullable
		public MethodEntry removeMethod(String srcName, @Nullable String srcDesc) {
			tree.assertNotInVisitPass();

			MethodEntry ret = getMethod(srcName, srcDesc);
			if (ret != null) methods.remove(ret.getKey());

			return ret;
		}

		private static <T extends MemberEntry<T>> T getMember(String srcName, @Nullable String srcDesc,
				@Nullable Map<MemberKey, T> map, int flags, int flagHasAny, int flagMissesAny) {
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
						if (entry.getSrcName().equals(srcName)) return entry;
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
						if (entry.getSrcName().equals(srcName)
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
							if (entry.getSrcName().equals(srcName)
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
			T ret = map.putIfAbsent(entry.getKey(), entry);

			if (ret != null) { // same desc
				ret.copyFrom(entry, true);

				return ret;
			} else if (isValidDescriptor(entry.srcDesc, true)) { // may have replaced desc-less
				flags |= flagHasAny;

				if ((flags & flagMissesAny) != 0) {
					ret = map.remove(new MemberKey(entry.getSrcName(), null));

					if (ret != null) { // compatible entry exists, copy desc + extra content
						ret.setKey(entry.getKey());
						ret.srcDesc = entry.srcDesc;
						map.put(ret.getKey(), ret);
						ret.copyFrom(entry, true);
						entry = ret;
					}
				}

				return entry;
			} else { // entry.srcDesc == null, may have replaced desc-containing
				if ((flags & flagHasAny) != 0) {
					for (T prevEntry : map.values()) {
						if (prevEntry != entry && prevEntry.getSrcName().equals(entry.getSrcName()) && (entry.srcDesc == null || prevEntry.srcDesc.startsWith(entry.srcDesc))) {
							map.remove(entry.getKey());
							prevEntry.copyFrom(entry, true);

							return prevEntry;
						}
					}
				}

				flags |= flagMissesAny;

				return entry;
			}
		}

		void accept(MappingVisitor visitor, VisitOrder order, boolean supplyFieldDstDescs, boolean supplyMethodDstDescs) throws IOException {
			if (visitor.visitClass(getSrcName()) && acceptElement(visitor, null)) {
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
					FieldEntry field = getField(oField.getSrcName(), oField.srcDesc);

					if (field == null) { // missing
						addFieldInternal(oField);
					} else {
						if (oField.srcDesc != null && field.srcDesc == null) { // extra location info
							fields.remove(field.getKey());
							field.setKey(oField.getKey());
							field.srcDesc = oField.srcDesc;
							fields.put(field.getKey(), field);

							flags |= FLAG_HAS_ANY_FIELD_DESC;
						}

						field.copyFrom(oField, replace);
					}
				}
			}

			if (o.methods != null) {
				for (MethodEntry oMethod : o.methods.values()) {
					MethodEntry method = getMethod(oMethod.getSrcName(), oMethod.srcDesc);

					if (method == null) { // missing
						addMethodInternal(oMethod);
					} else {
						if (oMethod.srcDesc != null && method.srcDesc == null) { // extra location info
							methods.remove(method.getKey());
							method.setKey(oMethod.getKey());
							method.srcDesc = oMethod.srcDesc;
							methods.put(method.getKey(), method);

							flags |= FLAG_HAS_ANY_METHOD_DESC;
						}

						method.copyFrom(oMethod, replace);
					}
				}
			}
		}

		@Override
		public String toString() {
			return getSrcNameUnchecked();
		}

		private static final byte FLAG_HAS_ANY_FIELD_DESC = 1;
		private static final byte FLAG_MISSES_ANY_FIELD_DESC = 2;
		private static final byte FLAG_HAS_ANY_METHOD_DESC = 4;
		private static final byte FLAG_MISSES_ANY_METHOD_DESC = 8;

		private Map<MemberKey, FieldEntry> fields = null;
		private Map<MemberKey, MethodEntry> methods = null;
		private Collection<FieldEntry> fieldsView = null;
		private Collection<MethodEntry> methodsView = null;
		private byte flags;
	}

	abstract static class MemberEntry<T extends MemberEntry<T>> extends Entry<T> implements MemberMapping {
		protected MemberEntry(ClassEntry owner, String srcName, @Nullable String srcDesc) {
			super(owner.tree, srcName);

			this.owner = owner;
			this.srcDesc = srcDesc;
			this.key = new MemberKey(srcName, srcDesc);
		}

		protected MemberEntry(ClassEntry owner, MemberMapping src, int srcNsEquivalent) {
			super(owner.tree, src, srcNsEquivalent);

			this.owner = owner;
			this.srcDesc = src.getDesc(srcNsEquivalent);
			this.key = new MemberKey(getSrcName(), srcDesc);
		}

		@Override
		public MappingTree getTree() {
			return owner.tree;
		}

		@Override
		public final ClassEntry getOwner() {
			return owner;
		}

		void setOwner(ClassEntry owner) {
			assert tree.inVisitPass;
			assert owner.getSrcName().equals(this.owner.getSrcName());
			this.owner = owner;
		}

		@Override
		void setSrcName(String name) {
			assert tree.inVisitPass;
			super.setSrcName(name);
			key = new MemberKey(name, srcDesc);
		}

		@Override
		@Nullable
		public final String getSrcDesc() {
			return srcDesc;
		}

		abstract void setSrcDescInternal(@Nullable String desc);

		MemberKey getKey() {
			assertSrcNamePresent();
			return key;
		}

		void setKey(MemberKey key) {
			this.key = key;
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

		protected ClassEntry owner;
		protected String srcDesc;
		private MemberKey key;
	}

	static final class FieldEntry extends MemberEntry<FieldEntry> implements FieldMapping {
		FieldEntry(ClassEntry owner, String srcName, @Nullable String srcDesc) {
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
		public void setSrcDesc(@Nullable String desc) {
			tree.assertNotInVisitPass();
			setSrcDescInternal(desc);
		}

		@Override
		void setSrcDescInternal(@Nullable String desc) {
			if (Objects.equals(desc, srcDesc)) return;

			MemberKey newKey = new MemberKey(getSrcName(), desc);

			if (owner.fields != null) { // pending member
				if (owner.fields.containsKey(newKey)) throw new IllegalArgumentException("conflicting name+desc after changing desc to "+desc+" for "+this);
				owner.fields.remove(getKey());
			}

			srcDesc = desc;
			setKey(newKey);

			if (owner.fields != null) {
				owner.fields.put(newKey, this);
			}

			if (desc != null) {
				owner.flags |= ClassEntry.FLAG_HAS_ANY_FIELD_DESC;
			} else {
				owner.flags |= ClassEntry.FLAG_MISSES_ANY_FIELD_DESC;
			}
		}

		void accept(MappingVisitor visitor, boolean supplyDstDescs) throws IOException {
			if (visitor.visitField(getSrcName(), srcDesc)) {
				acceptMember(visitor, supplyDstDescs);
			}
		}

		@Override
		public String toString() {
			return String.format("%s;;%s", getSrcNameUnchecked(), srcDesc);
		}
	}

	static final class MethodEntry extends MemberEntry<MethodEntry> implements MethodMapping {
		MethodEntry(ClassEntry owner, String srcName, @Nullable String srcDesc) {
			super(owner, srcName, srcDesc);
		}

		MethodEntry(ClassEntry owner, MethodMapping src, int srcNsEquivalent) {
			super(owner, src, srcNsEquivalent);

			for (MethodArgMapping arg : src.getArgs()) {
				addArgInternal(arg);
			}

			for (MethodVarMapping var : src.getVars()) {
				addVarInternal(var);
			}
		}

		@Override
		public MappedElementKind getKind() {
			return MappedElementKind.METHOD;
		}

		@Override
		public void setSrcDesc(@Nullable String desc) {
			tree.assertNotInVisitPass();
			setSrcDescInternal(desc);
		}

		@Override
		void setSrcDescInternal(@Nullable String desc) {
			if (Objects.equals(desc, srcDesc)) return;

			MemberKey newKey = new MemberKey(getSrcName(), desc);

			if (owner.methods != null) { // pending member
				if (owner.methods.containsKey(newKey)) throw new IllegalArgumentException("conflicting name+desc after changing desc to "+desc+" for "+this);
				owner.methods.remove(getKey());
			}

			srcDesc = desc;
			setKey(newKey);

			if (owner.methods != null) {
				owner.methods.put(newKey, this);
			}

			if (isValidDescriptor(desc, true)) {
				owner.flags |= ClassEntry.FLAG_HAS_ANY_METHOD_DESC;
			} else {
				owner.flags |= ClassEntry.FLAG_MISSES_ANY_METHOD_DESC;
			}
		}

		@Override
		public Collection<MethodArgEntry> getArgs() {
			if (args == null) return Collections.emptyList();

			return argsView;
		}

		@Override
		@Nullable
		public MethodArgEntry getArg(int argPosition, int lvIndex, @Nullable String srcName) {
			if (args == null) return null;

			if (argPosition >= 0 || lvIndex >= 0) {
				for (MethodArgEntry entry : args) {
					if (argPosition >= 0 && entry.argPosition == argPosition
							|| lvIndex >= 0 && entry.lvIndex == lvIndex) {
						if (srcName != null && entry.getSrcName() != null && !srcName.equals(entry.getSrcName())) continue; // both srcNames are present but not equal
						return entry;
					}
				}
			}

			if (srcName != null) {
				for (MethodArgEntry entry : args) {
					if (srcName.equals(entry.getSrcName())
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
			tree.assertNotInVisitPass();
			return addArgInternal(arg);
		}

		MethodArgEntry addArgInternal(MethodArgMapping arg) {
			MethodArgEntry entry = arg instanceof MethodArgEntry && arg.getMethod() == this ? (MethodArgEntry) arg : new MethodArgEntry(this, arg, owner.tree.getSrcNsEquivalent(arg));
			MethodArgEntry prev = getArg(arg.getArgPosition(), arg.getLvIndex(), arg.getSrcName());

			if (prev == null) {
				if (args == null) {
					args = new ArrayList<>();
					argsView = Collections.unmodifiableList(args);
				}

				args.add(entry);
			} else {
				prev.copyFrom(entry, true);
			}

			return entry;
		}

		@Override
		@Nullable
		public MethodArgEntry removeArg(int argPosition, int lvIndex, @Nullable String srcName) {
			tree.assertNotInVisitPass();

			MethodArgEntry ret = getArg(argPosition, lvIndex, srcName);
			if (ret != null) args.remove(ret);

			return ret;
		}

		@Override
		public Collection<MethodVarEntry> getVars() {
			if (vars == null) return Collections.emptyList();

			return varsView;
		}

		@Override
		@Nullable
		public MethodVarEntry getVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
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
							|| srcName != null && entry.getSrcName() != null && !srcName.equals(entry.getSrcName())) { // different srcName
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
					if (srcName.equals(entry.getSrcName())
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
			tree.assertNotInVisitPass();
			return addVarInternal(var);
		}

		MethodVarEntry addVarInternal(MethodVarMapping var) {
			MethodVarEntry entry = var instanceof MethodVarEntry && var.getMethod() == this ? (MethodVarEntry) var : new MethodVarEntry(this, var, owner.tree.getSrcNsEquivalent(var));
			MethodVarEntry prev = getVar(var.getLvtRowIndex(), var.getLvIndex(), var.getStartOpIdx(), var.getEndOpIdx(), var.getSrcName());

			if (prev == null) {
				if (vars == null) {
					vars = new ArrayList<>();
					varsView = Collections.unmodifiableList(vars);
				}

				vars.add(entry);
			} else {
				prev.copyFrom(entry, true);
			}

			return entry;
		}

		@Override
		@Nullable
		public MethodVarEntry removeVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
			tree.assertNotInVisitPass();

			MethodVarEntry ret = getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
			if (ret != null) vars.remove(ret);

			return ret;
		}

		void accept(MappingVisitor visitor, VisitOrder order, boolean supplyDstDescs) throws IOException {
			if (visitor.visitMethod(getSrcName(), srcDesc) && acceptMember(visitor, supplyDstDescs)) {
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
					MethodArgEntry arg = getArg(oArg.argPosition, oArg.lvIndex, oArg.getSrcName());

					if (arg == null) { // missing
						addArgInternal(oArg);
					} else {
						arg.copyFrom(oArg, replace);
					}
				}
			}

			if (o.vars != null) {
				for (MethodVarEntry oVar : o.vars) {
					MethodVarEntry var = getVar(oVar.lvtRowIndex, oVar.lvIndex, oVar.startOpIdx, oVar.endOpIdx, oVar.getSrcName());

					if (var == null) { // missing
						addVarInternal(oVar);
					} else {
						var.copyFrom(oVar, replace);
					}
				}
			}
		}

		@Override
		public String toString() {
			return String.format("%s%s", getSrcNameUnchecked(), srcDesc);
		}

		private List<MethodArgEntry> args = null;
		private List<MethodVarEntry> vars = null;
		private List<MethodArgEntry> argsView = null;
		private List<MethodVarEntry> varsView = null;
	}

	static final class MethodArgEntry extends Entry<MethodArgEntry> implements MethodArgMapping {
		MethodArgEntry(MethodEntry method, int argPosition, int lvIndex, @Nullable String srcName) {
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
			tree.assertNotInVisitPass();
			setArgPositionInternal(position);
		}

		void setArgPositionInternal(int position) {
			this.argPosition = position;
		}

		@Override
		public int getLvIndex() {
			return lvIndex;
		}

		@Override
		public void setLvIndex(int index) {
			tree.assertNotInVisitPass();
			setLvIndexInternal(index);
		}

		void setLvIndexInternal(int index) {
			this.lvIndex = index;
		}

		void accept(MappingVisitor visitor) throws IOException {
			if (visitor.visitMethodArg(argPosition, lvIndex, getSrcName())) {
				acceptElement(visitor, null);
			}
		}

		@Override
		protected void copyFrom(MethodArgEntry o, boolean replace) {
			if (o.argPosition >= 0 && (replace || argPosition < 0)) {
				setArgPositionInternal(o.argPosition);
			}

			if (o.lvIndex >= 0 && (replace || lvIndex < 0)) {
				setLvIndexInternal(o.getLvIndex());
			}

			if (o.getSrcName() != null && (replace || getSrcName() == null)) {
				setSrcName(o.getSrcName());
			}

			super.copyFrom(o, replace);
		}

		@Override
		public String toString() {
			return String.format("%d/%d:%s", argPosition, lvIndex, getSrcName());
		}

		private final MethodEntry method;
		private int argPosition;
		private int lvIndex;
	}

	static final class MethodVarEntry extends Entry<MethodVarEntry> implements MethodVarMapping {
		MethodVarEntry(MethodEntry method, int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
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
			tree.assertNotInVisitPass();
			setLvtRowIndexInternal(index);
		}

		void setLvtRowIndexInternal(int index) {
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
			tree.assertNotInVisitPass();
			setLvIndexInternal(lvIndex, startOpIdx, endOpIdx);
		}

		void setLvIndexInternal(int lvIndex, int startOpIdx, int endOpIdx) {
			this.lvIndex = lvIndex;
			this.startOpIdx = startOpIdx;
			this.endOpIdx = endOpIdx;
		}

		void accept(MappingVisitor visitor) throws IOException {
			if (visitor.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, getSrcName())) {
				acceptElement(visitor, null);
			}
		}

		@Override
		protected void copyFrom(MethodVarEntry o, boolean replace) {
			if (o.lvtRowIndex >= 0 && (replace || lvtRowIndex < 0)) {
				setLvtRowIndexInternal(o.lvtRowIndex);
			}

			if (o.lvIndex >= 0 && o.startOpIdx >= 0 && (replace || lvIndex < 0 || startOpIdx < 0)) {
				setLvIndexInternal(o.lvIndex, o.startOpIdx, o.endOpIdx);
			}

			if (o.getSrcName() != null && (replace || getSrcName() == null)) {
				setSrcName(o.getSrcName());
			}

			super.copyFrom(o, replace);
		}

		@Override
		public String toString() {
			return String.format("%d/%d@%d-%d:%s", lvtRowIndex, lvIndex, startOpIdx, endOpIdx, getSrcName());
		}

		private final MethodEntry method;
		private int lvtRowIndex;
		private int lvIndex;
		private int startOpIdx;
		private int endOpIdx;
	}

	static final class MemberKey {
		MemberKey(@Nullable String name, @Nullable String desc) {
			this.name = name;
			this.desc = desc;

			if (name == null) {
				hash = super.hashCode();
			} else if (desc == null) {
				hash = name.hashCode();
			} else {
				hash = name.hashCode() * 257 + desc.hashCode();
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != MemberKey.class) {
				return false;
			}

			MemberKey o = (MemberKey) obj;

			if (name == null || o.name == null) {
				return false;
			}

			return Objects.equals(name, o.name) && Objects.equals(desc, o.desc);
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
		MetadataEntryImpl(String key, @Nullable String value) {
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

		@Override
		public String toString() {
			return key + ":" + value;
		}

		final String key;
		final String value;
	}

	static final class GlobalMemberKey {
		GlobalMemberKey(ClassEntry owner, String name, @Nullable String desc, boolean isField) {
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

	private boolean inVisitPass;
	private boolean indexByDstNames;
	private String srcNamespace;
	private List<String> dstNamespaces = Collections.emptyList();
	private final List<MetadataEntry> metadata = new ArrayList<>();
	private final Map<String, ClassEntry> classesBySrcName = new LinkedHashMap<>();
	private final Collection<ClassEntry> classesView = Collections.unmodifiableCollection(classesBySrcName.values());
	private Map<String, ClassEntry>[] classesByDstNames;

	private HierarchyInfoProvider<?> hierarchyInfo;

	/** The incoming source namespace's namespace index on the tree side. */
	private int srcNsMap;
	private int[] dstNameMap;
	private Entry<?> currentEntry;
	private ClassEntry currentClass;
	private MethodEntry currentMethod;
	/** originalSrcName -> clsEntry. */
	private Map<String, ClassEntry> pendingClasses;
	private Map<GlobalMemberKey, MemberEntry<?>> pendingMembers;
}

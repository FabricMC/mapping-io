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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.ElementMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MemberMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodArgMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodVarMappingView;

/**
 * Visitation order configuration for {@link MappingTreeView#accept(net.fabricmc.mappingio.MappingVisitor, VisitOrder)}.
 */
public final class VisitOrder {
	// pre-defined orders

	public static VisitOrder createByInputOrder() {
		return new VisitOrder();
	}

	public static VisitOrder createByName() {
		return new VisitOrder()
				.classesBySrcName()
				.fieldsBySrcNameDesc()
				.methodsBySrcNameDesc()
				.methodArgsByLvIndex()
				.methodVarsByLvIndex();
	}

	// visit order configuration

	public VisitOrder classComparator(Comparator<ClassMappingView> comparator) {
		this.classComparator = comparator;

		return this;
	}

	public VisitOrder classesBySrcName() {
		return classComparator(compareBySrcName());
	}

	public VisitOrder fieldComparator(Comparator<FieldMappingView> comparator) {
		this.fieldComparator = comparator;

		return this;
	}

	public VisitOrder fieldsBySrcNameDesc() {
		return fieldComparator(compareBySrcNameDesc());
	}

	public VisitOrder methodComparator(Comparator<MethodMappingView> comparator) {
		this.methodComparator = comparator;

		return this;
	}

	public VisitOrder methodsBySrcNameDesc() {
		return methodComparator(compareBySrcNameDesc());
	}

	public VisitOrder methodArgComparator(Comparator<MethodArgMappingView> comparator) {
		this.methodArgComparator = comparator;

		return this;
	}

	public VisitOrder methodArgsByPosition() {
		return methodArgComparator(Comparator.comparingInt(MethodArgMappingView::getArgPosition));
	}

	public VisitOrder methodArgsByLvIndex() {
		return methodArgComparator(Comparator.comparingInt(MethodArgMappingView::getLvIndex));
	}

	public VisitOrder methodVarComparator(Comparator<MethodVarMappingView> comparator) {
		this.methodVarComparator = comparator;

		return this;
	}

	public VisitOrder methodVarsByLvtRowIndex() {
		return methodVarComparator(Comparator.comparingInt(MethodVarMappingView::getLvIndex).thenComparingInt(MethodVarMappingView::getLvtRowIndex));
	}

	public VisitOrder methodVarsByLvIndex() {
		return methodVarComparator(Comparator.comparingInt(MethodVarMappingView::getLvIndex).thenComparingInt(MethodVarMappingView::getStartOpIdx));
	}

	public VisitOrder methodsFirst(boolean methodsFirst) {
		this.methodsFirst = methodsFirst;

		return this;
	}

	public VisitOrder fieldsFirst() {
		return methodsFirst(false);
	}

	public VisitOrder methodsFirst() {
		return methodsFirst(true);
	}

	public VisitOrder methodVarsFirst(boolean varsFirst) {
		this.methodVarsFirst = varsFirst;

		return this;
	}

	public VisitOrder methodArgsFirst() {
		return methodVarsFirst(false);
	}

	public VisitOrder methodVarsFirst() {
		return methodVarsFirst(true);
	}

	// customization helpers

	public static <T extends ElementMappingView> Comparator<T> compareBySrcName() {
		return (a, b) -> compare(a.getSrcName(), b.getSrcName());
	}

	public static <T extends MemberMappingView> Comparator<T> compareBySrcNameDesc() {
		return (a, b) -> {
			int cmp = compare(a.getSrcName(), b.getSrcName());

			return cmp != 0 ? cmp : compare(a.getSrcDesc(), b.getSrcDesc());
		};
	}

	public static Comparator<ElementMappingView> compareBySrcNameShortFirst() {
		return (a, b) -> compareShortFirst(a.getSrcName(), b.getSrcName());
	}

	public static int compare(String a, String b) {
		if (a == null || b == null) return compareNullLast(a, b);

		return a.compareTo(b);
	}

	public static int compareShortFirst(String a, String b) {
		if (a == null || b == null) return compareNullLast(a, b);

		int cmp = a.length() - b.length();

		return cmp != 0 ? cmp : a.compareTo(b);
	}

	public static int compareShortFirst(String a, int startA, int endA, String b, int startB, int endB) {
		int lenA = endA - startA;
		int ret = Integer.compare(lenA, endB - startB);
		if (ret != 0) return ret;

		for (int i = 0; i < lenA; i++) {
			char ca = a.charAt(startA + i);
			char cb = b.charAt(startB + i);

			if (ca != cb) {
				return ca - cb;
			}
		}

		return 0;
	}

	public static int byNameShortFirstNestaware(String a, String b) {
		if (a == null || b == null) {
			return compareNullLast(a, b);
		}

		int pos = 0;

		do {
			int endA = a.indexOf('$', pos);
			int endB = b.indexOf('$', pos);

			int ret = compareShortFirst(a, pos, endA >= 0 ? endA : a.length(),
					b, pos, endB >= 0 ? endB : b.length());

			if (ret != 0) {
				return ret;
			} else if ((endA < 0) != (endB < 0)) {
				return endA < 0 ? -1 : 1;
			}

			pos = endA + 1;
		} while (pos > 0);

		return 0;
	}

	public static int compareNullLast(String a, String b) {
		if (a == null) {
			if (b == null) { // both null
				return 0;
			} else { // only a null
				return 1;
			}
		} else if (b == null) { // only b null
			return -1;
		} else { // neither null
			return a.compareTo(b);
		}
	}

	// application

	public <T extends ClassMappingView> Collection<T> sortClasses(Collection<T> classes) {
		return sort(classes, classComparator);
	}

	public <T extends FieldMappingView> Collection<T> sortFields(Collection<T> fields) {
		return sort(fields, fieldComparator);
	}

	public <T extends MethodMappingView> Collection<T> sortMethods(Collection<T> methods) {
		return sort(methods, methodComparator);
	}

	public <T extends MethodArgMappingView> Collection<T> sortMethodArgs(Collection<T> args) {
		return sort(args, methodArgComparator);
	}

	public <T extends MethodVarMappingView> Collection<T> sortMethodVars(Collection<T> vars) {
		return sort(vars, methodVarComparator);
	}

	private static <T> Collection<T> sort(Collection<T> inputs, Comparator<? super T> comparator) {
		if (comparator == null || inputs.size() < 2) return inputs;

		List<T> ret = new ArrayList<>(inputs);
		ret.sort(comparator);

		return ret;
	}

	public boolean isMethodsFirst() {
		return methodsFirst;
	}

	public boolean isMethodVarsFirst() {
		return methodVarsFirst;
	}

	private Comparator<ClassMappingView> classComparator;
	private Comparator<FieldMappingView> fieldComparator;
	private Comparator<MethodMappingView> methodComparator;
	private Comparator<MethodArgMappingView> methodArgComparator;
	private Comparator<MethodVarMappingView> methodVarComparator;
	private boolean methodsFirst;
	private boolean methodVarsFirst;
}

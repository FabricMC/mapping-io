/*
 * Copyright (c) 2024 FabricMC
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

package net.fabricmc.mappingio.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.fabricmc.mappingio.CommentStyle;
import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

class NameGen {
	boolean visitClass(MappingVisitor target, int... dstNs) throws IOException {
		return visitInnerClass(target, 0, dstNs);
	}

	boolean visitInnerClass(MappingVisitor target, int nestLevel, int... dstNs) throws IOException {
		if (!target.visitClass(nestLevel <= 0 ? srcOutermostClass() : srcInnerClasses(nestLevel))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(clsKind, ns, nestLevel <= 0 ? dstOutermostClass(ns) : dstInnerClasses(ns));
		}

		return target.visitElementContent(clsKind);
	}

	boolean visitField(MappingVisitor target, int... dstNs) throws IOException {
		String desc;

		if (!target.visitField(src(fldKind), desc = desc(fldKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(fldKind, ns, dst(fldKind, ns));
			target.visitDstDesc(fldKind, ns, desc);
		}

		return target.visitElementContent(fldKind);
	}

	boolean visitMethod(MappingVisitor target, int... dstNs) throws IOException {
		String desc;

		if (!target.visitMethod(src(mthKind), desc = desc(mthKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(mthKind, ns, dst(mthKind, ns));
			target.visitDstDesc(mthKind, ns, desc);
		}

		return target.visitElementContent(mthKind);
	}

	boolean visitMethodArg(MappingVisitor target, int... dstNs) throws IOException {
		if (!target.visitMethodArg(counter++, counter++, src(argKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(argKind, ns, dst(argKind, ns));
		}

		return target.visitElementContent(argKind);
	}

	boolean visitMethodVar(MappingVisitor target, int... dstNs) throws IOException {
		if (!target.visitMethodVar(
				counter,
				counter,
				counter++,
				counter++,
				src(varKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(varKind, ns, dst(varKind, ns));
		}

		return target.visitElementContent(varKind);
	}

	void visitComment(MappingVisitor target) throws IOException {
		target.visitComment(lastKind, comment, CommentStyle.HTML);
	}

	private String src(MappedElementKind kind) {
		nsNum = 0;
		lastKind = kind;

		if (kind == clsKind) {
			classHasDst = false;
		}

		return getPrefix(kind) + "_" + getCounter(kind).incrementAndGet();
	}

	private String srcOutermostClass() {
		innerClassNestLevel = 0;
		String ret = src(clsKind);

		if (clsNum.get() % 2 == 0) {
			ret = "package_" + clsNum.get() + "/" + ret;
		}

		return ret;
	}

	private String srcInnerClasses(/* >=1 */ int nestLevel) {
		if (innerClassNestLevel == 0) {
			clsNum.decrementAndGet(); // we need the previously generated outer class
		}

		boolean hasDst = classHasDst;
		StringBuilder sb = new StringBuilder(srcOutermostClass());

		for (int i = 0; i < nestLevel; i++) {
			sb.append('$');
			sb.append(src(clsKind));
		}

		classHasDst = hasDst;
		innerClassNestLevel = nestLevel;
		return sb.toString();
	}

	private String dst(MappedElementKind kind, int ns) {
		if (lastKind != null && lastKind != kind) {
			throw new UnsupportedOperationException("Last kind (expected): " + lastKind + ", actual: " + kind);
		}

		if (nsNum < ns) {
			nsNum = ns + 1;
		}

		if (kind == clsKind) {
			classHasDst = true;
		}

		return getPrefix(kind) + getCounter(kind).get() + "Ns" + ns + "Rename";
	}

	private String dstOutermostClass(int ns) {
		String ret = dst(clsKind, ns);

		if (clsNum.get() % 3 == 0) {
			int num = clsNum.get() % 6 == 0 && innerClassNestLevel == 0
					? clsNum.get() + 1
					: clsNum.get();
			ret = "package_" + num + "/" + ret;
		}

		return ret;
	}

	private String dstInnerClasses(int ns) {
		boolean hasDst = classHasDst;
		int nestLevel = innerClassNestLevel;
		StringBuilder sb = new StringBuilder(dst(clsKind, ns));

		for (int i = nestLevel - 1; i >= 0; i--) {
			sb.insert(0, '$');
			clsNum.decrementAndGet();

			if (!hasDst) {
				clsNum.decrementAndGet();
			}

			sb.insert(0, hasDst
					? i == 0
							? dstOutermostClass(ns)
							: dst(clsKind, ns)
					: i == 0
							? srcOutermostClass()
							: src(clsKind));
		}

		classHasDst = hasDst;
		innerClassNestLevel = nestLevel;
		clsNum.addAndGet(nestLevel);
		return sb.toString();
	}

	private String desc(MappedElementKind kind) {
		switch (kind) {
		case FIELD:
			return fldDescs.get((fldNum.get() - 1) % fldDescs.size());
		case METHOD:
			return mthDescs.get((mthNum.get() - 1) % mthDescs.size());
		default:
			throw new IllegalArgumentException("Invalid kind: " + kind);
		}
	}

	private AtomicInteger getCounter(MappedElementKind kind) {
		switch (kind) {
		case CLASS:
			return clsNum;
		case FIELD:
			return fldNum;
		case METHOD:
			return mthNum;
		case METHOD_ARG:
			return argNum;
		case METHOD_VAR:
			return varNum;
		default:
			throw new IllegalArgumentException("Unknown kind: " + kind);
		}
	}

	private String getPrefix(MappedElementKind kind) {
		switch (kind) {
		case CLASS:
			return clsPrefix;
		case FIELD:
			return fldPrefix;
		case METHOD:
			return mthPrefix;
		case METHOD_ARG:
			return argPrefix;
		case METHOD_VAR:
			return varPrefix;
		default:
			throw new IllegalArgumentException("Unknown kind: " + kind);
		}
	}

	private static final String clsPrefix = "class";
	private static final String fldPrefix = "field";
	private static final String mthPrefix = "method";
	private static final String argPrefix = "param";
	private static final String varPrefix = "var";
	private static final String comment = "This is a comment";
	private static final List<String> fldDescs = Collections.unmodifiableList(Arrays.asList("I", "Lcls;", "Lpkg/cls;", "[I", null));
	private static final List<String> mthDescs = Collections.unmodifiableList(Arrays.asList("()I", "(I)V", "(Lcls;)Lcls;", "(ILcls;)Lpkg/cls;", "(Lcls;[I)[[B", null));
	private static final MappedElementKind clsKind = MappedElementKind.CLASS;
	private static final MappedElementKind fldKind = MappedElementKind.FIELD;
	private static final MappedElementKind mthKind = MappedElementKind.METHOD;
	private static final MappedElementKind argKind = MappedElementKind.METHOD_ARG;
	private static final MappedElementKind varKind = MappedElementKind.METHOD_VAR;
	private final AtomicInteger clsNum = new AtomicInteger();
	private final AtomicInteger fldNum = new AtomicInteger();
	private final AtomicInteger mthNum = new AtomicInteger();
	private final AtomicInteger argNum = new AtomicInteger();
	private final AtomicInteger varNum = new AtomicInteger();
	private int nsNum;
	private int counter;
	private MappedElementKind lastKind;
	private boolean classHasDst;
	private int innerClassNestLevel;
}

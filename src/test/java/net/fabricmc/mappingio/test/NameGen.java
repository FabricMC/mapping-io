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

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

class NameGen {
	boolean visitPackage(MappingVisitor target, int nestLevel, int... dstNs) throws IOException {
		if (!target.visitPackage(nestLevel <= 0 ? srcOutermostPkgOrCls(pkgKind) : srcInnerPkgOrCls(pkgKind, nestLevel))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(pkgKind, ns, nestLevel <= 0 ? dstOutermostPkgOrCls(pkgKind, ns) : dstInnerPkgOrCls(pkgKind, ns));
		}

		return target.visitElementContent(pkgKind);
	}

	boolean visitClass(MappingVisitor target, int... dstNs) throws IOException {
		return visitInnerClass(target, 0, dstNs);
	}

	boolean visitInnerClass(MappingVisitor target, int nestLevel, int... dstNs) throws IOException {
		if (!target.visitClass(nestLevel <= 0 ? srcOutermostPkgOrCls(clsKind) : srcInnerPkgOrCls(clsKind, nestLevel))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(clsKind, ns, nestLevel <= 0 ? dstOutermostPkgOrCls(clsKind, ns) : dstInnerPkgOrCls(clsKind, ns));
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
		target.visitComment(lastKind, comment);
	}

	private String src(MappedElementKind kind) {
		nsNum = 0;
		lastKind = kind;

		if (kind.level == 0) {
			pkgOrClsHasDst = false;
		}

		return getPrefix(kind) + "_" + getCounter(kind).incrementAndGet();
	}

	private String srcOutermostPkgOrCls(MappedElementKind kind) {
		assert kind.level == 0;

		innerPkgOrClsNestLevel = 0;
		String ret = src(kind);
		int counter = getCounter(kind).get();

		if (kind == clsKind && counter % 2 == 0) {
			ret = "package_" + counter + "/" + ret;
		}

		return ret;
	}

	private String srcInnerPkgOrCls(MappedElementKind kind, /* >=1 */ int nestLevel) {
		assert kind.level == 0;

		if (innerPkgOrClsNestLevel == 0) {
			getCounter(kind).decrementAndGet(); // we need the previously generated outer package/class
		}

		boolean hasDst = pkgOrClsHasDst;
		char separator = kind == clsKind ? '$' : '/';
		StringBuilder sb = new StringBuilder(srcOutermostPkgOrCls(kind));

		for (int i = 0; i < nestLevel; i++) {
			sb.append(separator);
			sb.append(src(kind));
		}

		pkgOrClsHasDst = hasDst;
		innerPkgOrClsNestLevel = nestLevel;
		return sb.toString();
	}

	private String dst(MappedElementKind kind, int ns) {
		if (lastKind != null && lastKind != kind) {
			throw new UnsupportedOperationException("Last kind (expected): " + lastKind + ", actual: " + kind);
		}

		if (nsNum < ns) {
			nsNum = ns + 1;
		}

		if (kind.level == 0) {
			pkgOrClsHasDst = true;
		}

		return getPrefix(kind) + getCounter(kind).get() + "Ns" + ns + "Rename";
	}

	private String dstOutermostPkgOrCls(MappedElementKind kind, int ns) {
		assert kind.level == 0;

		String ret = dst(kind, ns);
		AtomicInteger counter = getCounter(kind);

		if (counter.get() % 3 == 0) {
			int num = counter.get() % 6 == 0
					? counter.get() + 1
					: counter.get();
			String pkgName = kind == pkgKind ? "filler_" : "package_";
			ret = pkgName + num + "/" + ret;
		}

		return ret;
	}

	private String dstInnerPkgOrCls(MappedElementKind kind, int ns) {
		assert kind.level == 0;

		boolean hasDst = pkgOrClsHasDst;
		int nestLevel = innerPkgOrClsNestLevel;
		char separator = kind == clsKind ? '$' : '/';
		AtomicInteger counter = getCounter(kind);
		StringBuilder sb = new StringBuilder(dst(kind, ns));

		for (int i = nestLevel - 1; i >= 0; i--) {
			sb.insert(0, separator);
			counter.decrementAndGet();

			if (!hasDst) {
				counter.decrementAndGet();
			}

			sb.insert(0, hasDst
					? i == 0
							? dstOutermostPkgOrCls(kind, ns)
							: dst(kind, ns)
					: i == 0
							? srcOutermostPkgOrCls(kind)
							: src(kind));
		}

		pkgOrClsHasDst = hasDst;
		innerPkgOrClsNestLevel = nestLevel;
		counter.addAndGet(nestLevel);
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
		case PACKAGE:
			return pkgNum;
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
		case PACKAGE:
			return pkgPrefix;
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

	private static final String pkgPrefix = "package";
	private static final String clsPrefix = "class";
	private static final String fldPrefix = "field";
	private static final String mthPrefix = "method";
	private static final String argPrefix = "param";
	private static final String varPrefix = "var";
	private static final String comment = "This is a comment";
	private static final List<String> fldDescs = Collections.unmodifiableList(Arrays.asList("I", "Lcls;", "Lpkg/cls;", "[I", null));
	private static final List<String> mthDescs = Collections.unmodifiableList(Arrays.asList("()I", "(I)V", "(Lcls;)Lcls;", "(ILcls;)Lpkg/cls;", "(Lcls;[I)[[B", null));
	private static final MappedElementKind pkgKind = MappedElementKind.PACKAGE;
	private static final MappedElementKind clsKind = MappedElementKind.CLASS;
	private static final MappedElementKind fldKind = MappedElementKind.FIELD;
	private static final MappedElementKind mthKind = MappedElementKind.METHOD;
	private static final MappedElementKind argKind = MappedElementKind.METHOD_ARG;
	private static final MappedElementKind varKind = MappedElementKind.METHOD_VAR;
	private final AtomicInteger pkgNum = new AtomicInteger();
	private final AtomicInteger clsNum = new AtomicInteger();
	private final AtomicInteger fldNum = new AtomicInteger();
	private final AtomicInteger mthNum = new AtomicInteger();
	private final AtomicInteger argNum = new AtomicInteger();
	private final AtomicInteger varNum = new AtomicInteger();
	private int nsNum;
	private int counter;
	private MappedElementKind lastKind;
	private boolean pkgOrClsHasDst;
	private int innerPkgOrClsNestLevel;
}

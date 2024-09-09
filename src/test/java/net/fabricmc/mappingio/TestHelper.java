/*
 * Copyright (c) 2023 FabricMC
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

package net.fabricmc.mappingio;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.neoforged.srgutils.IMappingFile;
import org.cadixdev.lorenz.io.MappingFormats;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.format.intellij.MigrationMapConstants;
import net.fabricmc.mappingio.lib.jool.Unchecked;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class TestHelper {
	public static Path getResource(String slashPrefixedResourcePath) {
		try {
			return Paths.get(TestHelper.class.getResource(slashPrefixedResourcePath).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Nullable
	public static String getFileName(MappingFormat format) {
		switch (format) {
		case ENIGMA_FILE:
			return "enigma.mappings";
		case ENIGMA_DIR:
			return "enigma-dir";
		case TINY_FILE:
			return "tiny.tiny";
		case TINY_2_FILE:
			return "tinyV2.tiny";
		case SRG_FILE:
			return "srg.srg";
		case XSRG_FILE:
			return "xsrg.xsrg";
		case JAM_FILE:
			return "jam.jam";
		case CSRG_FILE:
			return "csrg.csrg";
		case TSRG_FILE:
			return "tsrg.tsrg";
		case TSRG_2_FILE:
			return "tsrgV2.tsrg";
		case PROGUARD_FILE:
			return "proguard.txt";
		case INTELLIJ_MIGRATION_MAP_FILE:
			return "migration-map.xml";
		case RECAF_SIMPLE_FILE:
			return "recaf-simple.txt";
		case JOBF_FILE:
			return "jobf.jobf";
		default:
			return null;
		}
	}

	@Nullable
	public static org.cadixdev.lorenz.io.MappingFormat toLorenzFormat(MappingFormat format) {
		switch (format) {
		case SRG_FILE:
			return MappingFormats.SRG;
		case XSRG_FILE:
			return MappingFormats.XSRG;
		case CSRG_FILE:
			return MappingFormats.CSRG;
		case TSRG_FILE:
			return MappingFormats.TSRG;
		case ENIGMA_FILE:
			return MappingFormats.byId("enigma");
		case JAM_FILE:
			return MappingFormats.byId("jam");
		case TINY_FILE:
		case TINY_2_FILE:
		case ENIGMA_DIR:
		case TSRG_2_FILE:
		case PROGUARD_FILE:
		case INTELLIJ_MIGRATION_MAP_FILE:
		case RECAF_SIMPLE_FILE:
		case JOBF_FILE:
			return null;
		default:
			throw new IllegalArgumentException("Unknown format: " + format);
		}
	}

	@Nullable
	public static IMappingFile.Format toSrgUtilsFormat(MappingFormat format) {
		switch (format) {
		case TINY_FILE:
			return IMappingFile.Format.TINY1;
		case TINY_2_FILE:
			return IMappingFile.Format.TINY;
		case SRG_FILE:
			return IMappingFile.Format.SRG;
		case XSRG_FILE:
			return IMappingFile.Format.XSRG;
		case CSRG_FILE:
			return IMappingFile.Format.CSRG;
		case TSRG_FILE:
			return IMappingFile.Format.TSRG;
		case TSRG_2_FILE:
			return IMappingFile.Format.TSRG2;
		case PROGUARD_FILE:
			return IMappingFile.Format.PG;
		case ENIGMA_FILE:
		case ENIGMA_DIR:
		case JAM_FILE:
		case INTELLIJ_MIGRATION_MAP_FILE:
		case RECAF_SIMPLE_FILE:
		case JOBF_FILE:
			return null;
		default:
			throw new IllegalArgumentException("Unknown format: " + format);
		}
	}

	public static Path writeToDir(MappingTreeView tree, Path dir, MappingFormat format) throws IOException {
		Path path = dir.resolve(getFileName(format));
		tree.accept(MappingWriter.create(path, format));
		return path;
	}

	// After any changes, run "./gradlew updateTestMappings" to update the mapping files in the resources folder accordingly
	public static <T extends MappingVisitor> T acceptTestMappings(T target) throws IOException {
		MappingVisitor delegate = target instanceof VisitOrderVerifyingVisitor ? target : new VisitOrderVerifyingVisitor(target);

		if (delegate.visitHeader()) {
			delegate.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, Arrays.asList(MappingUtil.NS_TARGET_FALLBACK, MappingUtil.NS_TARGET_FALLBACK + "2"));
			delegate.visitMetadata("name", "valid");
			delegate.visitMetadata(MigrationMapConstants.ORDER_KEY, "0");
		}

		if (delegate.visitContent()) {
			int[] dstNs = new int[] { 0, 1 };
			nameGen.reset();

			if (visitClass(delegate, dstNs)) {
				visitField(delegate, dstNs);

				if (visitMethod(delegate, dstNs)) {
					visitMethodArg(delegate, dstNs);
					visitMethodVar(delegate, dstNs);
				}
			}

			if (visitInnerClass(delegate, 1, dstNs)) {
				visitComment(delegate);
				visitField(delegate, dstNs);
			}

			visitClass(delegate, dstNs);
		}

		if (!delegate.visitEnd()) {
			acceptTestMappings(delegate);
		}

		return target;
	}

	// After any changes, run "./gradlew updateTestMappings" to update the mapping files in the resources folder accordingly.
	// Make sure to keep the few manual changes in the files.
	public static <T extends MappingVisitor> T acceptTestMappingsWithRepeats(T target, boolean repeatComments, boolean repeatClasses) throws IOException {
		acceptTestMappings(new ForwardingMappingVisitor(new VisitOrderVerifyingVisitor(target, true)) {
			private List<Runnable> replayQueue = new ArrayList<>();

			@Override
			public void visitMetadata(String key, @Nullable String value) throws IOException {
				super.visitMetadata(key, key.equals("name") ? "repeated-elements" : value);
			}

			@Override
			public boolean visitClass(String srcName) throws IOException {
				replayQueue.clear();

				if (repeatClasses) {
					replayQueue.add(Unchecked.runnable(() -> super.visitClass(srcName)));
				}

				return super.visitClass(srcName);
			}

			@Override
			public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitField(srcName, srcDesc)));
				return super.visitField(srcName, srcDesc);
			}

			@Override
			public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitMethod(srcName, srcDesc)));
				return super.visitMethod(srcName, srcDesc);
			}

			@Override
			public boolean visitMethodArg(int lvIndex, int argIndex, String srcName) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitMethodArg(lvIndex, argIndex, srcName)));
				return super.visitMethodArg(lvIndex, argIndex, srcName);
			}

			@Override
			public boolean visitMethodVar(int lvIndex, int varIndex, int startOpIdx, int endOpIdx, String srcName) throws IOException {
				replayQueue.clear();
				replayQueue.add(Unchecked.runnable(() -> super.visitMethodVar(lvIndex, varIndex, startOpIdx, endOpIdx, srcName)));
				return super.visitMethodVar(lvIndex, varIndex, startOpIdx, endOpIdx, srcName);
			}

			@Override
			public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
				if (targetKind == MappedElementKind.CLASS && !repeatClasses) {
					super.visitDstName(targetKind, namespace, name);
				} else {
					replayQueue.add(Unchecked.runnable(() -> super.visitDstName(targetKind, namespace, name)));
					super.visitDstName(targetKind, namespace, name + "0");
				}
			}

			@Override
			public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
				replayQueue.add(Unchecked.runnable(() -> super.visitDstDesc(targetKind, namespace, desc)));
				super.visitDstDesc(targetKind, namespace, desc);
			}

			@Override
			public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
				boolean ret = super.visitElementContent(targetKind);

				if (!replayQueue.isEmpty()) {
					replayQueue.forEach(Runnable::run);

					ret = super.visitElementContent(targetKind);
				}

				return ret;
			}

			@Override
			public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
				if (repeatComments) {
					super.visitComment(targetKind, comment + ".");
				}

				super.visitComment(targetKind, comment);
			}
		});

		return target;
	}

	// After any changes, run "./gradlew updateTestMappings" to update the mapping files in the resources folder accordingly
	public static <T extends MappingVisitor> T acceptTestMappingsWithHoles(T target) throws IOException {
		MappingVisitor delegate = target instanceof VisitOrderVerifyingVisitor ? target : new VisitOrderVerifyingVisitor(target);

		if (delegate.visitHeader()) {
			delegate.visitNamespaces(MappingUtil.NS_SOURCE_FALLBACK, Arrays.asList(MappingUtil.NS_TARGET_FALLBACK, MappingUtil.NS_TARGET_FALLBACK + "2"));
		}

		if (delegate.visitContent()) {
			nameGen.reset();

			// (Inner) Classes
			for (int nestLevel = 0; nestLevel <= 2; nestLevel++) {
				visitClass(delegate);
				visitInnerClass(delegate, nestLevel, 0);
				visitInnerClass(delegate, nestLevel, 1);

				if (visitInnerClass(delegate, nestLevel)) {
					visitComment(delegate);
				}

				if (visitInnerClass(delegate, nestLevel, 0)) {
					visitComment(delegate);
				}

				if (visitInnerClass(delegate, nestLevel, 1)) {
					visitComment(delegate);
				}
			}

			if (visitClass(delegate)) {
				// Fields
				visitField(delegate);
				visitField(delegate, 0);
				visitField(delegate, 1);

				if (visitField(delegate)) {
					visitComment(delegate);
				}

				if (visitField(delegate, 0)) {
					visitComment(delegate);
				}

				if (visitField(delegate, 1)) {
					visitComment(delegate);
				}

				// Methods
				visitMethod(delegate);
				visitMethod(delegate, 0);
				visitMethod(delegate, 1);

				if (visitMethod(delegate)) {
					visitComment(delegate);
				}

				if (visitMethod(delegate, 0)) {
					visitComment(delegate);
				}

				if (visitMethod(delegate, 1)) {
					visitComment(delegate);
				}

				// Method args
				if (visitMethod(delegate)) {
					visitMethodArg(delegate);
					visitMethodArg(delegate, 1);
					visitMethodArg(delegate, 0);

					if (visitMethodArg(delegate)) {
						visitComment(delegate);
					}

					if (visitMethodArg(delegate, 0)) {
						visitComment(delegate);
					}

					if (visitMethodArg(delegate, 1)) {
						visitComment(delegate);
					}
				}

				// Method vars
				if (visitMethod(delegate)) {
					visitMethodVar(delegate);
					visitMethodVar(delegate, 1);
					visitMethodVar(delegate, 0);

					if (visitMethodVar(delegate)) {
						visitComment(delegate);
					}

					if (visitMethodVar(delegate, 0)) {
						visitComment(delegate);
					}

					if (visitMethodVar(delegate, 1)) {
						visitComment(delegate);
					}
				}
			}
		}

		if (!delegate.visitEnd()) {
			acceptTestMappingsWithHoles(delegate);
		}

		return target;
	}

	private static boolean visitClass(MappingVisitor target, int... dstNs) throws IOException {
		return visitInnerClass(target, 0, dstNs);
	}

	private static boolean visitInnerClass(MappingVisitor target, int nestLevel, int... dstNs) throws IOException {
		if (!target.visitClass(nestLevel <= 0 ? nameGen.src(clsKind) : nameGen.srcInnerCls(nestLevel))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(clsKind, ns, nameGen.dst(clsKind, ns));
		}

		return target.visitElementContent(clsKind);
	}

	private static boolean visitField(MappingVisitor target, int... dstNs) throws IOException {
		String desc;

		if (!target.visitField(nameGen.src(fldKind), desc = nameGen.desc(fldKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(fldKind, ns, nameGen.dst(fldKind, ns));
			target.visitDstDesc(fldKind, ns, desc);
		}

		return target.visitElementContent(fldKind);
	}

	private static boolean visitMethod(MappingVisitor target, int... dstNs) throws IOException {
		String desc;

		if (!target.visitMethod(nameGen.src(mthKind), desc = nameGen.desc(mthKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(mthKind, ns, nameGen.dst(mthKind, ns));
			target.visitDstDesc(mthKind, ns, desc);
		}

		return target.visitElementContent(mthKind);
	}

	private static boolean visitMethodArg(MappingVisitor target, int... dstNs) throws IOException {
		if (!target.visitMethodArg(nameGen.getCounter().getAndIncrement(), nameGen.getCounter().getAndIncrement(), nameGen.src(argKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(argKind, ns, nameGen.dst(argKind, ns));
		}

		return target.visitElementContent(argKind);
	}

	private static boolean visitMethodVar(MappingVisitor target, int... dstNs) throws IOException {
		if (!target.visitMethodVar(
				nameGen.getCounter().get(),
				nameGen.getCounter().get(),
				nameGen.getCounter().getAndIncrement(),
				nameGen.getCounter().getAndIncrement(),
				nameGen.src(varKind))) {
			return false;
		}

		for (int ns : dstNs) {
			target.visitDstName(varKind, ns, nameGen.dst(varKind, ns));
		}

		return target.visitElementContent(varKind);
	}

	private static void visitComment(MappingVisitor target) throws IOException {
		target.visitComment(nameGen.lastKind.get(), comment);
	}

	private static class NameGen {
		public void reset() {
			lastKind.remove();
			innerClassNestLevel.remove();
			clsNum.get().set(0);
			fldNum.get().set(0);
			mthNum.get().set(0);
			argNum.get().set(0);
			varNum.get().set(0);
			nsNum.get().set(0);
			counter.get().set(0);
		}

		private void resetNsNum() {
			nsNum.get().set(0);
		}

		public String src(MappedElementKind kind) {
			resetNsNum();
			lastKind.set(kind);
			innerClassNestLevel.set(0);

			if (kind == MappedElementKind.CLASS) {
				outerClassHasDst.set(false);
			}

			return getPrefix(kind) + "_" + getCounter(kind).incrementAndGet();
		}

		public String srcInnerCls(/* >=1 */ int nestLevel) {
			if (innerClassNestLevel.get() == 0) clsNum.get().decrementAndGet();
			boolean hasDst = outerClassHasDst.get();
			StringBuilder sb = new StringBuilder(src(clsKind));

			for (int i = 0; i < nestLevel; i++) {
				sb.append('$');
				sb.append(src(clsKind));
			}

			outerClassHasDst.set(hasDst);
			innerClassNestLevel.set(nestLevel);
			return sb.toString();
		}

		public String dst(MappedElementKind kind) {
			return dst(kind, nsNum.get().getAndIncrement());
		}

		public String dst(MappedElementKind kind, int ns) {
			if (lastKind != null && lastKind.get() != kind) {
				throw new UnsupportedOperationException();
			}

			if (nsNum.get().get() < ns) nsNum.get().set(ns + 1);

			if (innerClassNestLevel.get().intValue() == 0) {
				outerClassHasDst.set(true);
				return getPrefix(kind) + getCounter(kind).get() + "Ns" + ns + "Rename";
			}

			boolean hasDst = outerClassHasDst.get();
			int nestLevel = innerClassNestLevel.get();
			innerClassNestLevel.set(0);
			StringBuilder sb = new StringBuilder(dst(kind, ns));

			for (int i = nestLevel - 1; i >= 0; i--) {
				sb.insert(0, '$');
				clsNum.get().decrementAndGet();
				if (!hasDst) clsNum.get().decrementAndGet();
				sb.insert(0, hasDst ? dst(clsKind) : src(kind));
			}

			outerClassHasDst.set(hasDst);
			innerClassNestLevel.set(nestLevel);
			clsNum.get().addAndGet(nestLevel);
			return sb.toString();
		}

		public String desc(MappedElementKind kind) {
			switch (kind) {
			case FIELD:
				return fldDescs.get((fldNum.get().get() - 1) % fldDescs.size());
			case METHOD:
				return mthDescs.get((mthNum.get().get() - 1) % mthDescs.size());
			default:
				throw new IllegalArgumentException("Invalid kind: " + kind);
			}
		}

		public AtomicInteger getCounter() {
			return counter.get();
		}

		private AtomicInteger getCounter(MappedElementKind kind) {
			switch (kind) {
			case CLASS:
				return clsNum.get();
			case FIELD:
				return fldNum.get();
			case METHOD:
				return mthNum.get();
			case METHOD_ARG:
				return argNum.get();
			case METHOD_VAR:
				return varNum.get();
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
		private ThreadLocal<MappedElementKind> lastKind = ThreadLocal.withInitial(() -> null);
		private ThreadLocal<Boolean> outerClassHasDst = ThreadLocal.withInitial(() -> false);
		private ThreadLocal<Integer> innerClassNestLevel = ThreadLocal.withInitial(() -> 0);
		private ThreadLocal<AtomicInteger> clsNum = ThreadLocal.withInitial(() -> new AtomicInteger());
		private ThreadLocal<AtomicInteger> fldNum = ThreadLocal.withInitial(() -> new AtomicInteger());
		private ThreadLocal<AtomicInteger> mthNum = ThreadLocal.withInitial(() -> new AtomicInteger());
		private ThreadLocal<AtomicInteger> argNum = ThreadLocal.withInitial(() -> new AtomicInteger());
		private ThreadLocal<AtomicInteger> varNum = ThreadLocal.withInitial(() -> new AtomicInteger());
		private ThreadLocal<AtomicInteger> nsNum = ThreadLocal.withInitial(() -> new AtomicInteger());
		private ThreadLocal<AtomicInteger> counter = ThreadLocal.withInitial(() -> new AtomicInteger());
	}

	public static class MappingDirs {
		@Nullable
		public static MemoryMappingTree getCorrespondingTree(Path dir) throws IOException {
			if (dir.equals(VALID)) return acceptTestMappings(new MemoryMappingTree());
			if (dir.equals(REPEATED_ELEMENTS)) return acceptTestMappingsWithRepeats(new MemoryMappingTree(), true, true);
			if (dir.equals(VALID_WITH_HOLES)) return acceptTestMappingsWithHoles(new MemoryMappingTree());
			return null;
		}

		public static final Path DETECTION = getResource("/detection/");
		public static final Path VALID = getResource("/read/valid/");
		public static final Path REPEATED_ELEMENTS = getResource("/read/repeated-elements/");
		public static final Path VALID_WITH_HOLES = getResource("/read/valid-with-holes/");
		public static final Path MERGING = getResource("/merging/");
	}

	private static final List<String> fldDescs = Arrays.asList("I", "Lcls;", "Lpkg/cls;", "[I");
	private static final List<String> mthDescs = Arrays.asList("()I", "(I)V", "(Lcls;)Lcls;", "(ILcls;)Lpkg/cls;", "(Lcls;[I)[[B");
	private static final String comment = "This is a comment";
	private static final NameGen nameGen = new NameGen();
	private static final MappedElementKind clsKind = MappedElementKind.CLASS;
	private static final MappedElementKind fldKind = MappedElementKind.FIELD;
	private static final MappedElementKind mthKind = MappedElementKind.METHOD;
	private static final MappedElementKind argKind = MappedElementKind.METHOD_ARG;
	private static final MappedElementKind varKind = MappedElementKind.METHOD_VAR;
}

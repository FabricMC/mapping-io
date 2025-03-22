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

package net.fabricmc.mappingio.test.tests.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static net.fabricmc.mappingio.test.TestUtil.createTree;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.test.visitors.VisitOrderVerifyingVisitor;
import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;
import net.fabricmc.mappingio.tree.MappingTree.MethodMapping;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public class DebugModeTest {
	private static final String nsA = "nsA";
	private static final String nsB = "nsB";
	private static final String nsC = "nsC";
	private static final String cls1NsAName = "cls1NsAName";
	private static final String cls1NsBName = "cls1NsBName";
	private static final String cls1NsCName = "cls1NsCName";
	private static final String cls2NsAName = "cls2NsAName";
	private static final String fld1NsAName = "fld1NsAName";
	private static final String fld1NsBName = "fld1NsBName";
	private static final String fldNsADesc = "L" + cls1NsAName + ";";
	private static final String fld2NsAName = "fld2NsAName";
	private static final String mth1NsAName = "mth1NsAName";
	private static final String mth1NsBName = "mth1NsBName";
	private static final String mth2NsAName = "mth2NsAName";
	private static final String mthNsADesc = "(L" + cls1NsAName + ";)V";
	private static final int classPhase = 0;
	private static final int fieldPhase = 1;
	private static final int methodPhase = 2;
	private MemoryMappingTree tree;
	private MappingVisitor delegate;

	@BeforeEach
	public void setup() {
		tree = createTree();
		delegate = new VisitOrderVerifyingVisitor(tree);
	}

	/**
	 * Tests {@link MemoryMappingTree#initClassesByDstNames()}.
	 */
	@Test
	public void duplicateClsDstNameWithinNsAlreadyPresent() throws Exception {
		for (int i = 0; i <= 2; i++) {
			tree.setInDebugMode(false);
			tree.setIndexByDstNames(false);

			delegate.visitHeader();
			delegate.visitNamespaces(nsA, Arrays.asList(nsB, nsC));
			delegate.visitContent();
			delegate.visitClass(cls1NsAName);
			if (i != 1) delegate.visitDstName(MappedElementKind.CLASS, 0, cls1NsBName);
			if (i != 0)	delegate.visitDstName(MappedElementKind.CLASS, 1, cls1NsCName);
			delegate.visitElementContent(MappedElementKind.CLASS);
			delegate.visitClass(cls2NsAName);
			if (i != 1) delegate.visitDstName(MappedElementKind.CLASS, 0, cls1NsBName);
			if (i != 0) delegate.visitDstName(MappedElementKind.CLASS, 1, cls1NsCName);
			delegate.visitElementContent(MappedElementKind.CLASS);
			delegate.visitEnd();

			tree.setInDebugMode(true);
			assertThrows(IllegalStateException.class, () -> tree.setIndexByDstNames(true));
			setup();
		}
	}

	/**
	 * Tests {@link MemoryMappingTree#visitDstName(MappedElementKind, int, String)}.
	 */
	@Test
	public void insertDuplicateDstNameIntoNsViaVisitation() throws Exception {
		int ns = 0;

		for (int phase = classPhase; phase <= methodPhase; phase++) {
			delegate.visitHeader();
			delegate.visitNamespaces(nsA, Arrays.asList(nsB, nsC));
			delegate.visitContent();
			delegate.visitClass(cls1NsAName);
			delegate.visitDstName(MappedElementKind.CLASS, ns, cls1NsBName);
			delegate.visitElementContent(MappedElementKind.CLASS);
			ClassMapping cls1 = tree.getClass(cls1NsAName);

			if (phase == classPhase) {
				delegate.visitClass(cls2NsAName);
				assertThrows(IllegalArgumentException.class, () -> delegate.visitDstName(MappedElementKind.CLASS, ns, cls1NsBName));
				assertEquals(cls1, tree.getClass(cls1NsBName, ns));
			} else if (phase == fieldPhase) {
				delegate.visitField(fld1NsAName, fldNsADesc);
				delegate.visitDstName(MappedElementKind.FIELD, ns, fld1NsBName);
				delegate.visitElementContent(MappedElementKind.FIELD);
				delegate.visitField(fld2NsAName, fldNsADesc);
				assertThrows(IllegalArgumentException.class, () -> delegate.visitDstName(MappedElementKind.FIELD, ns, fld1NsBName));
				assertEquals(cls1.getField(fld1NsAName, fldNsADesc), cls1.getField(fld1NsBName, tree.mapDesc(fldNsADesc, ns), ns));
			} else {
				delegate.visitMethod(mth1NsAName, mthNsADesc);
				delegate.visitDstName(MappedElementKind.METHOD, ns, mth1NsBName);
				delegate.visitElementContent(MappedElementKind.METHOD);
				MethodMapping mth1 = cls1.getMethod(mth1NsAName, mthNsADesc);

				if (phase == methodPhase) {
					delegate.visitMethod(mth2NsAName, mthNsADesc);

					assertThrows(IllegalArgumentException.class, () -> delegate.visitDstName(MappedElementKind.METHOD, ns, mth1NsBName));
					assertEquals(mth1, cls1.getMethod(mth1NsBName, tree.mapDesc(mthNsADesc, ns), ns));
				}

				// TODO: args and vars
			}

			delegate.reset();
		}
	}

	/**
	 * Tests {@link MemoryMappingTree#addClass(ClassMapping)},
	 * {@link ClassMapping#addField(FieldMapping)} and
	 * {@link ClassMapping#addMethod(MethodMapping)}.
	 */
	@Test
	public void insertDuplicateClsDstNameIntoNsViaTreeApi() throws Exception {
		MemoryMappingTree treeToCopyFrom = createTree();
		treeToCopyFrom.visitHeader();
		treeToCopyFrom.visitNamespaces(nsA, Arrays.asList(nsB, nsC));
		treeToCopyFrom.visitContent();
		treeToCopyFrom.visitClass(cls2NsAName);
		treeToCopyFrom.visitDstName(MappedElementKind.CLASS, 0, cls1NsBName);
		treeToCopyFrom.visitDstName(MappedElementKind.CLASS, 1, cls1NsCName);
		treeToCopyFrom.visitElementContent(MappedElementKind.CLASS);
		treeToCopyFrom.visitField(fld2NsAName, fldNsADesc);
		treeToCopyFrom.visitDstName(MappedElementKind.FIELD, 0, fld1NsBName);
		treeToCopyFrom.visitElementContent(MappedElementKind.FIELD);
		treeToCopyFrom.visitMethod(mth2NsAName, mthNsADesc);
		treeToCopyFrom.visitDstName(MappedElementKind.METHOD, 0, mth1NsBName);
		treeToCopyFrom.visitElementContent(MappedElementKind.METHOD);
		treeToCopyFrom.visitEnd();

		ClassMapping cls2ToAdd = treeToCopyFrom.getClass(cls2NsAName);
		FieldMapping fld2ToAdd = cls2ToAdd.getField(fld2NsAName, fldNsADesc);
		MethodMapping mth2ToAdd = cls2ToAdd.getMethod(mth2NsAName, mthNsADesc);

		for (int phase = classPhase; phase <= methodPhase; phase++) {
			delegate.visitHeader();
			delegate.visitNamespaces(nsA, Arrays.asList(nsB, nsC));
			delegate.visitContent();
			delegate.visitClass(cls1NsAName);
			delegate.visitDstName(MappedElementKind.CLASS, 0, cls1NsBName);
			delegate.visitDstName(MappedElementKind.CLASS, 1, cls1NsCName);
			delegate.visitElementContent(MappedElementKind.CLASS);

			if (phase == fieldPhase) {
				delegate.visitField(fld1NsAName, fldNsADesc);
				delegate.visitDstName(MappedElementKind.FIELD, 0, fld1NsBName);
				delegate.visitElementContent(MappedElementKind.FIELD);
			} else {
				delegate.visitMethod(mth1NsAName, mthNsADesc);
				delegate.visitDstName(MappedElementKind.METHOD, 0, mth1NsBName);
				delegate.visitElementContent(MappedElementKind.METHOD);
			}

			delegate.visitEnd();
			ClassMapping cls1 = tree.getClass(cls1NsAName);

			if (phase == classPhase) {
				assertThrows(IllegalArgumentException.class, () -> tree.addClass(cls2ToAdd));
			} else if (phase == fieldPhase) {
				assertThrows(IllegalArgumentException.class, () -> cls1.addField(fld2ToAdd));
			} else {
				if (phase == methodPhase) {
					assertThrows(IllegalArgumentException.class, () -> cls1.addMethod(mth2ToAdd));
				}

				// TODO: args and vars
			}
		}
	}
}

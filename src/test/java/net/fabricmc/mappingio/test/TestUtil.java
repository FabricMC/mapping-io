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

package net.fabricmc.mappingio.test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.neoforged.srgutils.IMappingFile;
import org.cadixdev.lorenz.io.MappingFormats;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;

public final class TestUtil {
	public static void setResourceRoot(Path path) {
		resourceRoot = path;
	}

	public static Path getResource(String slashPrefixedResourcePath) {
		if (resourceRoot != null) {
			return resourceRoot.resolve(slashPrefixedResourcePath.substring(1));
		}

		try {
			URL url = TestUtil.class.getResource(slashPrefixedResourcePath);

			if (url == null) {
				throw new IllegalArgumentException("Resource not found: " + slashPrefixedResourcePath);
			}

			return Paths.get(url.toURI());
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
		case PDME_FILE:
			return "pdme.pdme";
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
		case PDME_FILE:
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
		case PDME_FILE:
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

	private static Path resourceRoot;
}

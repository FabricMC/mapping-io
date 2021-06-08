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

package net.fabricmc.mappingio;

import java.util.Map;

public final class MappingUtil {
	public static String mapDesc(String desc, Map<String, String> clsMap) {
		return mapDesc(desc, 0, desc.length(), clsMap);
	}

	public static String mapDesc(String desc, int start, int end, Map<String, String> clsMap) {
		StringBuilder ret = null;
		int searchStart = start;
		int clsStart;

		while ((clsStart = desc.indexOf('L', searchStart)) >= 0) {
			int clsEnd = desc.indexOf(';', clsStart + 1);
			if (clsEnd < 0) throw new IllegalArgumentException();

			String cls = desc.substring(clsStart + 1, clsEnd);
			String mappedCls = clsMap.get(cls);

			if (mappedCls != null) {
				if (ret == null) ret = new StringBuilder(end - start);

				ret.append(desc, start, clsStart + 1);
				ret.append(mappedCls);
				start = clsEnd;
			}

			searchStart = clsEnd + 1;
		}

		if (ret == null) return desc.substring(start, end);

		ret.append(desc, start, end);

		return ret.toString();
	}

	static String[] toArray(String s) {
		return s != null ? new String[] { s } : null;
	}

	public static final String NS_SOURCE_FALLBACK = "source";
	public static final String NS_TARGET_FALLBACK = "target";
}

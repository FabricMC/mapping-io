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

package net.fabricmc.mappingio.format;

import net.fabricmc.mappingio.format.FeatureSet.FeaturePresence;
import net.fabricmc.mappingio.format.FeatureSet.LocalSupport;
import net.fabricmc.mappingio.format.FeatureSet.MemberSupport;

// Only exists since Java 8 doesn't support private interface methods
final class FeatureSetHelper {
	private FeatureSetHelper() {
	}

	static boolean isSupported(MemberSupport members) {
		return members.srcNames() != FeaturePresence.ABSENT
				|| members.dstNames() != FeaturePresence.ABSENT
				|| members.srcDescs() != FeaturePresence.ABSENT
				|| members.dstDescs() != FeaturePresence.ABSENT;
	}

	static boolean isSupported(LocalSupport locals) {
		return locals.positions() != FeaturePresence.ABSENT
				|| locals.lvIndices() != FeaturePresence.ABSENT
				|| locals.lvtRowIndices() != FeaturePresence.ABSENT
				|| locals.startOpIndices() != FeaturePresence.ABSENT
				|| locals.endOpIndices() != FeaturePresence.ABSENT
				|| locals.srcNames() != FeaturePresence.ABSENT
				|| locals.dstNames() != FeaturePresence.ABSENT
				|| locals.srcDescs() != FeaturePresence.ABSENT
				|| locals.dstDescs() != FeaturePresence.ABSENT;
	}
}

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

import net.fabricmc.mappingio.format.FeatureSet.LocalSupport;
import net.fabricmc.mappingio.format.FeatureSet.MemberSupport;
import net.fabricmc.mappingio.format.FeatureSet.SupportLevel;

// Only exists since Java 8 doesn't support private interface methods yet
final class FeatureSetHelper {
	static boolean isSupported(MemberSupport members) {
		return members.srcNames() != SupportLevel.UNSUPPORTED
				|| members.dstNames() != SupportLevel.UNSUPPORTED
				|| members.srcDescs() != SupportLevel.UNSUPPORTED
				|| members.dstDescs() != SupportLevel.UNSUPPORTED;
	}

	static boolean isSupported(LocalSupport locals) {
		return locals.positions() != SupportLevel.UNSUPPORTED
				|| locals.lvIndices() != SupportLevel.UNSUPPORTED
				|| locals.lvtRowIndices() != SupportLevel.UNSUPPORTED
				|| locals.startOpIndices() != SupportLevel.UNSUPPORTED
				|| locals.endOpIndices() != SupportLevel.UNSUPPORTED
				|| locals.srcNames() != SupportLevel.UNSUPPORTED
				|| locals.dstNames() != SupportLevel.UNSUPPORTED
				|| locals.srcDescs() != SupportLevel.UNSUPPORTED
				|| locals.dstDescs() != SupportLevel.UNSUPPORTED;
	}
}

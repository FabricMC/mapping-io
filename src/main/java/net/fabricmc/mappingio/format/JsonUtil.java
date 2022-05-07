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

package net.fabricmc.mappingio.format;

import java.io.IOException;
import java.io.Writer;

final class JsonUtil {
	public static boolean needEscape(String s) {
		for (int pos = 0, len = s.length(); pos < len; pos++) {
			char c = s.charAt(pos);
			if (toEscape.indexOf(c) >= 0) return true;
		}

		return false;
	}

	public static void writeEscaped(String s, Writer out) throws IOException {
		final int len = s.length();
		int start = 0;

		for (int pos = 0; pos < len; pos++) {
			char c = s.charAt(pos);
			int idx = toEscape.indexOf(c);

			if (idx >= 0) {
				out.write(s, start, pos - start);
				out.write('\\');
				out.write(escaped.charAt(idx));
				start = pos + 1;
			}
		}

		out.write(s, start, len - start);
	}

	public static String unescape(String str) {
		int pos = str.indexOf('\\');
		if (pos < 0) return str;

		StringBuilder ret = new StringBuilder(str.length() - 1);
		int start = 0;

		do {
			ret.append(str, start, pos);
			pos++;
			int type;

			if (pos >= str.length()) {
				throw new RuntimeException("incomplete escape sequence at the end");
			} else if ((type = escaped.indexOf(str.charAt(pos))) < 0) {
				throw new RuntimeException("invalid escape character: \\"+str.charAt(pos));
			} else {
				ret.append(toEscape.charAt(type));
			}

			start = pos + 1;
		} while ((pos = str.indexOf('\\', start)) >= 0);

		ret.append(str, start, str.length());

		return ret.toString();
	}

	private static final String toEscape = "\"\\\b\f\n\r\t";
	private static final String escaped = "\"\\bfnrt";
}

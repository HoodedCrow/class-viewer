package classviewer.changes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This parser reads JSON stream are return a list/map/whatever internal
 * equivalent.
 * 
 * @author TK
 */
public class JsonParser {
	/** Parse JSON from the reader (file or URL) into a list */
	public static ArrayList<Object> parse(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		StringBuffer buf = new StringBuffer();
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			buf.append(line.replace("\\\"", "'"));
		}

		int[] index = new int[1];
		index[0] = 0;
		@SuppressWarnings("unchecked")
		ArrayList<Object> mainList = (ArrayList<Object>) parseObject(buf, index);
		return mainList;
	}

	/**
	 * Parse whatever JSON element from the buffer. Idx is the start index and
	 * also the new index after this element is parsed. The array has size 1,
	 * using array to return 2nd value from this function
	 */
	public static Object parseObject(StringBuffer buf, int[] idx) {
		if (buf.length() == 0)
			return null;
		if (buf.charAt(idx[0]) == '[') {
			return parseList(buf, idx);
		} else if (buf.charAt(idx[0]) == '{') {
			return parseMap(buf, idx);
		} else if (buf.charAt(idx[0]) == '\"') {
			// Parse string
			int next = buf.indexOf("\"", idx[0] + 1);
			if (next < 0)
				next = buf.length() - 1;
			String str = buf.substring(idx[0] + 1, next);
			idx[0] = next + 1;

			// A couple of special cases
			str = str.replace("\\u00a0", "<br/>");
			str = str.replace("\\u2019", "'");

			// Treat the rest as normal unicode
			int v = str.indexOf("\\u");
			while (v >= 0 && v + 6 <= str.length()) {
				char c = (char) Integer.parseInt(str.substring(v + 2, v + 6),
						16);
				str = str.substring(0, v) + c + str.substring(v + 6);
				v = str.indexOf("\\u");
			}
			return str;
		} else if (buf.length() >= idx[0] + 4
				&& "null".equals(buf.substring(idx[0], idx[0] + 4))) {
			idx[0] += 4;
			return null;
		} else if (buf.length() >= idx[0] + 4
				&& "true".equals(buf.substring(idx[0], idx[0] + 4))) {
			idx[0] += 4;
			return Boolean.TRUE;
		} else if (buf.length() >= idx[0] + 5
				&& "false".equals(buf.substring(idx[0], idx[0] + 5))) {
			idx[0] += 5;
			return Boolean.FALSE;
		} else {
			// Ints or doubles doubles
			int next = idx[0];
			boolean isDouble = false;
			if (buf.charAt(next) == '-')
				next++;
			while (next < buf.length() && Character.isDigit(buf.charAt(next)))
				next++;
			if (next < buf.length() && buf.charAt(next) == '.') {
				next++;
				isDouble = true;
				while (next < buf.length()
						&& Character.isDigit(buf.charAt(next)))
					next++;
			}
			try {
				Object val;
				if (isDouble)
					val = new Double(buf.substring(idx[0], next));
				else
					val = new Integer(buf.substring(idx[0], next));
				idx[0] = next;
				return val;
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return new Integer(-1);
			}
		}
	}

	private static void skipSpace(StringBuffer b, int[] idx) {
		while (idx[0] < b.length() && Character.isWhitespace(b.charAt(idx[0])))
			idx[0]++;
	}

	public static ArrayList<Object> parseList(StringBuffer buf, int[] idx) {
		idx[0]++;
		ArrayList<Object> into = new ArrayList<Object>();
		while (true) {
			skipSpace(buf, idx);
			if (idx[0] >= buf.length()) {
				System.err.println("Unexpected end");
				return into;
			}
			if (buf.charAt(idx[0]) == ']') {
				idx[0]++;
				return into; // Done
			}
			if (buf.charAt(idx[0]) == ',') {
				idx[0]++;
				continue; // skip commas, don't check for errors
			}
			into.add(parseObject(buf, idx));
		}
	}

	public static HashMap<String, Object> parseMap(StringBuffer buf, int[] idx) {
		idx[0]++;
		HashMap<String, Object> into = new HashMap<String, Object>();
		while (true) {
			skipSpace(buf, idx);
			if (idx[0] >= buf.length()) {
				System.err.println("Unexpected end");
				return into;
			}
			if (buf.charAt(idx[0]) == '}') {
				idx[0]++;
				return into; // Done
			}
			if (buf.charAt(idx[0]) == ',') {
				idx[0]++;
				continue; // skip commas, don't check for errors
			}

			// Should have a string key
			String key = (String) parseObject(buf, idx);
			// Should have colon
			skipSpace(buf, idx);
			assert (buf.charAt(idx[0]) == ':');
			idx[0]++;
			skipSpace(buf, idx);
			into.put(key, parseObject(buf, idx));
		}
	}
}

package com.sgcharts.bcrecommender;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;

public final class StringUtil {
	private static final Logger log = LoggerFactory.getLogger(StringUtil.class);
	public static final CharMatcher ASCII_DIGITS = CharMatcher.inRange('0', '9');
	public static final CharMatcher LATIN_LETTERS_LOWER_CASE = CharMatcher.inRange(
			'a', 'z');
	public static final CharMatcher LATIN_LETTERS_UPPER_CASE = CharMatcher.inRange(
			'A', 'Z');
	public static final CharMatcher LATIN_LETTERS = LATIN_LETTERS_LOWER_CASE.or(LATIN_LETTERS_UPPER_CASE);
	public static final CharMatcher HYPHEN = CharMatcher.is('-');
	public static final CharMatcher UNDERSCORE = CharMatcher.is('_');
	public static final CharMatcher COMMA = CharMatcher.is(',');
	public static final CharMatcher SEMI_COLON = CharMatcher.is(';');
	public static final CharMatcher TAB = CharMatcher.is('\t');
	public static final CharMatcher HASHTAG = CharMatcher.is('#');
	public static final CharMatcher NEW_LINE = CharMatcher.is('\n');
	public static final CharMatcher RETURN = CharMatcher.is('\r');
	public static final CharMatcher LINE_BREAKS = NEW_LINE.or(RETURN); 
	private static final CharMatcher SLUG_WHITELIST = ASCII_DIGITS.or(
			LATIN_LETTERS_LOWER_CASE)
		.or(HYPHEN);
	private static final Set<String> TRUTHY_VALUES = Sets.newHashSet("y",
			"yes", "1", "true");
	private static final String DOTS = "...";
	private static final Escaper HTML_ESCAPER = HtmlEscapers.htmlEscaper();

	private StringUtil() {
		// Private constructor; not meant to be instantiated
	}
	
	public static List<String> toParagraphs(String s) {
		if (s == null) {
			return new ArrayList<>();
		}
		return split(s, LINE_BREAKS);
	}
	
	public static byte[] getBytesInUtf8(String s) {
		if (s == null) {
			log.error("string must not be null");
			throw new IllegalArgumentException();
		}
		return s.getBytes(StandardCharsets.UTF_8);
	}
	
	public static int countBytesInUtf8(String s) {
		return getBytesInUtf8(s).length;
	}

	/**
	 * Trim whitespace according to latest Unicode standard (different from
	 * JDK's spec).
	 * 
	 * @param s
	 * @return
	 */
	public static String trim(CharSequence sequence) {
		if (sequence == null) {
			return "";
		}
		return CharMatcher.whitespace().trimFrom(sequence);
	}

	public static String lowerTrim(CharSequence sequence) {
		return trim(sequence).toLowerCase();
	}

	public static boolean isNullEmptyOrBlank(CharSequence sequence) {
		String s = trim(sequence);
		return s.isEmpty();
	}

	public static String concat(Object... objs) {
		StringBuilder sb = new StringBuilder();
		for (Object obj : objs) {
			sb.append(obj);
		}
		return sb.toString();
	}

	public static String join(Iterable<?> parts, String separator) {
		Joiner j = Joiner.on(separator)
			.skipNulls();
		return j.join(parts);
	}

	/**
	 * Slug should contain only ASCII digits, latin letters in lowercase and
	 * hypen ('-').
	 * 
	 * @param s
	 * @return
	 */
	public static String slug(String s) {
		String slug = lowerTrim(s);
		if (slug.isEmpty()) {
			return slug;
		}
		// Replace existing hyphen with whitespace,
		// so that "like - this" is later transformed to "like-this".
		// If hyphen is not adjacent to whitespace e.g. "anti-hero"
		// the hyphen will be restored later.
		slug = HYPHEN.replaceFrom(slug, ' ');
		// Collapse whitespaces down to a single space
		slug = CharMatcher.whitespace().collapseFrom(slug, ' ');
		// Replace whitespace and underscore with hyphen
		slug = CharMatcher.whitespace().or(UNDERSCORE)
			.replaceFrom(slug, '-');
		slug = SLUG_WHITELIST.retainFrom(slug);
		return slug;
	}

	public static List<String> split(CharSequence sequence,
			CharMatcher separator, boolean omitEmptyStrings) {
		Splitter s = Splitter.on(separator)
			.trimResults();
		if (omitEmptyStrings) {
			s = s.omitEmptyStrings();
		}
		return s.splitToList(sequence);
	}

	public static List<String> split(CharSequence sequence,
			CharMatcher separator) {
		boolean omitEmptyStrings = true;
		return split(sequence, separator, omitEmptyStrings);
	}

	public static List<String> split(CharSequence sequence, String separator,
			boolean omitEmptyStrings) {
		Splitter s = Splitter.on(separator)
			.trimResults();
		if (omitEmptyStrings) {
			s = s.omitEmptyStrings();
		}
		return s.splitToList(sequence);
	}

	public static List<String> split(CharSequence sequence, String separator) {
		boolean omitEmptyStrings = true;
		return split(sequence, separator, omitEmptyStrings);
	}

	public static boolean truthy(String s) {
		String val = s.toLowerCase();
		if (TRUTHY_VALUES.contains(val)) {
			return true;
		}
		return false;
	}

	public static String truncate(String s, int length, boolean appendDots, boolean wordSafe) {
		if (s == null) {
			log.error("string must not be null");
			throw new IllegalArgumentException();
		}
		if (s.length() <= length) {
			return s;
		}
		if (appendDots) {
			length -= DOTS.length();
		}
		if (wordSafe) {
			// Search for the last index of the whitespace character 
			// starting from index len (instead of len-1)
			// because the last word may fit nicely into the truncated string
			int lastSpaceIndex = s.lastIndexOf(' ', length);
			// Index of last whitespace falls after 0
			if (lastSpaceIndex > 0) {
				s = s.substring(0, lastSpaceIndex);
			} else {
				s = s.substring(0, length);
			}
		} else {
			s = s.substring(0, length);
		}
		if (appendDots) {
			s = concat(s, DOTS);
		}
		return s;
	}
	
	public static String truncate(String s, int length) {
		boolean wordSafe = false;
		boolean appendDots = false;
		return truncate(s, length, appendDots, wordSafe);
	}
	
	public static String truncate(String s, int length, boolean appendDots) {
		boolean wordSafe = false;
		return truncate(s, length, appendDots, wordSafe);
	}
	
	public static String truncateWordSafe(String s, int length, boolean appendDots) {
		boolean wordSafe = true;
		return truncate(s, length, appendDots, wordSafe);
	}
	
	public static String truncateWordSafe(String s, int length) {
		boolean wordSafe = true;
		boolean appendDots = false;
		return truncate(s, length, appendDots, wordSafe);
	}
	
	/**
	 * Guava throws NPE if input string is null!
	 * 
	 * @param s
	 * @return
	 */
	public static String escapeHtml(String s) {
		return HTML_ESCAPER.escape(s);
	}
	
	public static void escapeHtml(List<String> list) {
		int size = list.size();
		String s;
		for (int i = 0; i < size; i++) {
			s = list.get(i);
			list.set(i, escapeHtml(s));
		}
	}
	
	public static void escapeHtml(Set<String> set) {
		for (String s : set) {
			set.remove(s);
			set.add(escapeHtml(s));
		}
	}
	
	public static void escapeHtml(Map<String, Object> map) {
		if (map == null) {
			return;
		}
		Object v;
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			v = entry.getValue();
			if (!(v instanceof String)) {
				continue;
			}
			entry.setValue(escapeHtml((String) v));
		}
	}
	
	public static String trimAndEscapeHtml(String s) {
		return escapeHtml(trim(s));
	}
}

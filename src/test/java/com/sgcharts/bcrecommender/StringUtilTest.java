package com.sgcharts.bcrecommender;

import static com.sgcharts.bcrecommender.StringUtil.*;
import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class StringUtilTest {

	@Test(dataProvider = "isNullEmptyOrBlankTestData")
	public void isNullEmptyOrBlankTest(String input, boolean expected) {
		boolean actual = isNullEmptyOrBlank(input);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] isNullEmptyOrBlankTestData() {
		return new Object[][] { { " a b c ", false }, { null, true },
				{ "", true }, { "     ", true } };
	}

	@Test(dataProvider = "trimTestData")
	public void trimTest(String input, String expected) {
		String actual = trim(input);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] trimTestData() {
		return new Object[][] { { "a b c", "a b c" }, { "  a b c  ", "a b c" } };
	}

	@Test(dataProvider = "lowerTrimTestData")
	public void lowerTrimTest(String input, String expected) {
		String actual = lowerTrim(input);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] lowerTrimTestData() {
		return new Object[][] { { "a b c", "a b c" }, { "  a B C  ", "a b c" } };
	}

	@Test
	public void concatTest() {
		String actual = concat("a", " b ", "c");
		assertEquals(actual, "a b c");
	}

	@Test(dataProvider = "splitTestData")
	public void splitTest(String input, CharMatcher separator,
			boolean omitEmptyStrings, List<String> expected) {
		List<String> actual = split(input, separator, omitEmptyStrings);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] splitTestData() {
		boolean omitEmptyStrings = true;
		CharMatcher whitespace = CharMatcher.is(' ');
		CharMatcher comma = CharMatcher.is(',');
		return new Object[][] {
				{
						"the quick brown fox jumps over the lazy dog",
						whitespace,
						omitEmptyStrings,
						Lists.newArrayList("the", "quick", "brown", "fox",
								"jumps", "over", "the", "lazy", "dog") },
				{
						"the quick brown fox jumps over the lazy dog",
						comma,
						omitEmptyStrings,
						Lists.newArrayList("the quick brown fox jumps over the lazy dog") },
				{ "the", comma, omitEmptyStrings, Lists.newArrayList("the") },
				{
						"the,,quick, brown, fox, jumps, over, the, lazy, dog",
						comma,
						omitEmptyStrings,
						Lists.newArrayList("the", "quick", "brown", "fox",
								"jumps", "over", "the", "lazy", "dog") },
				{
						" , the,,quick, brown, fox, jumps, over, the, lazy, dog",
						comma,
						!omitEmptyStrings,
						Lists.newArrayList("", "the", "", "quick", "brown",
								"fox", "jumps", "over", "the", "lazy", "dog") }

		};
	}

	@Test(dataProvider = "joinTestData")
	public void joinTest(List<String> input, String separator, String expected) {
		String actual = join(input, separator);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] joinTestData() {
		String whitespace = " ";
		String comma = ",";
		return new Object[][] {
				{ Lists.newArrayList("a", "b", "c", "d"), whitespace, "a b c d" },
				{ Lists.newArrayList("a", "b", "c", "d"), comma, "a,b,c,d" },
				{ Lists.newArrayList(null, "b", "c", "d"), whitespace, "b c d" } };
	}

	@Test(dataProvider = "truthyTestData")
	public void truthyTest(String input, boolean expected) {
		boolean actual = truthy(input);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] truthyTestData() {
		return new Object[][] { { "y", true }, { "yes", true }, { "1", true },
				{ "n", false }, { "no", false }, { "0", false } };
	}

	@Test(dataProvider = "slugTestData")
	public void slugTest(String input, String expected) {
		String actual = slug(input);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] slugTestData() {
		return new Object[][] {
				{ "B-LOT SINGAPORE PTE. LTD.", "b-lot-singapore-pte-ltd" },
				{ "KEPPEL LAND INTERNATIONAL LIMITED",
						"keppel-land-international-limited" },
				{ "anti-hero", "anti-hero" }, { "like - this", "like-this" },
				{ "abc-123 ?><:\")(~!@#$%^&*+ -", "abc-123--" } };
	}

	@Test(dataProvider = "truncateTestData")
	public void truncateTest(String s, int length, boolean wordSafe,
			boolean appendDots, String expected) {
		String actual = truncate(s, length, appendDots, wordSafe);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] truncateTestData() {
		return new Object[][] {
				{ "the quick brown fox", 100, false, false,
						"the quick brown fox" },
				{ "the quick brown fox", 100, true, false,
						"the quick brown fox" },
				{ "the quick brown fox", 100, false, true,
						"the quick brown fox" },
				{ "the quick brown fox", 13, false, false, "the quick bro" },
				{ "the quick brown fox", 13, true, false, "the quick" },
				{ "the quick brown fox", 13, false, true, "the quick ..." },
				{ "the quick brown fox", 13, true, true, "the quick..." },
				{ " thequickbrownfox", 5, true, false, " theq" },
				{ " thequickbrownfox", 5, true, true, " t..." } };
	}

	@Test(dataProvider = "escapeHtmlTestData")
	public void escapeHtmlTest(Map<String, Object> map,
			Map<String, String> expected) {
		escapeHtml(map);
		assertEquals(map, expected);
	}

	@DataProvider
	public Object[][] escapeHtmlTestData() {
		Map<String, String> input = Maps.newHashMap();
		input.put("1", "\"");
		input.put("2", "\'");
		input.put("3", "&");
		input.put("4", ">");
		input.put("5", "<");
		Map<String, String> expected = Maps.newHashMap();
		expected.put("1", "&quot;");
		expected.put("2", "&#39;");
		expected.put("3", "&amp;");
		expected.put("4", "&gt;");
		expected.put("5", "&lt;");
		return new Object[][] { { input, expected } };
	}
}
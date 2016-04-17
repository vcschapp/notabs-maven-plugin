package org.victorschappert.notabs;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import static org.junit.runners.Parameterized.Parameters;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;

/**
 * Unit tests for {@link GlobParser}.
 * 
 * @author Victor Schappert
 * @since 20160410
 */
@RunWith(Parameterized.class)
public class GlobParserTest {

    @Parameter
    public String glob;

    @Parameter(value = 1)
    public String expectedPattern;

    @Parameter(value = 2)
    public String[] matches;

    @Parameter(value = 3)
    public String[] nonMatches;

    @Test
    public void test() {
        final String actualPattern = parse(glob);
        assertEquals(expectedPattern, actualPattern);
        final Pattern pattern = compile(expectedPattern);
        final Matcher matcher = pattern.matcher("");
        IntStream.range(0, matches.length).forEach(
                i -> check("matches", matcher, i, matches[i],
                        Assert::assertTrue));
        IntStream.range(0, nonMatches.length).forEach(
                j -> check("does not match", matcher, j, nonMatches[j],
                        Assert::assertFalse));
    }

    @Parameters(name = "\"{0}\" => `{1}`  (@Parameters index {index}")
    public static Collection<Object[]> params() {
        return asList(new Object[][] {
                // @formatter:off
                // Basic glob special characters
                param("", "", array(""), array("a", "/", "/x")),
                param("?", "[^/]", array("a", ".", "x", "A", ";", "?"),
                        array("", "aa", "AA", "/", "/a", "a/")),
                param("*", "[^/]*", array("a", "abc", "hello.txt"),
                        array("/", "/a", "a/a")),
                // Star
                param("/*/", "[^/]*", array("a", "A", "0xabc", "-"),
                        array("/a", "/a/", "a/")),
                param("a*b.*c", "a[^/]*b\\.[^/]*c",
                        array("ab.c", "a-b.cc, a????b.\\c")),
                param("dir/*/README*", "dir/[^/]*/README[^/]*",
                        array("dir//README", "dir/a/README", "dir/a/README.txt"),
                        array("dir/a//README", "dir/a/b/README", "dir/a/b/README/")),
                param("*/*/dir/h*d/*xyz/*/file", "[^/]*/[^/]*/dir/h[^/]*d/[^/]*xyz/[^/]*/file",
                        array("a/b/dir/hd/xyz/c/file", "//dir/hd/xyz/x/file",
                              "aa/bb/dir/hello, world/vvvxyz//file")),
                // "Nova" - '**'
                param("**", ".*"),
                param("/**", ".*"),
                param("**/", ".*"),
                param("/**/", ".*"),
                param("**/**", ".*"),
                param("/**/**", ".*"),
                param("**/**/", ".*"),
                param("/**/**/", ".*"),
                param("a/**/b", "a/.*/?b",
                        array("a/b", "a//b", "a/b/c/b", "a/path/to/b")),
                // Slashes
                param("/", "", array(""), array("/")),
                param("/a", "a", array("a"), array("/a")),
                param("//", ""),
                param("//a", "a", array("a")),
                param("///a///a///", "a/a", array("a/a")),
                param("\\\\", "\\\\", array("\\")),
                // Escaping regex special characters
                param("(", "\\(", array("(")),
                param(")", "\\)", array(")")),
                param("[", "\\[", array("[")),
                param("]", "\\]", array("]")),
                param("{", "\\{", array("{")),
                param("}", "\\}", array("}")),
                param(".", "\\.", array(".")),
                param("-", "\\-", array("-")),
                param("+", "\\+", array("+")),
                param("|", "\\|", array("|")),
                param("^", "\\^", array("^")),
                param("$", "\\$", array("$")),
                param("=", "\\=", array("=")),
                param("!", "\\!", array("!")),
                param("<", "\\<", array("<")),
                // Error cases
// TODO: error cases here
                // General
                param("/a/**/b*/c?/*Test*.java", "a/.*/?b[^/]*/c[^/]/[^/]*Test[^/]*\\.java",
                        array("a/bb/cc/Test.java", "a/b/b/cd/MyIntegrationTestCase.java"))
                // @formatter:on
        });
    }

    //
    // INTERNALS
    //

    private static final String[] NONE = new String[0];

    private static Object[] param(final String glob,
            final String expectedPattern) {
        return param(glob, expectedPattern, NONE);
    }

    private static Object[] param(final String glob,
            final String expectedPattern, final String[] matches) {
        return param(glob, expectedPattern, matches, NONE);
    }

    private static Object[] param(final String glob,
            final String expectedPattern, final String[] matches,
            final String[] nonMatches) {
        return new Object[] { glob, expectedPattern, matches, nonMatches };
    }

    private static String[] array(final String... array) {
        return array;
    }

    private static String parse(final String glob) {
        return GlobParser.parse(glob).pattern();
    }

    private void check(final String assertion, final Matcher matcher,
            final int index, final String candidate,
            final BiConsumer<String, Boolean> assertMethod) {
        final String message = "Failed assertion that '" + glob + "' "
                + assertion + " '" + candidate + "' (candidate " + index + ")";
        matcher.reset(candidate);
        assertMethod.accept(message, matcher.matches());
    }
}

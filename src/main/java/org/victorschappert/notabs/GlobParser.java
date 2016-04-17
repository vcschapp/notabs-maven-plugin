package org.victorschappert.notabs;

import static java.util.regex.Pattern.compile;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <p>
 * Converts an Ant-style glob string to a compiled Java regular expression.
 * </p>
 *
 * @author Victor Schappert
 * @since 20160304
 */
final class GlobParser {

    //
    // DATA
    //

    private final String glob;
    private final int n;
    private int i;
    private final StringBuilder pattern;
    private boolean anySeps; // At least one separator written to pattern
    private boolean hasSep; // Current character is a separator
    private boolean hadSep; // Last character was separator or start/end of glob
    private boolean hasNova; // Current character is second star of '**'
    private boolean hadNova; // Last character was second star of "**"

    //
    // CONSTRUCTORS
    //

    private GlobParser(final String glob) {
        this.glob = Objects.requireNonNull(glob);
        this.n = glob.length();
        this.i = 0;
        this.pattern = new StringBuilder(8 + n);
        this.anySeps = false;
        this.hadSep = true;
        this.hasSep = false;
        this.hasNova = this.hadNova = false;
    }

    //
    // STATICS
    //

    static Pattern parse(final String glob) {
        return new GlobParser(glob).parse();
    }

    //
    // INTERNALS
    //

    private Pattern parse() {
        int c = next();
        while ('/' == c) {
            c = next();
        }
        loop: while (true) {
            switch (c) {
            case -1:
                break loop;
            case '*':
                c = star();
                break;
            case '?':
                c = query();
                break;
            // @formatter:off
            case '(': case ')': case '[': case ']': case '{': case '}':
            case '.': case '-': case '+': case '|': case '^': case '$':
            case '=': case '!': case '<':
            // @formatter:on
                c = escape(c);
                break;
            case '\\':
                c = escaping();
                break;
            case '/':
                c = sep();
                break;
            default:
                c = ordinary(c);
                break;
            } // switch (c)
            hadSep = hasSep;
            hasSep = false;
            hadNova = hasNova;
            hasNova = false;
        } // while (0 <= c)
        return compile(trim(pattern).toString());
    }

    private int next() {
        if (i < n) {
            return glob.charAt(i++);
        } else {
            return -1;
        }
    }

    private static final String ERR_TWOSTARS_NOT_ADJACENT_TO_SEP = "'**' must be adjacent to '/' or the start or end of the glob";

    private static final String ANYCHAR = "[^/]";

    private int star() {
        int c = next();
        if ('*' != c) {
            // Convert a single '*' into any number of non-path separator
            // characters.
            pattern.append(ANYCHAR + '*');
            return c;
        } else if (hadSep) {
            // Convert a nova '**' that appears either at the start of the glob
            // or after a path separator into any number of characters
            // including path separator characters.
            if (!hadNova) {
                pattern.append(".*");
                hasNova = true;
            }
            c = next();
            if (c < 0) {
                return c;
            } else if ('/' == c) {
                hasSep = true;
                if (anySeps) {
                    pattern.append("/?");
                }
                return next();
                // } else if ('/' == c && hadNova) {
                // hasSep = true;
                // return next();
                // } else if ('/' == c && !hadNova) {
                // return c;
            } else {
                throw error(ERR_TWOSTARS_NOT_ADJACENT_TO_SEP);
            }
        } else {
            throw error(ERR_TWOSTARS_NOT_ADJACENT_TO_SEP);
        }
    }

    private int query() {
        pattern.append(ANYCHAR);
        return next();
    }

    private int escape(final int c) {
        pattern.append('\\').append((char) c);
        return next();
    }

    private int escaping() {
        final int c = next();
        if (c < 0) {
            throw error("trailing escape character");
        }
        return escape(c);
    }

    private int sep() {
        assert !hadSep;
        pattern.append('/');
        anySeps = true;
        hasSep = true;
        int c = next();
        while ('/' == c) {
            c = next();
        }
        return c;
    }

    private int ordinary(final int c) {
        pattern.append((char) c);
        return next();
    }

    private static StringBuilder trim(final StringBuilder pattern) {
        final int n = pattern.length();
        int i = n;
        while (0 < i && '/' == pattern.charAt(i - 1)) {
            --i;
        }
        return pattern.delete(i, n);
    }

    private GlobParseException error(final String message) {
        return new GlobParseException("invalid glob: '" + glob + "': "
                + message);
    }
}

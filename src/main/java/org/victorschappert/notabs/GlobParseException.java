package org.victorschappert.notabs;

/**
 * <p>
 * Exception thrown when a glob parse fails.
 * </p>
 *
 * @author Victor Schappert
 * @since 20160304
 */
@SuppressWarnings("serial")
final class GlobParseException extends RuntimeException {

    GlobParseException(final String message) {
        super(message);
    }
}

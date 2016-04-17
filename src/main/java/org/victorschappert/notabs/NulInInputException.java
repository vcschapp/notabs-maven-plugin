package org.victorschappert.notabs;

import java.io.IOException;

/**
 * <p>
 * Exception thrown when a NUL character is found in the input stream. This
 * event is used to classify a file as binary.
 * </p>
 *
 * @author Victor Schappert
 * @since 20160305
 */
@SuppressWarnings("serial")
final class NulInInputException extends IOException {

    //
    // CONSTRUCTORS
    //

    NulInInputException(final long byteIndex) {
        super("NUL character found in input at byte position " + byteIndex);
    }
}

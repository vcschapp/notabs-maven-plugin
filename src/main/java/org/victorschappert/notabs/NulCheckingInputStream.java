package org.victorschappert.notabs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static java.lang.Math.min;

/**
 * <p>
 * Buffering wrapper around an input stream which throws
 * {@link NulInInputException} when it encounters a NUL byte (or byte sequence)
 * in the underlying input stream.
 * </p>
 * 
 * @author Victor Schappert
 * @since 20160305
 */
final class NulCheckingInputStream extends InputStream {

    //
    // DATA
    //

    private final InputStream underlying;
    private final int nulSize;
    private final byte[] buffer;
    private int pos; // -1 indicates closed stream
    private int count;
    private int nulCount;
    private long bytesRead;

    //
    // CONSTRUCTORS
    //

    NulCheckingInputStream(final InputStream underlying, final int nulSize,
            final int bufferSize) {
        this.underlying = Objects.requireNonNull(underlying);
        this.nulSize = requirePositive(nulSize, "nulSize");
        this.buffer = new byte[bufferSize];
        this.pos = 0;
        this.count = 0;
        this.nulCount = 0;
        this.bytesRead = 0L;
    }

    //
    // ANCESTOR CLASS: InputStream
    //

    @Override
    public int read() throws IOException {
        assertOpen();
        if (count <= pos) {
            if (fill() < 1) {
                return -1;
            }
        }
        return buffer[pos++] & 0xff;
    }

    @Override
    public int read(final byte[] b, final int off, final int len)
            throws IOException {
        assertOpen();
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = read1(b, off, len);
        if (0 <= n) {
            while (n < len && 0 < underlying.available()) {
                n += read1(b, off + n, len - n);
            } // while
        }
        return n;
    }

    @Override
    public void close() throws IOException {
        assertOpen();
        pos = -1;
        underlying.close();
    }

    //
    // INTERNALS
    //

    private void assertOpen() throws IOException {
        if (pos < 0) {
            throw new IOException("Stream closed");
        }
    }

    private int read1(final byte[] b, final int off, final int len)
            throws IOException {
        int avail = count - pos;
        if (avail + buffer.length <= len) {
            System.arraycopy(buffer, pos, b, off, avail);
            pos = count = 0;
            final int n = underlying.read(b, off + avail, len - avail);
            if (0 < n) {
                checkNulls(b, off + avail, n);
                return avail + n;
            } else if (0 < avail) {
                return avail;
            } else {
                return n;
            }
        } else if (avail <= len) {
            System.arraycopy(buffer, pos, b, off, avail);
            if (0 < fill()) {
                pos = min(len - avail, count);
                System.arraycopy(buffer, 0, b, off, pos);
                return avail + pos;
            } else if (0 < avail) {
                return avail;
            } else {
                return -1;
            }
        } else /* len < avail */{
            System.arraycopy(buffer, pos, b, off, len);
            pos += len;
            return len;
        }
    }

    private int fill() throws IOException {
        pos = 0;
        count = underlying.read(buffer, 0, buffer.length);
        if (1 < count) {
            checkNulls(buffer, 0, count);
            return count;
        } else {
            return -1;
        }
    }

    private void checkNulls(final byte[] buffer, final int off, final int len)
            throws NulInInputException {
        for (int i = 0; i < len; ++i) {
            if (0 == buffer[off + i] && nulSize == ++nulCount) {
                throw new NulInInputException(bytesRead + i);
            } else {
                nulCount = 0;
            }
        }
        bytesRead += len - off;
    }

    private static int requirePositive(final int value, final String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name
                    + " must be positive, but is " + value);
        }
        return value;
    }
}

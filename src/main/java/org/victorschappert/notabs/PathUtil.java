package org.victorschappert.notabs;

import static java.io.File.separatorChar;
import static java.util.Arrays.stream;

import java.io.File;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import com.google.common.base.Predicate;

/**
 * <p>
 * Utilities relating to filesystem paths.
 * </p>
 *
 * @author Victor Schappert
 * @since 20160410
 */
class PathUtil {

    static String normalize(final Path basepath, final File subpath) {
        final Path relpath = basepath.relativize(subpath.toPath());
        final String str = relpath.toString();
        if ('/' == separatorChar) {
            return str;
        } else {
            return str.replace(separatorChar, '/');
        }
    }

    static void traverse(final Path basepath, final File dir,
            final Predicate<String> filter,
            final BiConsumer<File, String> consumer) {
        final File[] listing = dir.listFiles(file -> filter.apply(normalize(
                basepath, dir)));
        if (null != listing) {
            stream(listing).filter(file -> {
                if (!file.isDirectory()) {
                    consumer.accept(file, normalize(basepath, file));
                    return false;
                } else {
                    return true;
                }
            }).forEach(subdir -> traverse(basepath, subdir, filter, consumer));
        }
    }
}

package org.victorschappert.notabs;

/* The MIT License (MIT)
 *
 * Copyright (c) 2016 Victor Charles Schappert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.victorschappert.notabs.PathUtil.traverse;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
/**
 * <p>
 * Trivial goal that simply looks through all project text files and fails the
 * build if any of them contains a tab character.
 * </p>
 *
 * @author Victor Schappert
 * @since 20160302
 */
@Mojo(name = "notabs", defaultPhase = LifecyclePhase.VALIDATE)
public final class NoTabsMojo extends AbstractMojo {

    //
    // DATA
    //

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${" + ENCODING_PROPERTY + "}", required = true, readonly = true)
    private String defaultEncoding;

//    @Parameter(defaultValue = "", required = false)
//    private String[] addglobs;

    @Parameter(defaultValue = "", required = false)
    private List<Object> subglobs;

    private Pattern[] addPats;

    //
    // INTERFACE: Mojo
    //

    @Override
    public void execute() throws MojoExecutionException {
        // TODO: Somewhere convert globparseexception into
        // mojoexecutionexception
        // TODO: Need to deal with Maven source encoding, XML file encoding, and
        // the fact that tab in UTF-16 is a.k.a contains NUL
        // TODO: Log if no source encoding specified
        // Use XMLStreamReader on XML -
        // https://docs.oracle.com/javase/8/docs/api/javax/xml/stream/XMLStreamReader.html
        // https://docs.oracle.com/javase/8/docs/api/javax/xml/stream/XMLInputFactory.html
        if (null == project) {
            throw new MojoExecutionException("Maven project is not set!");
        }
        Charset charset = null;
        if (null == defaultEncoding) {
            charset = Charset.defaultCharset();
            getLog().warn(
                    "Using platform encoding " + charset.name()
                            + " for checking tabs "
                            + "- your build is platform-dependent!"
                            + "(Set property " + ENCODING_PROPERTY
                            + " to correct this.)");
        }
        final File basedir = project.getBasedir();
        final Path basedirpath = basedir.toPath();
        if (getLog().isDebugEnabled()) {
            getLog().debug("Project base directory is " + basedirpath);
        }
//        addPats = glob2Regex(addglobs);
        final Pattern[] subPats = glob2Regex(subglobs);
        traverse(
                basedirpath,
                basedir,
                file -> {
                    if (!stream(subPats).anyMatch(
                            pat -> pat.matcher(file).matches())) {
                        return true;
                    } else {
                        if (getLog().isDebugEnabled()) {
                            getLog().debug(
                                    format("Skipping %s due to subglobs", file));
                        }
                        return false;
                    }
                }, this::checkForTabs);
        // algo
        // 3. for each file:
        // IF it matches a subglob, skip it
        // IF it matches an addglob, check it for tabs
        // OTHERWISE, check for tabs, skip it if binary

        // Code for each file is:
        // Have an 8K buffer
        // Have a special input stream that reads from the buffer
        // Have an input stream reader wrapping the special input stream
        // Every time we need to refill the buffer, trap and scan for NULs
        //
        // Code for XML is:
        // - Open a FileInputStream
        // - Read first 8KB
        // - Pump it through XmlStreamReader to get declared encoding
        // - Create ByteArrayInputStream on first 8KB
        // - Chain it with the FileInputStream into a SequenceInputStream
        // => NulCheckingStream nis = new NulCheckingStream(
        // new SequenceInputStream(bis, fis));
        // - so NulCheckingInputStream should have the BufferedInputStream
        // logic for skipping the buffer if exactly bufsize is requested

    } // execute()

    //
    // INTERNALS
    //

    private static final String ENCODING_PROPERTY = "project.build.sourceEncoding";

    private static Pattern[] glob2Regex(final List<Object> globs)
            throws MojoExecutionException {
        try {
            return globs.stream().map(Object::toString).map(GlobParser::parse)
                    .toArray(Pattern[]::new);
        } catch (GlobParseException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void checkForTabs(final File file, final String normalized) {
//        if (getLog().isDebugEnabled()) {
            getLog().debug(format("Checking %s for tabs", file));
//        }
    }
}

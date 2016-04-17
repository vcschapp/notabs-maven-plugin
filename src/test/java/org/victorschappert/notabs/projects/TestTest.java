package org.victorschappert.notabs.projects;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.victorschappert.notabs.NoTabsMojo;

public class TestTest extends MojoTestCase {

    @Test
    public void test() throws Exception {
        File pom = getTestFile("src/test/resources/projects/pom_only_platform_utf8_override_notabs/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());
        NoTabsMojo mojo = (NoTabsMojo)lookupConfiguredMojo(pom, "notabs");
        assertNotNull(mojo);
        mojo.execute();
    }
}

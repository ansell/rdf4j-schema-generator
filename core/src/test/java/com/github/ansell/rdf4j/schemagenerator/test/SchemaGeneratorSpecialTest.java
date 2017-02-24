package com.github.ansell.rdf4j.schemagenerator.test;

import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;
import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SchemaGeneratorSpecialTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private Path testDir;

    @Before
    public void before() throws IOException {
        testDir = tempDir.newFolder("vocabbuildertest").toPath();
    }

    @After
    public void tearDown() throws Exception {
        testDir = null;
    }

    @Test
    public final void testReservedWordsHandling() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(Resources.getResource("oa.ttl").getPath(), "text/turtle");

        Path javaFilePath = outputPath.resolve("OA.java");
        testBuilder.generate(javaFilePath);

        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(result.contains("public static final IRI hasTarget"));
        assertTrue(result.contains("public static final IRI _default"));
    }

}

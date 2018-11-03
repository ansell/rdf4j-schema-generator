package com.github.ansell.rdf4j.schemagenerator.test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SchemaGeneratorPrefixTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    private Path input;

    @Before
    public void setUp() throws Exception {
        input = temp.newFile("schema.rdf").toPath();
        Files.copy(getClass().getResourceAsStream("/schema.rdf"), input,
                StandardCopyOption.REPLACE_EXISTING);

        output = temp.newFile("SCHEMA.java").toPath();
    }

    @Test
    public void testVocabBuilderNoSettings() throws Exception {
        // test without settings
        final RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(
                input.toAbsolutePath().toString(), (String) null);
        vb.generate(output);

        final int count = Files.readAllLines(output, StandardCharsets.UTF_8).size();

        Assert.assertTrue("prefix was not set properly", count > 10);
    }

    @Test
    public void testVocabBuilderWithSettings() throws Exception {
        // test with settings
        final RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(
                input.toAbsolutePath().toString(), (String) null);
        vb.setPrefix("http://schema.org/");
        vb.generate(output);

        final int count = Files.readAllLines(output, StandardCharsets.UTF_8).size();

        Assert.assertTrue("prefix was not set properly", count > 10);
    }

}

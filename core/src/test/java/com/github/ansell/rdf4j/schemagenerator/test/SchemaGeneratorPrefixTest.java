package com.github.ansell.rdf4j.schemagenerator.test;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SchemaGeneratorPrefixTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Test
    public void testVocabBuilder() throws Exception {

        File input = temp.newFile("schema.rdf");
        Files.copy(getClass().getResourceAsStream("/schema.rdf"), input.toPath(), StandardCopyOption.REPLACE_EXISTING);

        output = temp.newFile("SCHEMA.java").toPath();

        //test without settings
        RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(input.getAbsolutePath(), (String) null);
        vb.generate(output);

        int count = Files.readAllLines(output, StandardCharsets.UTF_8).size();

        Assert.assertEquals("prefix was not set properly", 34, count);

        //test with settings
        vb = new RDF4JSchemaGeneratorCore(input.getAbsolutePath(), (String) null);
        vb.setPrefix("http://schema.org/");
        vb.generate(output);

        count = Files.readAllLines(output, StandardCharsets.UTF_8).size();

        Assert.assertEquals("prefix was not set properly", 10666, count);
    }

}

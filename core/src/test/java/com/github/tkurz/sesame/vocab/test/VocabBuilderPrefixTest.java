package com.github.tkurz.sesame.vocab.test;

import com.github.tkurz.sesame.vocab.GenerationException;
import com.github.tkurz.sesame.vocab.VocabBuilder;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.eclipse.rdf4j.model.util.GraphUtilException;
import org.eclipse.rdf4j.rio.RDFParseException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class VocabBuilderPrefixTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Test
    public void testVocabBuilder() throws IOException {

        File input = temp.newFile("schema.rdf");
        Files.copy(getClass().getResourceAsStream("/schema.rdf"), input.toPath(), StandardCopyOption.REPLACE_EXISTING);

        output = temp.newFile("SCHEMA.java").toPath();

        try {
            //test without settings
            VocabBuilder vb = new VocabBuilder(input.getAbsolutePath(), (String) null);
            vb.generate(output);

            int count = Files.readAllLines(output, StandardCharsets.UTF_8).size();

            Assert.assertEquals("prefix was not set properly", 34, count);

            //test with settings
            vb = new VocabBuilder(input.getAbsolutePath(), (String) null);
            vb.setPrefix("http://schema.org/");
            vb.generate(output);

            count = Files.readAllLines(output, StandardCharsets.UTF_8).size();

            Assert.assertEquals("prefix was not set properly", 10666, count);

        } catch (GenerationException e) {
            Assert.fail("Could not generate vocab " + e.getMessage());
        } catch (RDFParseException e) {
            Assert.fail("Could not parse test-file: " + e.getMessage());
        } catch (GraphUtilException e) {
            Assert.fail("Could not read vocabulary: " + e.getMessage());
        }
    }

}

package com.github.tkurz.sesame.vocab.test;

import com.github.tkurz.sesame.vocab.GenerationException;
import com.github.tkurz.sesame.vocab.VocabBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.rio.RDFParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.google.common.base.Charsets;
import com.google.common.io.Files;


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
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/schema.rdf"), input);

        output = temp.newFile("SCHEMA.java").toPath();

        try {
            //test without settings
            VocabBuilder vb = new VocabBuilder(input.getAbsolutePath(), (String) null);
            vb.generate(output);

            Integer count = Files.toString(new File(output.toString()), Charsets.UTF_8).split("\n").length;

            Assert.assertEquals("prefix was not set properly",(Integer)34,count);

            //test with settings
            vb = new VocabBuilder(input.getAbsolutePath(), (String) null);
            vb.setPrefix("http://schema.org/");
            vb.generate(output);

            count = Files.toString(new File(output.toString()), Charsets.UTF_8).split("\n").length;

            Assert.assertEquals("prefix was not set properly", (Integer) 10463, count);

        } catch (GenerationException e) {
            Assert.fail("Could not generate vocab " + e.getMessage());
        } catch (RDFParseException e) {
            Assert.fail("Could not parse test-file: " + e.getMessage());
        } catch (GraphUtilException e) {
            Assert.fail("Could not read vocabulary: " + e.getMessage());
        }
    }

}

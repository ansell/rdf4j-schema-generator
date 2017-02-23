package com.github.ansell.rdf4j.schemagenerator.test;


import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.ansell.rdf4j.schemagenerator.GenerationException;
import com.github.ansell.rdf4j.schemagenerator.VocabBuilder;

import org.eclipse.rdf4j.model.util.GraphUtilException;
import org.eclipse.rdf4j.rio.RDFParseException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

public class VocabBuilderCompileTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws IOException {
        File input = temp.newFile("ldp.ttl");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/ldp.ttl"), input);

        output = temp.newFile("LPD.java").toPath();

        try {
            VocabBuilder vb = new VocabBuilder(input.getAbsolutePath(), (String) null);
            vb.generate(output);
            System.out.println(output);
        } catch (GenerationException e) {
            Assert.fail("Could not generate vocab " + e.getMessage());
        } catch (RDFParseException e) {
            Assert.fail("Could not parse test-file: " + e.getMessage());
        } catch (GraphUtilException e) {
            Assert.fail("Could not read vocabulary: " + e.getMessage());
        }

    }


    @Test
    public void testVocabularyCompilation() throws ClassNotFoundException, MalformedURLException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        int result = compiler.run(null, null, null, output.toString());
        Assert.assertEquals("Compiling the Vocab failed", 0, result);

    }


}

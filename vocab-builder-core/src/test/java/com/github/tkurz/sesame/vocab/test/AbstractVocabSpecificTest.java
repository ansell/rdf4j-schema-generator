package com.github.tkurz.sesame.vocab.test;


import com.github.tkurz.sesame.vocab.GenerationException;
import com.github.tkurz.sesame.vocab.VocabBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public abstract class AbstractVocabSpecificTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws IOException {
        File input = temp.newFile(String.format("%s.%s", getBasename(), getFormat().getDefaultFileExtension()));
        FileUtils.copyInputStreamToFile(getInputStream(), input);

        output = temp.newFile(String.format("%S.java", getBasename())).toPath();

        try {
            VocabBuilder vb = new VocabBuilder(input.getAbsolutePath(), getFormat());
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

    protected abstract InputStream getInputStream();

    protected abstract String getBasename();

    protected abstract RDFFormat getFormat();

    @Test
    public void testCompilation() {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        int result = compiler.run(null, null, null, output.toString());
        Assert.assertEquals("Compiling the Vocab failed", 0, result);

    }


}

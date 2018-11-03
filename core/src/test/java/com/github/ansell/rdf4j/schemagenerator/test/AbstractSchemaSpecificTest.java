package com.github.ansell.rdf4j.schemagenerator.test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;

public abstract class AbstractSchemaSpecificTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws Exception {
        final File input = temp.newFile(
                String.format("%s.%s", getBasename(), getFormat().getDefaultFileExtension()));
        FileUtils.copyInputStreamToFile(getInputStream(), input);

        output = temp.newFile(String.format("%S.java", getBasename())).toPath();

        final RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(input.getAbsolutePath(),
                getFormat());
        vb.setPrefix(getPrefix());
        vb.generate(output);
        System.out.println(output);
    }

    protected abstract InputStream getInputStream();

    protected abstract String getBasename();

    protected abstract RDFFormat getFormat();

    protected abstract String getPrefix();

    @Test
    public void testCompilation() throws Exception {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final int result = compiler.run(null, null, null, output.toString());
        Assert.assertEquals("Compiling the Schema failed", 0, result);

    }

}

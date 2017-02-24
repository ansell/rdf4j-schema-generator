package com.github.ansell.rdf4j.schemagenerator.test;


import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.ansell.rdf4j.schemagenerator.GenerationException;
import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;

import org.eclipse.rdf4j.model.util.GraphUtilException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public abstract class AbstractSchemaSpecificTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws Exception {
        File input = temp.newFile(String.format("%s.%s", getBasename(), getFormat().getDefaultFileExtension()));
        FileUtils.copyInputStreamToFile(getInputStream(), input);

        output = temp.newFile(String.format("%S.java", getBasename())).toPath();

        RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(input.getAbsolutePath(), getFormat());
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

        int result = compiler.run(null, null, null, output.toString());
        Assert.assertEquals("Compiling the Schema failed", 0, result);

    }


}

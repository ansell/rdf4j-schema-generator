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
import org.eclipse.rdf4j.rio.RDFParseException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

public class SchemaGeneratorCompileTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws Exception {
        File input = temp.newFile("ldp.ttl");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/ldp.ttl"), input);

        output = temp.newFile("LPD.java").toPath();

        RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(input.getAbsolutePath(), (String) null);
        vb.generate(output);
        System.out.println(output);
    }


    @Test
    public void testSchemaCompilation() throws ClassNotFoundException, MalformedURLException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        int result = compiler.run(null, null, null, output.toString());
        Assert.assertEquals("Compiling the Vocab failed", 0, result);

    }


}

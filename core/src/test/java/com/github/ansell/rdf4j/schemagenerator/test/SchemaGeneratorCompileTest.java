package com.github.ansell.rdf4j.schemagenerator.test;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;

public class SchemaGeneratorCompileTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws Exception {
        final File input = temp.newFile("ldp.ttl");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/ldp.ttl"), input);

        output = temp.newFile("LPD.java").toPath();

        final RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(input.getAbsolutePath(),
                (String) null);
        vb.generate(output);
        System.out.println(output);
    }

    @Test
    public void testSchemaCompilation() throws ClassNotFoundException, MalformedURLException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final int result = compiler.run(null, null, null, output.toString());
        Assert.assertEquals("Compiling the Vocab failed", 0, result);

    }

}

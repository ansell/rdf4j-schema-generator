package com.github.ansell.rdf4j.schemagenerator.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;

public class SchemaGeneratorResourceBundleTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws Exception {
        final File input = temp.newFile("rdfs.ttl");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/rdfs.ttl"), input);

        output = temp.newFolder("bundleDir").toPath();

        final RDF4JSchemaGeneratorCore vb = new RDF4JSchemaGeneratorCore(input.getAbsolutePath(),
                (String) null);
        vb.generateResourceBundle("RDFS", output);
    }

    @Test
    public void testBundleGeneration() throws Exception {
        final String[] files = output.toFile().list();
        Assert.assertEquals(3, files.length);
        Assert.assertThat(Arrays.asList(files), CoreMatchers.hasItems("RDFS.properties",
                "RDFS_es.properties", "RDFS_fr.properties"));
    }

    @Test
    public void testDefaultBundle() throws Exception {
        final Properties p = loadBundle("RDFS.properties");

        Assert.assertEquals("Class", p.getProperty("Class.label"));
        Assert.assertEquals("The class of classes.", p.getProperty("Class.comment"));

        Assert.assertEquals("label", p.getProperty("label.label"));
        Assert.assertEquals("A human-readable name for the subject.",
                p.getProperty("label.comment"));

        Assert.assertEquals("isDefinedBy", p.getProperty("isDefinedBy.label"));
        Assert.assertEquals("The defininition of the subject resource.",
                p.getProperty("isDefinedBy.comment"));

        Assert.assertEquals("Container", p.getProperty("Container.label"));
        Assert.assertEquals("The class of RDF containers.", p.getProperty("Container.comment"));

    }

    private Properties loadBundle(String bundle) throws IOException {
        final Properties p = new Properties();
        try (BufferedReader is = Files.newBufferedReader(output.resolve(bundle),
                Charset.forName("utf-8"))) {
            p.load(is);
        }
        return p;
    }

    @Test
    public void testSpanishBundle() throws Exception {
        final Properties p = loadBundle("RDFS_es.properties");

        Assert.assertEquals("Clase", p.getProperty("Class.label"));
        Assert.assertEquals("La clase de todas las clases.", p.getProperty("Class.comment"));

        Assert.assertEquals("etiqueta", p.getProperty("label.label"));
        Assert.assertEquals("Un nombre dado al sujeto, legibile por un ser humano.",
                p.getProperty("label.comment"));

        Assert.assertEquals("definido por", p.getProperty("isDefinedBy.label"));
        Assert.assertEquals("La definición del recurso sujeto.",
                p.getProperty("isDefinedBy.comment"));

        Assert.assertEquals("Contenedor", p.getProperty("Container.label"));
        Assert.assertEquals("La clase de los contenedores RDF.",
                p.getProperty("Container.comment"));
    }

    @Test
    public void testFrenchBundle() throws Exception {
        final Properties p = loadBundle("RDFS_fr.properties");

        Assert.assertEquals("Classe", p.getProperty("Class.label"));
        Assert.assertEquals("La classe des toutes les classes.", p.getProperty("Class.comment"));

        Assert.assertEquals("label", p.getProperty("label.label"));
        Assert.assertEquals("Un nom du sujet lisible par un humain.",
                p.getProperty("label.comment"));

        Assert.assertEquals("estDéfiniPar", p.getProperty("isDefinedBy.label"));
        Assert.assertEquals("La définition de la ressource sujet.",
                p.getProperty("isDefinedBy.comment"));

        Assert.assertEquals("Conteneur", p.getProperty("Container.label"));
        Assert.assertEquals("La classe des conteneurs RDF.", p.getProperty("Container.comment"));
    }
}

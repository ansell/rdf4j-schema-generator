/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tkurz.sesame.vocab.test;

import com.github.tkurz.sesame.vocab.GenerationException;
import com.github.tkurz.sesame.vocab.VocabBuilder;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openrdf.rio.RDFParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

public class VocabBuilderResourceBundleTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private Path output;

    @Before
    public void setUp() throws IOException {
        File input = temp.newFile("rdfs.ttl");
        FileUtils.copyInputStreamToFile(getClass().getResourceAsStream("/rdfs.ttl"), input);

        output = temp.newFolder("bundleDir").toPath();

        try {
            VocabBuilder vb = new VocabBuilder(input.getAbsolutePath(), (String) null);
            vb.generateResourceBundle("RDFS", output);
        } catch (GenerationException e) {
            Assert.fail("Could not generate vocab " + e.getMessage());
        } catch (RDFParseException e) {
            Assert.fail("Could not parse test-file: " + e.getMessage());
        }

    }

    @Test
    public void testBundleGeneration() throws Exception {
        final String[] files = output.toFile().list();
        Assert.assertEquals(3, files.length);
        Assert.assertThat(Arrays.asList(files), CoreMatchers.hasItems("RDFS.properties", "RDFS_es.properties", "RDFS_fr.properties"));
    }

    @Test
    public void testDefaultBundle() throws Exception {
        Properties p = loadBundle("RDFS.properties");

        Assert.assertEquals("Class", p.getProperty("Class.label"));
        Assert.assertEquals("The class of classes.", p.getProperty("Class.comment"));

        Assert.assertEquals("label", p.getProperty("label.label"));
        Assert.assertEquals("A human-readable name for the subject.", p.getProperty("label.comment"));

        Assert.assertEquals("isDefinedBy", p.getProperty("isDefinedBy.label"));
        Assert.assertEquals("The defininition of the subject resource.", p.getProperty("isDefinedBy.comment"));

        Assert.assertEquals("Container", p.getProperty("Container.label"));
        Assert.assertEquals("The class of RDF containers.", p.getProperty("Container.comment"));

    }

    private Properties loadBundle(String bundle) throws IOException {
        Properties p = new Properties();
        try (BufferedReader is = Files.newBufferedReader(output.resolve(bundle), Charset.forName("utf-8"))) {
            p.load(is);
        }
        return p;
    }

    @Test
    public void testSpanishBundle() throws Exception {
        Properties p = loadBundle("RDFS_es.properties");

        Assert.assertEquals("Clase", p.getProperty("Class.label"));
        Assert.assertEquals("La clase de todas las clases.", p.getProperty("Class.comment"));

        Assert.assertEquals("etiqueta", p.getProperty("label.label"));
        Assert.assertEquals("Un nombre dado al sujeto, legibile por un ser humano.", p.getProperty("label.comment"));

        Assert.assertEquals("definido por", p.getProperty("isDefinedBy.label"));
        Assert.assertEquals("La definición del recurso sujeto.", p.getProperty("isDefinedBy.comment"));

        Assert.assertEquals("Contenedor", p.getProperty("Container.label"));
        Assert.assertEquals("La clase de los contenedores RDF.", p.getProperty("Container.comment"));
    }

    @Test
    public void testFrenchBundle() throws Exception {
        Properties p = loadBundle("RDFS_fr.properties");

        Assert.assertEquals("Classe", p.getProperty("Class.label"));
        Assert.assertEquals("La classe des toutes les classes.", p.getProperty("Class.comment"));

        Assert.assertEquals("label", p.getProperty("label.label"));
        Assert.assertEquals("Un nom du sujet lisible par un humain.", p.getProperty("label.comment"));

        Assert.assertEquals("estDéfiniPar", p.getProperty("isDefinedBy.label"));
        Assert.assertEquals("La définition de la ressource sujet.", p.getProperty("isDefinedBy.comment"));

        Assert.assertEquals("Conteneur", p.getProperty("Container.label"));
        Assert.assertEquals("La classe des conteneurs RDF.", p.getProperty("Container.comment"));
    }
}

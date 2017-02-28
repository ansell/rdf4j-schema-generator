/**
 *
 */
package com.github.ansell.rdf4j.schemagenerator.test;

import com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore;
import com.google.common.base.CaseFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link RDF4JSchemaGeneratorCore}
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
@RunWith(Parameterized.class)
public class SchemaGeneratorTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        Collection<Object[]> result = new ArrayList<>();
        for (RDFFormat nextParserFormat : RDFParserRegistry.getInstance().getKeys()) {
            try {
                // Try to create a writer, as not all formats (RDFa for example) have writers,
                // and we can't automatically test those formats like this
                OutputStream out = new ByteArrayOutputStream();
                Rio.createWriter(nextParserFormat, out);
                // If the writer creation did not throw an exception, add it to the list
                result.add(new Object[]{nextParserFormat});
            } catch(UnsupportedRDFormatException e) {
                // Ignore to drop this format from the list
            }
        }
        assertFalse("No RDFFormats found with RDFParser and RDFWriter implementations on classpath", result.isEmpty());
        return result;
    }

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private Path testDir;

    private IRI testOntologyUri;

    private IRI testProperty1;

    private IRI testProperty2;

    private IRI testProperty3;

    private IRI testProperty4;

    private Literal testProperty1Description;

    private Literal testProperty2Description;

    private Literal testProperty3Description;

    private Literal testProperty4DescriptionEn;

    private Literal testProperty4DescriptionFr;

    private RDFFormat format;

    private Path inputPath;

    public SchemaGeneratorTest(RDFFormat format) {
    	System.out.println("Running SchemaGeneratorTest for format: " + format);
        this.format = format;
    }

    @Before
    public void setUp() throws Exception {
        testDir = tempDir.newFolder("schema-generator-test").toPath();

        ValueFactory vf = SimpleValueFactory.getInstance();

        String ns = "http://example.com/ns/ontology#";
        testOntologyUri = vf.createIRI(ns);
        testProperty1 = vf.createIRI(ns, "property1");
        testProperty2 = vf.createIRI(ns, "property_2");
        testProperty3 = vf.createIRI(ns, "property-3");
        testProperty4 = vf.createIRI(ns, "propertyLocalised4");
        testProperty1Description = vf.createLiteral("property 1 description");
        testProperty2Description = vf.createLiteral("property 2 description");
        testProperty3Description = vf.createLiteral("property 3 description");
        testProperty4DescriptionEn = vf.createLiteral("property 4 description english", "en");
        testProperty4DescriptionFr = vf.createLiteral("Description de la propriété français", "fr");

        Model testOntology = new LinkedHashModel();
        testOntology.add(testOntologyUri, RDF.TYPE, OWL.ONTOLOGY);
        testOntology.add(testProperty1, RDF.TYPE, OWL.DATATYPEPROPERTY);
        testOntology.add(testProperty2, RDF.TYPE, OWL.OBJECTPROPERTY);
        testOntology.add(testProperty3, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
        testOntology.add(testProperty4, RDF.TYPE, OWL.ANNOTATIONPROPERTY);
        testOntology.add(testProperty1, DCTERMS.DESCRIPTION, testProperty1Description);
        testOntology.add(testProperty2, RDFS.COMMENT, testProperty2Description);
        testOntology.add(testProperty3, SKOS.DEFINITION, testProperty3Description);
        testOntology.add(testProperty4, SKOS.PREF_LABEL, testProperty4DescriptionEn);
        testOntology.add(testProperty4, SKOS.PREF_LABEL, testProperty4DescriptionFr);
        String fileName = "test." + format.getDefaultFileExtension();
        inputPath = testDir.resolve(fileName);
        try (final OutputStream outputStream = Files.newOutputStream(inputPath)) {
            Rio.write(testOntology, outputStream, format);
        }
    }

    @After
    public void tearDown() throws Exception {
        testDir = null;
    }

    /**
     * Test method for {@link com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore#generate(java.nio.file.Path)}.
     */
    @Test
    public final void testRun() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(inputPath.toAbsolutePath().toString(), format);

        testBuilder.setPreferredLanguage("fr");

        Path javaFilePath = outputPath.resolve("Test.java");
        testBuilder.generate(javaFilePath);
        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(result, result.contains(testProperty4DescriptionFr.getLabel()));
        assertFalse(result.contains(testProperty4DescriptionEn.getLabel()));
        assertTrue(result.contains("\"http://example.com/ns/ontology#property_2\""));
        assertTrue(result.contains("\"http://example.com/ns/ontology#property-3\""));
        assertTrue(result.contains("property_2 = "));
        assertTrue(result.contains("property_3 = "));
    }

    /**
     * Test method for {@link com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore#generate(java.nio.file.Path)}.
     */
    @Test
    public final void testUpperUnderscoreCase() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(inputPath.toAbsolutePath().toString(), format);

        testBuilder.setConstantCase(CaseFormat.UPPER_UNDERSCORE);

        Path javaFilePath = outputPath.resolve("Test.java");
        testBuilder.generate(javaFilePath);
        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue("Did not find expected key case", result.contains("PROPERTY_LOCALISED4 = "));
        assertTrue("Did not find original URI", result.contains("\"http://example.com/ns/ontology#propertyLocalised4\""));
    }

    /**
     * Test method for {@link com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore#generate(java.nio.file.Path)}.
     */
    @Test
    public final void testNoExplicitCase() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(inputPath.toAbsolutePath().toString(), format);

        testBuilder.setConstantCase(null);

        Path javaFilePath = outputPath.resolve("Test.java");
        testBuilder.generate(javaFilePath);
        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue("Did not find expected key case", result.contains("propertyLocalised4 = "));
        assertTrue("Did not find original URI", result.contains("\"http://example.com/ns/ontology#propertyLocalised4\""));
    }

    /**
     * Test method for {@link com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore#generate(java.nio.file.Path)}.
     */
    @Test
    public final void testUpperUnderscoreCaseString() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(inputPath.toAbsolutePath().toString(), format);

        testBuilder.setStringConstantCase(CaseFormat.UPPER_UNDERSCORE);

        Path javaFilePath = outputPath.resolve("Test.java");
        testBuilder.generate(javaFilePath);
        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue("Did not find expected key case", result.contains("PROPERTY_LOCALISED4 = "));
        assertTrue("Did not find original URI", result.contains("\"http://example.com/ns/ontology#propertyLocalised4\""));
    }

    /**
     * Test method for {@link com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore#generate(java.nio.file.Path)}.
     */
    @Test
    public final void testNoExplicitCaseString() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(inputPath.toAbsolutePath().toString(), format);

        testBuilder.setStringConstantCase(null);

        Path javaFilePath = outputPath.resolve("Test.java");
        testBuilder.generate(javaFilePath);
        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue("Did not find expected key case", result.contains("propertyLocalised4 = "));
        assertTrue("Did not find original URI", result.contains("\"http://example.com/ns/ontology#propertyLocalised4\""));
    }

    /**
     * Test method for {@link com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore#generate(java.nio.file.Path)}.
     */
    @Test
    public final void testUpperUnderscoreCaseLocalName() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(inputPath.toAbsolutePath().toString(), format);

        testBuilder.setLocalNameStringConstantCase(CaseFormat.UPPER_UNDERSCORE);

        Path javaFilePath = outputPath.resolve("Test.java");
        testBuilder.generate(javaFilePath);
        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue("Did not find expected key case", result.contains("PROPERTY_LOCALISED4 = "));
        assertTrue("Did not find original URI", result.contains("\"http://example.com/ns/ontology#propertyLocalised4\""));
    }

    /**
     * Test method for {@link com.github.ansell.rdf4j.schemagenerator.RDF4JSchemaGeneratorCore#generate(java.nio.file.Path)}.
     */
    @Test
    public final void testNoExplicitCaseLocalName() throws Exception {
        Path outputPath = testDir.resolve("output");
        Files.createDirectories(outputPath);

        RDF4JSchemaGeneratorCore testBuilder = new RDF4JSchemaGeneratorCore(inputPath.toAbsolutePath().toString(), format);

        testBuilder.setLocalNameStringConstantCase(null);

        Path javaFilePath = outputPath.resolve("Test.java");
        testBuilder.generate(javaFilePath);
        assertTrue("Java file was not found", Files.exists(javaFilePath));
        assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(javaFilePath, out);
        String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
        assertTrue("Did not find expected key case", result.contains("propertyLocalised4 = "));
        assertTrue("Did not find original URI", result.contains("\"http://example.com/ns/ontology#propertyLocalised4\""));
    }

}

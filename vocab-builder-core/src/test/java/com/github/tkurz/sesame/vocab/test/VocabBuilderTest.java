/**
 * 
 */
package com.github.tkurz.sesame.vocab.test;

import com.github.tkurz.sesame.vocab.VocabBuilder;
import com.google.common.base.CaseFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.*;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;

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
 * Tests for {@link VocabBuilder}
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
@RunWith(Parameterized.class)
public class VocabBuilderTest {

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
    	Collection<Object[]> result = new ArrayList<>();
    	for(RDFFormat nextParserFormat : RDFParserRegistry.getInstance().getKeys()) {
    		result.add(new Object[] { nextParserFormat });
    	}
    	assertFalse("No RDFFormats found with RDFParser implementations on classpath", result.isEmpty());
    	return result;
    }
    
    @Rule
	public TemporaryFolder tempDir = new TemporaryFolder();
	
	private Path testDir;
	
	private URI testOntologyUri;

	private URI testProperty1;
	
	private URI testProperty2;
	
	private URI testProperty3;
	
	private URI testProperty4;
	
	private Literal testProperty1Description;
	
	private Literal testProperty2Description;
	
	private Literal testProperty3Description;

	private Literal testProperty4DescriptionEn;

	private Literal testProperty4DescriptionFr;

	private RDFFormat format;

	private Path inputPath;
	
	public VocabBuilderTest(RDFFormat format) {
		this.format = format;
	}
	
	@Before
	public void setUp() throws Exception {
		testDir = tempDir.newFolder("vocabbuildertest").toPath();
		
		ValueFactory vf = ValueFactoryImpl.getInstance();
		
		String ns = "http://example.com/ns/ontology#";
		testOntologyUri = vf.createURI(ns);
		testProperty1 = vf.createURI(ns, "property1");
		testProperty2 = vf.createURI(ns, "property2");
		testProperty3 = vf.createURI(ns, "property3");
		testProperty4 = vf.createURI(ns, "propertyLocalised4");
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
		String fileName = "test."+ format.getDefaultFileExtension();
		inputPath = testDir.resolve(fileName);
		try(final OutputStream outputStream = Files.newOutputStream(inputPath)) {
			Rio.write(testOntology, outputStream, format);
		}
	}

	@After
	public void tearDown() throws Exception {
		testDir = null;
	}

	/**
	 * Test method for {@link com.github.tkurz.sesame.vocab.VocabBuilder#generate(java.nio.file.Path)}.
	 */
	@Test
	public final void testRun() throws Exception {
		Path outputPath = testDir.resolve("output");
		Files.createDirectories(outputPath);
		
		VocabBuilder testBuilder = new VocabBuilder(inputPath.toAbsolutePath().toString(), format);
		
		testBuilder.setPreferredLanguage("fr");
		
		Path javaFilePath = outputPath.resolve("Test.java");
		testBuilder.generate(javaFilePath);
		assertTrue("Java file was not found", Files.exists(javaFilePath));
		assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Files.copy(javaFilePath, out);
		String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
		assertTrue(result.contains(testProperty4DescriptionFr.getLabel()));
		assertFalse(result.contains(testProperty4DescriptionEn.getLabel()));
	}

	/**
	 * Test method for {@link com.github.tkurz.sesame.vocab.VocabBuilder#generate(java.nio.file.Path)}.
	 */
	@Test
	public final void testUpperUnderscoreCase() throws Exception {
		Path outputPath = testDir.resolve("output");
		Files.createDirectories(outputPath);
		
		VocabBuilder testBuilder = new VocabBuilder(inputPath.toAbsolutePath().toString(), format);
		
		testBuilder.setConstantCase(CaseFormat.UPPER_UNDERSCORE);
		
		Path javaFilePath = outputPath.resolve("Test.java");
		testBuilder.generate(javaFilePath);
		assertTrue("Java file was not found", Files.exists(javaFilePath));
		assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Files.copy(javaFilePath, out);
		String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
		assertTrue("Did not find expected key case", result.contains("PROPERTY_LOCALISED4 = "));
		assertTrue("Did not find original URI", result.contains("\"propertyLocalised4\""));
	}

	/**
	 * Test method for {@link com.github.tkurz.sesame.vocab.VocabBuilder#generate(java.nio.file.Path)}.
	 */
	@Test
	public final void testNoExplicitCase() throws Exception {
		Path outputPath = testDir.resolve("output");
		Files.createDirectories(outputPath);
		
		VocabBuilder testBuilder = new VocabBuilder(inputPath.toAbsolutePath().toString(), format);
		
		testBuilder.setConstantCase(null);
		
		Path javaFilePath = outputPath.resolve("Test.java");
		testBuilder.generate(javaFilePath);
		assertTrue("Java file was not found", Files.exists(javaFilePath));
		assertTrue("Java file was empty", Files.size(javaFilePath) > 0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Files.copy(javaFilePath, out);
		String result = new String(out.toByteArray(), StandardCharsets.UTF_8);
		assertTrue("Did not find expected key case", result.contains("propertyLocalised4 = "));
		assertTrue("Did not find original URI", result.contains("\"propertyLocalised4\""));
	}

}

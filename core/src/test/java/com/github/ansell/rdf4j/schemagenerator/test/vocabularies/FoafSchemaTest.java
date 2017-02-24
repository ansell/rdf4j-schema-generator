package com.github.ansell.rdf4j.schemagenerator.test.vocabularies;

import org.junit.Assume;

import com.github.ansell.rdf4j.schemagenerator.test.AbstractSchemaSpecificTest;

import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FoafSchemaTest extends AbstractSchemaSpecificTest {

    @Override
    protected InputStream getInputStream() {
        try {
            return new URL("http://xmlns.com/foaf/spec/index.rdf").openStream();
        } catch (IOException e) {
            Assume.assumeNoException(e);
            return null;
        }
    }

    @Override
    protected String getBasename() {
        return "foaf";
    }

    @Override
    protected RDFFormat getFormat() {
        return RDFFormat.RDFXML;
    }
    
    @Override
    protected String getPrefix() {
    	return "http://xmlns.com/foaf/0.1/";
    }
}

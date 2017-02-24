package com.github.ansell.rdf4j.schemagenerator.test.vocabularies;

import com.github.ansell.rdf4j.schemagenerator.test.AbstractSchemaSpecificTest;

import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.InputStream;

public class GNASchemaTest extends AbstractSchemaSpecificTest {

    @Override
    protected InputStream getInputStream() {
        return this.getClass().getResourceAsStream("/gna.rdf");
    }

    @Override
    protected String getBasename() {
        return "gna";
    }

    @Override
    protected RDFFormat getFormat() {
        return RDFFormat.RDFXML;
    }
}

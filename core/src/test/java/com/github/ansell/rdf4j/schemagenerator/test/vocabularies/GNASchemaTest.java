package com.github.ansell.rdf4j.schemagenerator.test.vocabularies;

import java.io.InputStream;

import org.eclipse.rdf4j.rio.RDFFormat;

import com.github.ansell.rdf4j.schemagenerator.test.AbstractSchemaSpecificTest;

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

    @Override
    protected String getPrefix() {
        return "http://rs.gbif.org/terms/1.0/";
    }
}

package com.github.tkurz.sesame.vocab.test.vocabularies;

import com.github.tkurz.sesame.vocab.test.AbstractVocabSpecificTest;
import org.junit.Assume;
import org.openrdf.rio.RDFFormat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FoafVocabTest extends AbstractVocabSpecificTest {

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
}

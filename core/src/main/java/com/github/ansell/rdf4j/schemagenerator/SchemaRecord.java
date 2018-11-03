package com.github.ansell.rdf4j.schemagenerator;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

/**
 * A record from a Schema, including the IRI, along with any Label or
 * Description available.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public interface SchemaRecord {

    /**
     * 
     * @return The IRI for this schema record. Must not be null.
     */
    IRI getIRI();

    /**
     * 
     * @return The record key to use for identifying this schema record in an
     *         output. Must not be null.
     */
    String getFormattedRecordKey();

    /**
     * 
     * @return The raw record key before formatting it. Must not be null.
     */
    String getRawRecordKey();

    /**
     * 
     * @return A label for this IRI, returning {@link Optional#empty()} if none
     *         is available.
     */
    Optional<Literal> getLabel();

    /**
     * 
     * @return A description for this IRI, returning {@link Optional#empty()} if
     *         none is available.
     */
    Optional<Literal> getDescription();
}

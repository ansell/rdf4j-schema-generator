/**
 *
 */
package com.github.ansell.rdf4j.schemagenerator;

import org.eclipse.rdf4j.model.IRI;

/**
 * A high level interface used to denote the vital details of a schema and
 * enable a reference for registry based auto-discovery.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public interface Schema {

    /**
     * 
     * @return The IRI used as the base for this schema.
     */
    IRI getIRI();

}

package com.github.ansell.rdf4j.schemagenerator.internal;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import com.github.ansell.rdf4j.schemagenerator.SchemaRecord;

/**
 * Implementation of {@link SchemaRecord}.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class SchemaRecordImpl implements SchemaRecord {

	private final IRI iri;
	private final String formattedRecordKey;
	private final String rawRecordKey;
	private final Literal label;
	private final Literal description;

	public SchemaRecordImpl(IRI nextIRI, String formattedRecordKey, String rawRecordKey, Literal nextLabel, Literal nextDescription) {
		this.iri = Objects.requireNonNull(nextIRI, "IRI cannot be null");
		this.formattedRecordKey = Objects.requireNonNull(formattedRecordKey, "Formatted record key cannot be null for IRI: " + nextIRI);
		this.rawRecordKey = Objects.requireNonNull(rawRecordKey, "Raw record key cannot be null for IRI: " + nextIRI);
		this.label = nextLabel;
		this.description = nextDescription;
	}

	@Override
	public IRI getIRI() {
		return iri;
	}

	@Override
	public String getFormattedRecordKey() {
		return formattedRecordKey;
	}

	@Override
	public String getRawRecordKey() {
		return rawRecordKey;
	}

	@Override
	public Optional<Literal> getLabel() {
		return Optional.ofNullable(label);
	}

	@Override
	public Optional<Literal> getDescription() {
		return Optional.ofNullable(description);
	}

}

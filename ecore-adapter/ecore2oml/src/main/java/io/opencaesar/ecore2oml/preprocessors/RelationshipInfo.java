package io.opencaesar.ecore2oml.preprocessors;

import org.eclipse.emf.ecore.EStructuralFeature;

import io.opencaesar.ecore2oml.util.Util;

public class RelationshipInfo {
	
	private String sourceIRI;
	private int src = 0;
	public String getSourceIRI() {
		return sourceIRI;
	}

	public void setSourceIRI(String sourceIRI) {
		this.sourceIRI = sourceIRI;
		src +=1;
	}

	public String getTargetIRI() {
		return targetIRI;
	}

	public void setTargetIRI(String targetIRI) {
		this.targetIRI = targetIRI;
		trg+=1;
	}

	private String targetIRI;
	private int trg= 0;
	
	public RelationshipInfo(String sourceIRI, String targetIRI) {
		this.sourceIRI = sourceIRI;
		this.targetIRI = targetIRI;
	}
	
	public boolean isSource(EStructuralFeature toCheck) {
		return Util.getIri(toCheck).equals(sourceIRI);
	}
	
	public boolean isTarget(EStructuralFeature toCheck) {
		return Util.getIri(toCheck).equals(targetIRI);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "Source: " + sourceIRI + " Target: " + targetIRI );
		builder.append(" " + src + " - " + trg);
		return builder.toString();
	}

}

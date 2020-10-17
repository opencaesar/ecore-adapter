package io.opencaesar.ecore2oml.util;

import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;

public class Relationship {
	
	public String root;
	public  String source;
	public String target;
	public String forwardPostFix;
	public String forwardName;
	public List<ForwardOverride> overrides;
	
	public boolean isSource(EStructuralFeature toCheck) {
		return Util.getIri(toCheck).equals(source);
	}
	
	public boolean isTarget(EStructuralFeature toCheck) {
		return Util.getIri(toCheck).equals(target);
	}
	
	public Relationship(String iri, String source, String target) {
		this.root = iri;
		this.source = source;
		this.target = target;
	}
	

}

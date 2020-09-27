package io.opencaesar.ecore2oml;

import static io.opencaesar.ecore2oml.NameSpaces.DC;
import static io.opencaesar.ecore2oml.NameSpaces.Ecore;
import static io.opencaesar.ecore2oml.NameSpaces.OML;
import static io.opencaesar.ecore2oml.NameSpaces.RDFS;
import static io.opencaesar.ecore2oml.NameSpaces.XSD;
import static io.opencaesar.oml.util.OmlRead.getMembers;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.exists;

import java.util.Optional;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class Util {
	
	
	
	public static String getMappedName(ENamedElement object) {
		return getAnnotationValue(object, AnnotationKind.name, object.getName());
	}
	
	public static String getAnnotationValue(EModelElement object, AnnotationKind kind) {
		final Optional<EAnnotation> annotation = object.getEAnnotations().stream().
			filter(i -> OML.equals(i.getSource())).findFirst();
		if (annotation.isPresent()) {
			return annotation.get().getDetails().get(kind.toString());
		}
		return null;
	}
	
	public static String getAnnotationValue(EModelElement object, AnnotationKind kind, String defaultValue) {
		final String value = getAnnotationValue(object, kind);
		return (value != null) ? value : defaultValue;
	}
	
		
	public static boolean isAnnotationSet(EModelElement object, AnnotationKind kind) {
		return getAnnotationValue(object, kind) != null;
	}
	
	public static void addGeneratedAnnotation(Member object, OmlWriter oml, Vocabulary vocabulary) {
		Literal generated = oml.createQuotedLiteral(vocabulary, "generated", null, null);
		oml.addAnnotation(vocabulary, OmlRead.getIri(object), DC+"/source", generated);
	}
	
	public static void addLabelAnnotatiopnIfNeeded(Member object, String mappedName, OmlWriter oml, Vocabulary vocabulary) {
		String originalName = object.getName();
		if (!originalName.equals(mappedName)) {
			Literal label = oml.createQuotedLiteral(vocabulary, originalName, null, null);
			oml.addAnnotation(vocabulary, OmlRead.getIri(object), RDFS+"#label", label);
		}
	}
		
	public static String getPrefix(EPackage object) {
		return object.getNsPrefix();
	}	

	public static SeparatorKind getSeparator(EPackage object) {
		final String nsURI = object.getNsURI();
		if (nsURI.endsWith("/")) {
			return SeparatorKind.SLASH;
		} else {
			return SeparatorKind.HASH;
		}
	}
	
	public static boolean memberExists(String name, Vocabulary vocabulary) {
		return exists(getMembers(vocabulary), i -> i.getName().equals(name));		
	}
	
	public static String getIri(final ENamedElement object, Vocabulary vocabulary, OmlWriter oml) {
		if (object instanceof EPackage) {
			return getIri((EPackage) object);
		} else if (object instanceof EClass) {
			return getIri((EClass) object,vocabulary,oml);
		} else if (object instanceof EEnum) {
			return getIri((EEnum) object,vocabulary,oml);
		} else if (object instanceof EDataType) {
			return getIri((EDataType) object,vocabulary,oml);
		} else if (object instanceof EStructuralFeature) {
			return getIri((EStructuralFeature) object);
		}
		return null;
	}

	static public String getIri(EPackage object) {
		String nsURI = object.getNsURI();
		if (nsURI.endsWith("#") || nsURI.endsWith("/")) {
			nsURI = nsURI.substring(0, nsURI.length()-1);
		}
		return nsURI;
	}	

	public static String getIri(EClass object, Vocabulary vocabulary, OmlWriter oml) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object), object,vocabulary,oml);
		}
		return null;
	}	

	public static String getIri(EEnum object, Vocabulary vocabulary, OmlWriter oml) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object), object,vocabulary,oml);
		}
		return null;
	}	

	static public String getIri(EDataType object, Vocabulary vocabulary, OmlWriter oml) {
		final String name = getMappedName(object);
		
		if (EcorePackage.Literals.ESTRING.getName().equals(name))
			return XSD+"#string";
		if (EcorePackage.Literals.EINT.getName().equals(name))
			return XSD+"#int";
		if (EcorePackage.Literals.EINTEGER_OBJECT.getName().equals(name))
			return XSD+"#int";
		if (EcorePackage.Literals.EBOOLEAN.getName().equals(name))
			return XSD+"#boolean";
		if (EcorePackage.Literals.EDOUBLE.getName().equals(name))
			return XSD+"#double";
		if (EcorePackage.Literals.EDOUBLE_OBJECT.getName().equals(name))
			return XSD+"#double";
		if (EcorePackage.Literals.EFLOAT.getName().equals(name))
			return XSD+"#float";
		if (EcorePackage.Literals.EFLOAT_OBJECT.getName().equals(name))
			return XSD+"#float";
		if (EcorePackage.Literals.EBIG_DECIMAL.getName().equals(name))
			return XSD+"#decimal";

		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+getSeparator(ePackage)+object.getName(), object,vocabulary,oml);
		}
		return null;
	}

	static public String getIri(EStructuralFeature object) {
		final EPackage ePackage = object.getEContainingClass().getEPackage();  
		return getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object);
	}	

	static private String qualify(String iri, EClassifier object, Vocabulary vocabulary, OmlWriter oml) {
		final String vocabularyIri = getIri(object.getEPackage());
		if (!vocabularyIri.equals(vocabulary.getIri()) && !Ecore.equals(vocabularyIri)) {
			if (!vocabulary.getOwnedImports().stream().anyMatch(i -> i.getUri().equals(vocabularyIri))) {
				oml.addVocabularyExtension(vocabulary, vocabularyIri, null);
			}
		}
		return iri;
	}
}
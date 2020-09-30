package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.Util.addLabelAnnotatiopnIfNeeded;
import static io.opencaesar.ecore2oml.Util.getAnnotationValue;
import static io.opencaesar.ecore2oml.Util.getIri;
import static io.opencaesar.ecore2oml.Util.getMappedName;
import static io.opencaesar.ecore2oml.Util.handleNamedElementDoc;
import static io.opencaesar.ecore2oml.Util.isAnnotationSet;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EClassHandler implements ConversionHandler  {

	@Override
	public EObject convert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) {
		EClass object = (EClass)eObject;
		final String name = getMappedName(object);

		Entity entity = null;
		if (isAnnotationSet(object, AnnotationKind.isRelationEntity)) {
			entity = convertEClassToRelationEntity(object,oml,vocabulary);
		} else if (isAspect(object)) {
			entity = oml.addAspect(vocabulary, getMappedName(object));
		} else {
			entity = oml.addConcept(vocabulary, getMappedName(object));
		}
		for (EClass eSuperType : object.getESuperTypes()) {
			String superIri = getIri(eSuperType,vocabulary,oml);
			if (superIri != null) {
				oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(entity), superIri);
			}
		}
		addLabelAnnotatiopnIfNeeded(entity, name,oml,vocabulary);
		handleNamedElementDoc(object, entity,oml,vocabulary);
		return entity;
	}
	
	static private RelationEntity convertEClassToRelationEntity(EClass object, OmlWriter oml, Vocabulary vocabulary) {
		final String sourceIri = getAnnotatedElementIri(object.getEAllReferences(), AnnotationKind.isRelationSource,oml,vocabulary);
		final String targetIri = getAnnotatedElementIri(object.getEAllReferences(), AnnotationKind.isRelationTarget,oml, vocabulary);
		final String forward = getAnnotationValue(object, AnnotationKind.forwardName);
		final String reverse = getAnnotationValue(object, AnnotationKind.reverseName);

		final RelationEntity entity = oml.addRelationEntity(
			vocabulary, getMappedName(object),sourceIri, targetIri,
			false, false, false, false, false, false, false);
		oml.addForwardRelation(entity, forward);
		if (reverse != null) {
			oml.addReverseRelation(entity, reverse);
		}
		
		return entity;
	}
	
	static private <T extends ENamedElement> String getAnnotatedElementIri(Collection<T> coll, AnnotationKind kind, OmlWriter oml, Vocabulary vocabulary) {
		final Optional<T> object = coll.stream().filter(i -> isAnnotationSet(i, kind)).findFirst();
		if (object.isPresent()) {
			return getIri(object.get(),vocabulary,oml);
		}
		return null;
	}
	
	static private boolean isAspect(EClass object) {
		return (object.eIsProxy() || object.isAbstract() || object.isInterface()) && 
				object.getESuperTypes().stream().allMatch(i -> isAspect(i));
	}
}

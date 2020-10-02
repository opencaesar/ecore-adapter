package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.Util.addGeneratedAnnotation;
import static io.opencaesar.ecore2oml.Util.addLabelAnnotatiopnIfNeeded;
import static io.opencaesar.ecore2oml.Util.getAnnotationValue;
import static io.opencaesar.ecore2oml.Util.getIri;
import static io.opencaesar.ecore2oml.Util.getMappedName;
import static io.opencaesar.ecore2oml.Util.handleNamedElementDoc;
import static io.opencaesar.ecore2oml.Util.isAnnotationSet;
import static io.opencaesar.ecore2oml.Util.memberExists;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.CONSTANTS;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.CollidingEOppositeData;
import io.opencaesar.ecore2oml.preprocessors.RefCollisionInfo;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EReferenceHandler implements ConversionHandler{
	
	public  EObject convert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) {
		EReference object = (EReference)eObject;
		if (object.isDerived()) {
			return null;
		}
		if (isAnnotationSet(object, AnnotationKind.ignore)) {
			return null;
		}
		if (isAnnotationSet(object.getEReferenceType(), AnnotationKind.ignore)) {
			return null;
		}
		if (isAnnotationSet(object, AnnotationKind.isRelationSource) || 
			isAnnotationSet(object, AnnotationKind.isRelationTarget)) {
			return null;
		}
		
		String sourceIri = getIri(object.getEContainingClass(), vocabulary, oml);
		String targetIri = getIri(object.getEReferenceType(), vocabulary, oml);
		final EReference opposite = object.getEOpposite();
		final boolean isFunctional = object.getUpperBound() == 1;
		final boolean isInverseFunctional = (opposite != null) && opposite.getLowerBound() == 1;
		@SuppressWarnings("unchecked")
		Map<String, RefCollisionInfo> names = (Map<String, RefCollisionInfo>) collections.get(CollectionKind.CollidingRefernces);
		CollidingEOppositeData collidingEOpposite = (CollidingEOppositeData) collections.get(CollectionKind.CollidingEOppositeRefernces);
		final String name = getMappedName(object);
		final String entityName =  StringExtensions.toFirstUpper(name) + CONSTANTS.EREFERENCE_POSTFIX;
		RefCollisionInfo collisionInfo = names!=null ? names.get(name) : null;


		if (opposite!=null && collidingEOpposite!=null &&  collidingEOpposite.shouldSkip(object)) {
			return null;
		}
		
		String refIRI = getIri(object, vocabulary, oml);
		String rangeIRI = getIri(object.getEType(), vocabulary, oml);
		
		if (collisionInfo!=null) {
			// create the base source, and target
			if (collisionInfo.fromAspect==null) {
				String baseNameFrom = CONSTANTS.BASE_PREFIX + entityName + CONSTANTS.FROM;
				collisionInfo.fromAspect = oml.addAspect(vocabulary, baseNameFrom);
				addGeneratedAnnotation(collisionInfo.fromAspect, oml, vocabulary);
				String baseNameTo = CONSTANTS.BASE_PREFIX + entityName + CONSTANTS.To;
				collisionInfo.toAspect = oml.addAspect(vocabulary, baseNameTo);
				addGeneratedAnnotation(collisionInfo.toAspect, oml, vocabulary);
			}
			sourceIri =  OmlRead.getIri(collisionInfo.fromAspect);
			targetIri =  OmlRead.getIri(collisionInfo.toAspect);
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEContainingClass(), vocabulary, oml), sourceIri);
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEReferenceType(), vocabulary, oml), targetIri);
		}
		
		
		
		// the relation entity's source
		if (isAnnotationSet(object, AnnotationKind.domainName)) {
			final String aspectName = getAnnotationValue(object, AnnotationKind.domainName);
			if (!memberExists(aspectName, vocabulary)) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				addGeneratedAnnotation(aspect, oml, vocabulary);
			}
			final String aspectIri = OmlRead.getNamespace(vocabulary) + aspectName;
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEContainingClass(), vocabulary, oml), aspectIri);
			sourceIri = aspectIri;
		}
		RelationEntity entity=null;

		if (collisionInfo==null || collisionInfo.entity==null) {
			entity = oml.addRelationEntity(vocabulary, entityName, sourceIri, targetIri, isFunctional, isInverseFunctional,
					false, false, false, false, false);
			
			// the forward relation
			final String forwardName = name;
			ForwardRelation forward = oml.addForwardRelation(entity, forwardName);
			addLabelAnnotatiopnIfNeeded(object,forward, oml, vocabulary);
			if (collisionInfo!=null) {
				collisionInfo.entity = entity;
				collisionInfo.forward = forward;
			}
			// the reverse relation
			if (opposite != null) {
				String reverseName = getMappedName(opposite);
				if (isAnnotationSet(object, AnnotationKind.reverseName)) {
					reverseName = getAnnotationValue(object, AnnotationKind.reverseName);
				}
				ReverseRelation reverse = oml.addReverseRelation(entity, reverseName);
				addLabelAnnotatiopnIfNeeded(opposite,reverse, oml, vocabulary);
			}
		}
		
		// forward restriction happen last
		if (collisionInfo!=null) {
			oml.addRelationRangeRestrictionAxiom(vocabulary,
						getIri(object.getEContainingClass(), vocabulary, oml),
					   OmlRead.getIri(collisionInfo.forward),
					   getIri(object.getEReferenceType(), vocabulary, oml),
					   RangeRestrictionKind.ALL);
		}
		
		if (!isFunctional && object.getUpperBound()!=-1) {
			// TODO: remove the range IRI
			oml.addRelationCardinalityRestrictionAxiom(vocabulary, sourceIri,  refIRI,
					CardinalityRestrictionKind.MAX, object.getUpperBound(), rangeIRI);
			oml.addRelationCardinalityRestrictionAxiom(vocabulary, sourceIri,  refIRI,
					CardinalityRestrictionKind.MIN, object.getLowerBound(), rangeIRI);
		}
		
		handleNamedElementDoc(object, entity,oml,vocabulary);
		return entity;
	}

}

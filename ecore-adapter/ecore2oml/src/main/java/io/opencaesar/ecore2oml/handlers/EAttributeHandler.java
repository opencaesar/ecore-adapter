package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.Util.addGeneratedAnnotation;
import static io.opencaesar.ecore2oml.Util.addLabelAnnotatiopnIfNeeded;
import static io.opencaesar.ecore2oml.Util.getAnnotationValue;
import static io.opencaesar.ecore2oml.Util.getIri;
import static io.opencaesar.ecore2oml.Util.getMappedName;
import static io.opencaesar.ecore2oml.Util.isAnnotationSet;
import static io.opencaesar.ecore2oml.Util.memberExists;

import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.CONSTANTS;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.CollisionInfo;
import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EAttributeHandler implements ConversionHandler {

	static public ScalarProperty handleEAttributeToScalarProperty(EAttribute object, String domainIri, String rangeIri,
			OmlWriter oml, Vocabulary vocabulary, Map<CollectionKind, Object> collections) {
		final String name = getMappedName(object);
		// check for collision
		@SuppressWarnings("unchecked")
		Map<String, CollisionInfo> names = (Map<String, CollisionInfo>) collections
				.get(CollectionKind.CollidingAttributes);
		CollisionInfo collisionInfo = names!=null ? names.get(name) : null;
		final boolean isFunctional = object.getUpperBound() == 1;
		if (collisionInfo != null) {
			// fix the rangeIRI
			String realName = CONSTANTS.BASE_PREFIX + StringExtensions.toFirstUpper(name);
			if (!memberExists(realName, vocabulary)) {
				collisionInfo.baseConcept = oml.addAspect(vocabulary, realName);
				collisionInfo.baseProperty = oml.addScalarProperty(vocabulary, name,
						OmlRead.getIri(collisionInfo.baseConcept), rangeIri, isFunctional);
			}
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEContainingClass(), vocabulary, oml),
					OmlRead.getIri(collisionInfo.baseConcept));
			if (!collisionInfo.sameType() && !collisionInfo.getName().equals("value")) {
				oml.addScalarPropertyRangeRestrictionAxiom(vocabulary,
						getIri(object.getEContainingClass(), vocabulary, oml),
						OmlRead.getIri(collisionInfo.baseProperty), rangeIri, RangeRestrictionKind.ALL);

			}
			return collisionInfo.baseProperty;
		}
		return oml.addScalarProperty(vocabulary, name, domainIri, rangeIri, isFunctional);
	}

	@Override
	public EObject convert(EObject oObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) {
		EAttribute object = (EAttribute) oObject;
		final EClass domain = object.getEContainingClass();
		final EDataType range = object.getEAttributeType();
		final boolean isDerived = object.isDerived();

		if (isDerived) {
			return null;
		}
		if (isAnnotationSet(object, AnnotationKind.ignore)) {
			return null;
		}
		if (isAnnotationSet(range, AnnotationKind.ignore)) {
			return null;
		}

		// find the domain
		String domainIri = getIri(domain, vocabulary, oml);
		if (isAnnotationSet(object, AnnotationKind.domainName)) {
			final String aspectName = getAnnotationValue(object, AnnotationKind.domainName);
			if (!memberExists(aspectName, vocabulary)) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				addGeneratedAnnotation(aspect, oml, vocabulary);
			}
			final String aspectIri = OmlRead.getNamespace(vocabulary) + aspectName;
			oml.addSpecializationAxiom(vocabulary, getIri(domain, vocabulary, oml), aspectIri);
			domainIri = aspectIri;
		}

		// find the range
		String rangeIri = getIri(range, vocabulary, oml);

		// create Property
		Property property = null;
		if (isAnnotationSet(object, AnnotationKind.isAnnotationProperty)) {
			property = caseEAttributeToAnnotationProperty(object, oml, vocabulary);
		} else {
			property = handleEAttributeToScalarProperty(object, domainIri, rangeIri, oml, vocabulary, collections);
		}
		//addLabelAnnotatiopnIfNeeded(property, name, oml, vocabulary);
		addLabelAnnotatiopnIfNeeded(object,property, oml, vocabulary);
		return property;
	}

	static private AnnotationProperty caseEAttributeToAnnotationProperty(EAttribute object, OmlWriter oml,
			Vocabulary vocabulary) {
		final String name = getMappedName(object);
		return oml.addAnnotationProperty(vocabulary, name);
	}

}

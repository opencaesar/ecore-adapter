package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.addLabelAnnotatiopnIfNeeded;
import static io.opencaesar.ecore2oml.util.Util.handleNamedElementDoc;

import java.util.Map;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreSwitch;

import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public interface ConversionHandler {
	

	public EObject doConvert(EObject object, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,EcoreSwitch<EObject> visitor) ;
	
	default public void postConvert(Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) {
		// No OP
	}
	
	default public EObject convert(EObject object, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,EcoreSwitch<EObject> visitor) {
		EObject retVal = doConvert(object, vocabulary, oml, collections, visitor);
		if (object instanceof ENamedElement && retVal instanceof Member) {
			addLabelAnnotatiopnIfNeeded((ENamedElement)object,(Member)retVal, oml, vocabulary);
			handleNamedElementDoc((ENamedElement)object, (Member)retVal,oml,vocabulary);
		}
		return retVal;
	}
	

}

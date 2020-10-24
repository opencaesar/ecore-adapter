package io.opencaesar.ecore2oml.preprocessors;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreSwitch;

public class ConversionPreProcessing extends EcoreSwitch<EObject> {

	private Map<Integer, Set<ConversionParticipant>> participants = new HashMap<>();
	private Map<CollectionKind, Object> collections;
	
	private Logger LOGGER = LogManager.getLogger(ConversionPreProcessing.class);

	public void setCollections(Map<CollectionKind, Object> collections) {
		this.collections = collections;
	}

	public void addParticipant(Integer id, Class<? extends ConversionParticipant> part) {
		Set<ConversionParticipant> container = participants.get(id);
		if (container == null) {
			container = new HashSet<ConversionParticipant>();
			participants.put(id, container);
		}
		try {
			container.add(part.getDeclaredConstructor().newInstance());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			LOGGER.error("Failed to create instance of " + part);
		}

	}

	public void run(EPackage ePackage) {
		doSwitch(ePackage);
	}

	@Override
	public EObject caseEPackage(EPackage object) {
		callPreProcessingHandlers(EcorePackage.EPACKAGE, object);
		object.getEClassifiers().stream().forEach(c -> doSwitch(c));
		return null;
	}

	@Override
	public EObject caseEEnumLiteral(EEnumLiteral object) {
		// System.out.println(object.getName());
		return null;
	}

	@Override
	public EObject caseEDataType(EDataType object) {
		// System.out.println(object.getName());
		return null;
	}

	@Override
	public EObject caseEClass(EClass object) {
		// System.out.println(object.getName());
		object.getEStructuralFeatures().stream().forEach(f -> doSwitch(f));
		return null;
	}

	@Override
	public EObject caseEAttribute(EAttribute object) {
		callPreProcessingHandlers(EcorePackage.EATTRIBUTE, object);
		return null;
	}

	@Override
	public EObject caseEReference(EReference object) {
		callPreProcessingHandlers(EcorePackage.EREFERENCE, object);
		return null;
	}

	// util
	private void callPreProcessingHandlers(int id, EObject element) {
		// get registered participants
		Set<ConversionParticipant> prts = participants.get(id);
		if (prts != null) {
			for (ConversionParticipant prt : prts) {
				prt.handle(element, collections);
			}
		}
	}

	public void finished() {
		Set<Entry<Integer, Set<ConversionParticipant>>> enteries = participants.entrySet();
		for (Entry<Integer, Set<ConversionParticipant>> entry : enteries) {
			Set<ConversionParticipant> prts = entry.getValue();
			for (ConversionParticipant prt : prts) {
				prt.postProcess(collections);
			}
		}
	}

}

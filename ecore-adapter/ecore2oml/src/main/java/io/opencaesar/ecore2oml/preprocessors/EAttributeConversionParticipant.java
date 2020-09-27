package io.opencaesar.ecore2oml.preprocessors;

import static io.opencaesar.ecore2oml.Util.getMappedName;
import static io.opencaesar.ecore2oml.Util.isAnnotationSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.ScalarProperty;

public class EAttributeConversionParticipant extends ConversionParticipant {

	private Logger LOGGER = LogManager.getLogger(EAttributeConversionParticipant.class);

	public static class CollisionInfo {
		public Set<EAttribute> members = new HashSet<EAttribute>();
		public Set<EClassifier> types = new HashSet<EClassifier>();
		private EClassifier baseType = null;
		public ScalarProperty baseProperty = null;
		public Aspect baseConcept = null;
		private String name = "";

		public CollisionInfo(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}

		public boolean sameType() {
			return types.size() == 1;
		}

		public int size() {
			return members.size();
		}

		public void add(EAttribute attr) {
			members.add(attr);
			types.add(attr.getEType());
		}

		public void setName(String name) {
			this.name = name;
		}

		public void finalize() {
			// deal with type
			if (!sameType()) {
				for (EClassifier type : types) {
					if (baseType == null) {
						baseType = type;
					} 
				}
			}
		}

		

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(" Size = ");
			builder.append(size());
			builder.append(" - One Type = " + sameType() + " : ");
			types.forEach(e -> {
				builder.append(e.getName() + " - ");
			});
			builder.append("++ Memebrs : ");
			members.forEach(e -> {
				builder.append(e.getEContainingClass().getName() + " - ");
			});
			return builder.toString();
		}
	}

	private boolean shouldIngore(EAttribute eAttr) {
		if (eAttr.isDerived()) {
			return true;
		}
		if (isAnnotationSet(eAttr, AnnotationKind.ignore)) {
			return true;
		}
		if (isAnnotationSet(eAttr.getEAttributeType(), AnnotationKind.ignore)) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		// EAttribute
		EAttribute eAttr = (EAttribute) element;
		final String name = getMappedName(eAttr);
		if (shouldIngore(eAttr)) {
			return;
		}

		Map<String, CollisionInfo> names = (Map<String, CollisionInfo>) collections
				.get(CollectionKind.CollidingAttributes);
		if (names == null) {
			names = new HashMap<>();
			collections.put(CollectionKind.CollidingAttributes, names);
		}
		CollisionInfo info = names.get(name);
		if (info == null) {
			info = new CollisionInfo(name);
			names.put(name, info);
		}
		info.add(eAttr);
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		LOGGER.info("Post Processing");
		@SuppressWarnings("unchecked")
		Map<String, CollisionInfo> names = (Map<String, CollisionInfo>) collections
				.get(CollectionKind.CollidingAttributes);
		if (names == null) {
			return;
		}
		int before = names.size();
		Set<String> cleanMe = new HashSet<>();
		if (names != null) {
			Set<Entry<String, CollisionInfo>> entries = names.entrySet();
			for (Entry<String, CollisionInfo> entry : entries) {
				String name = entry.getKey();
				CollisionInfo info = entry.getValue();
				info.finalize();
				if (info.size() > 1) {
					LOGGER.debug(name + " ==> " + info);
				} else {
					cleanMe.add(name);
				}
			}

		}
		for (String name : cleanMe) {
			names.remove(name);
		}
		LOGGER.info("Size befoe = " + before + ", Size after = " + names.size());
	}

}

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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.RelationEntity;

public class EReferencConversionParticipant extends ConversionParticipant {

	private Logger LOGGER = LogManager.getLogger(EReferencConversionParticipant.class);

	public static class RefCollisionInfo {
		public Set<EReference> members = new HashSet<>();
		private String name = "";
		public Aspect fromAspect;
		public Aspect toAspect;
		public RelationEntity entity;
		public ForwardRelation forward;

		public RefCollisionInfo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int size() {
			return members.size();
		}

		public void add(EReference attr) {
			members.add(attr);
		}

		public void setName(String name) {
			this.name = name;
		}

		public void finalize() {
			// deal with type
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(" Size = ");
			builder.append(size());
			builder.append("++ Memebrs : ");
			members.forEach(e -> {
				builder.append(e.getEContainingClass().getName() + " - ");
			});
			return builder.toString();
		}
	}

	private static class TwoERefKey {

		public TwoERefKey(EReference forward, EReference reverse) {
			this.forward = forward;
			this.reverse = reverse;
		}

		public EReference forward;
		public EReference reverse;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((forward == null) ? 0 : forward.hashCode());
			result = prime * result + ((reverse == null) ? 0 : reverse.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TwoERefKey other = (TwoERefKey) obj;
			if (forward == null) {
				if (other.forward != null)
					return false;
			} else if (!forward.equals(other.forward))
				return false;
			if (reverse == null) {
				if (other.reverse != null)
					return false;
			} else if (!reverse.equals(other.reverse))
				return false;
			return true;
		}

	}

	public static class CollidingEOppositeData {
		private Map<TwoERefKey, Set<EReference>> forward = new HashMap<>();
		private Set<EReference> toSkip = new HashSet<>();

		public CollidingEOppositeData() {

		}
		
		public boolean shouldSkip(EReference eRef) {
			return toSkip.contains(eRef);
		}

		public void addForward(EReference eRef) {
			TwoERefKey key = new TwoERefKey(eRef, eRef.getEOpposite());
			Set<EReference> forwardSet = forward.get(key);
			if (forwardSet == null) {
				forwardSet = new HashSet<>();
				forward.put(key, forwardSet);
			}
			forwardSet.add(eRef);
		}

		public void addReverse(EReference eRef) {
			TwoERefKey key = new TwoERefKey(eRef.getEOpposite(), eRef);
			Set<EReference> forwardSet = forward.get(key);
			if (forwardSet != null) {
				forwardSet.add(eRef);
				toSkip.add(eRef);
			}
		}

		public void finalize() {
			// deal with type
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			return builder.toString();
		}

		public int size() {
			return forward.size();
		}
	}

	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		EReference eRef = (EReference) element;
		final String name = getMappedName(eRef);
		if (shouldIngore(eRef)) {
			return;
		}
		if (eRef.getEOpposite() == null) {
			_handleEReference(collections, eRef, name);
		} else {
			_handleEOppositeRefernces(collections, eRef, name);
		}
	}

	private void _handleEOppositeRefernces(Map<CollectionKind, Object> collections, EReference eRef, String name) {
		CollidingEOppositeData collInfo = (CollidingEOppositeData) collections
				.get(CollectionKind.CollidingEOppositeRefernces);
		if (collInfo == null) {
			collInfo = new CollidingEOppositeData();
			collections.put(CollectionKind.CollidingEOppositeRefernces, collInfo);
		}
		collInfo.addForward(eRef);
		collInfo.addReverse(eRef);
		LOGGER.debug(name);
	}

	private void _handleEReference(Map<CollectionKind, Object> collections, EReference eRef, final String name) {
		@SuppressWarnings("unchecked")
		Map<String, RefCollisionInfo> names = (Map<String, RefCollisionInfo>) collections
				.get(CollectionKind.CollidingRefernces);
		if (names == null) {
			names = new HashMap<>();
			collections.put(CollectionKind.CollidingRefernces, names);
		}
		RefCollisionInfo info = names.get(name);
		if (info == null) {
			info = new RefCollisionInfo(name);
			names.put(name, info);
		}
		info.add(eRef);
		LOGGER.debug(name);
	}

	private boolean shouldIngore(EReference eRef) {
		if (eRef.isDerived()) {
			return true;
		}
		if (isAnnotationSet(eRef, AnnotationKind.ignore) || isAnnotationSet(eRef, AnnotationKind.isRelationSource)
				|| isAnnotationSet(eRef, AnnotationKind.isRelationTarget)) {
			return true;
		}
		if (isAnnotationSet(eRef.getEReferenceType(), AnnotationKind.ignore)) {
			return true;
		}
		return false;
	}

	private void _postProcessEOpposite(Map<CollectionKind, Object> collections) {
		LOGGER.info("Post Processing : EOpposite");
		CollidingEOppositeData collInfo = (CollidingEOppositeData) collections
				.get(CollectionKind.CollidingEOppositeRefernces);
		if (collInfo == null) {
			return;
		}
		int before = collInfo.size();
		Set<TwoERefKey> cleanMe = new HashSet<>();
		Set<Entry<TwoERefKey, Set<EReference>>> enteries = collInfo.forward.entrySet();
		for (Entry<TwoERefKey, Set<EReference>> entry : enteries) {
			TwoERefKey key = entry.getKey();
			Set<EReference> val = entry.getValue();
			if (val.size() > 1) {
				LOGGER.info(getMappedName(key.forward) + " ==> " + val.size());
			} else {
				cleanMe.add(key);
			}
		}

		for (TwoERefKey key : cleanMe) {
			collInfo.forward.remove(key);
		}
		LOGGER.info("Size befoe = " + before + ", Size after = " + collInfo.size());

	}

	private void _postProcessEReference(Map<CollectionKind, Object> collections) {
		LOGGER.debug("Post Processing : EREFERNCE");
		@SuppressWarnings("unchecked")
		Map<String, RefCollisionInfo> names = (Map<String, RefCollisionInfo>) collections
				.get(CollectionKind.CollidingRefernces);
		if (names == null) {
			return;
		}
		int before = names.size();
		Set<String> cleanMe = new HashSet<>();
		if (names != null) {
			Set<Entry<String, RefCollisionInfo>> entries = names.entrySet();
			for (Entry<String, RefCollisionInfo> entry : entries) {
				String name = entry.getKey();
				RefCollisionInfo info = entry.getValue();
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
		LOGGER.debug("Size befoe = " + before + ", Size after = " + names.size());
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		_postProcessEReference(collections);
		_postProcessEOpposite(collections);
	}

}

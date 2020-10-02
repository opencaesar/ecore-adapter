package io.opencaesar.ecore2oml.preprocessors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EReference;

public class CollidingEOppositeData {
	Map<TwoERefKey, Set<EReference>> forward = new HashMap<>();
	private Set<EReference> toSkip = new HashSet<>();
	private Map<EReference,EReference> eRefToReplacement = new HashMap<EReference, EReference>();
	public CollidingEOppositeData() {

	}
	
	public boolean shouldSkip(EReference eRef) {
		return toSkip.contains(eRef);
	}

	public void addForward(EReference eRef) {
		TwoERefKey key = new TwoERefKey(eRef, eRef.getEOpposite());
		Set<EReference> forwardSet = forward.get(key);
		if (forwardSet == null) {
			forwardSet = new LinkedHashSet<>();
			forward.put(key, forwardSet);
		}
		forwardSet.add(eRef);
	}

	public void addReverse(EReference eRef) {
		TwoERefKey key = new TwoERefKey(eRef.getEOpposite(), eRef);
		Set<EReference> forwardSet = forward.get(key);
		if (forwardSet != null) {
			forwardSet.add(eRef);
		}
	}
	
	public EReference getMainRef(Set<EReference> refs) {
		EReference[] retVal = new EReference[1];
		retVal[0] = refs.stream().findFirst().get();
		refs.forEach(ref -> {
			if (!retVal[0].isContainer() && ref.isContainer()) {
				retVal[0] = ref;
			} else if (retVal[0].getUpperBound() > ref.getUpperBound()) {
				retVal[0] = ref;
			} else if (retVal[0].getName().compareTo(ref.getName()) > 0) {
				retVal[0] = ref;
			} else if (retVal[0].getEContainingClass().getName().compareTo(ref.getEContainingClass().getName()) > 0) {
				retVal[0] = ref;
			} else if (retVal[0].getEContainingClass().getEPackage().getNsURI()
					.compareTo(ref.getEContainingClass().getEPackage().getNsURI()) > 0) {
				retVal[0] = ref;
			}
		});
		return retVal[0];
	}

	public void finalize() {
		// deal with type
		// reflection the ERef Election Rules
		forward.entrySet().forEach(entry -> {
			Set<EReference> value = entry.getValue();
			if (value.size()>1) {
				EReference ref = getMainRef(value);
				value.remove(ref);
				assert value.size()<=1;
				value.forEach(replacedEef -> {
					eRefToReplacement.put(replacedEef, ref);
					toSkip.add(replacedEef);
				});
			}
		});
	}
	
	public EReference getMappedRef(EReference replacedRef) {
		return eRefToReplacement.get(replacedRef);
	}

	public int size() {
		return forward.size();
	}
}
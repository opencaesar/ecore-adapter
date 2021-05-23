package io.opencaesar.ecore2oml.preprocessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.util.Util;

public class ERefGroups {
	AtomicInteger ID = new AtomicInteger();
	private Set<Group> groups = new HashSet<>();
	private static final String SUBSETS = "subsets";
	private Map<EReference, Group> refToGroup = new HashMap<>();
	private Set<EReference> toSkip = new HashSet<>();
	private Graph graph = new Graph();
	private Map<EReference, Set<EReference>> memory = new HashMap<>();
	private class Graph {
		List<Node> nodes = new ArrayList<>();
		Map<EReference, Node> refToNode = new HashMap<>();
		
		public void addRef(EReference ref, EReference superRef) {
			Node superNode = getOrCreateNode(superRef);
			Node node = getOrCreateNode(ref);
			superNode.addSub(node);
		}

		private Node getOrCreateNode(EReference ref) {
			Node node = refToNode.get(ref);
			if (node==null) {
				node = new Node(ref);
				nodes.add(node);
				refToNode.put(ref, node);
			}
			return node;
		}

		public void buildGroups() {
			List<Node> roots = new ArrayList<>();
			for (Node node : nodes) {
				if (node.getParents().isEmpty()) {
					roots.add(node);
				}
			}
			
			for (Node node : roots) {
				if (!node.isVisited()) {
					List<Node> reachables = new ArrayList<>();
					collectReachables(node,reachables); 
					Group g = findGroup(reachables);
					if (g==null) {
						g = new Group(ID.addAndGet(1));
						groups.add(g);
					}
					g.addAll(reachables);
				}
			}
		}

		private Group findGroup(List<Node> reachables) {
			for (Node node : reachables) {
				Group g = getERefGroup(node.ref);
				if (g==null) {
					g = getERefGroup(node.ref.getEOpposite());
				}
				if (g!=null) {
					return g;
				}
			}
			return null;
		}

		private void collectReachables(Node node, List<Node> reachables) {
			if (!node.isVisited()) {
				reachables.add(node);
				node.setVisited();
				for (Node sub : node.getSubs()) {
					collectReachables(sub, reachables);
				}
				for (Node parents : node.getParents()) {
					collectReachables(parents, reachables);
				}
			}
		}
	}
	
	private class Node {
		private EReference ref;
		private Set<Node> subs = new HashSet<>();
		private Set<Node> parents = new HashSet<>();
		boolean visited = false;
		
		public Node(EReference ref) {
			this.ref = ref;
		}
		
		public void addSub(Node ref) {
			subs.add(ref);
			ref.addParent(this);
		}
		
		private void addParent(Node node) {
			parents.add(node);
		}

		public boolean isVisited() {
			return visited;
		}
		
		public void setVisited() {
			visited = true;
		}
		
		public Set<Node> getSubs() {
			return subs;
		}
		
		public Set<Node> getParents() {
			return parents;
		}
		
		@Override
		public String toString() {
			return getRefLabel(ref);
		}
	}
	
	
	
	private class Group {
		private int id;
		private Set<EReference> side = new HashSet<>();
		private Set<EReference> otherSide = new HashSet<>();
		private double side1Weight = 0 ;
		private double side2Weight = 0 ;
		
		public Group(int id) {
			this.id = id;
		}
		
		public void addAll(List<Node> nodes) {
			List<EReference> otherRefs = new ArrayList<>();
			Set<EReference> whereToAdd = null;
			Set<EReference> whereToAddOthers = null;
			for (Node node : nodes) {
				EReference other = node.ref.getEOpposite();
				if (whereToAdd==null) {
					if (side.contains(node.ref) ||
						(other!=null && otherSide.contains(other))) {
						whereToAdd = side;
						whereToAddOthers = otherSide;
					}else if (otherSide.contains(node.ref) || 
							(other!=null && side.contains(other))) {
						whereToAdd = otherSide;
						whereToAddOthers = side;
					}
					if (other!=null) otherRefs.add(other);
				}
			}
			if (whereToAdd==null) {
				whereToAdd = side;
				whereToAddOthers = otherSide;
			}
			for (Node node : nodes){
				whereToAdd.add(node.ref);
				refToGroup.put(node.ref,this);
			}
			whereToAddOthers.addAll(otherRefs);
			map(otherRefs);
		}

		private void map(List<EReference> refs) {
			for (EReference ref : refs) {
				refToGroup.put(ref,this);
			}
		}
		
		private double calcWeight(EReference ref) {
			if (ref.getEOpposite()==null) {
				return 1;
			}
			if (ref.isContainer() != ref.getEOpposite().isContainer()) {
				if (ref.getEOpposite().isContainer()) {
					return 0.0;
				}
				return 0.5;
			} else if (ref.getUpperBound() != ref.getEOpposite().getUpperBound()) {
				if (ref.getEOpposite().getUpperBound() > 0
						&& (ref.getUpperBound() < 0 || ref.getUpperBound() > ref.getEOpposite().getUpperBound())) {
					return 0.0;
				}
				return 0.4;
			}else if (!ref.getName().equals(ref.getEOpposite().getName())) {
				if (ref.getName().compareTo(ref.getEOpposite().getName()) > 0) {
					return 0.0;
				}
				return 0.2;
			} else if (!ref.getEContainingClass().getName().equals(ref.getEOpposite().getEContainingClass().getName())) {
				if (ref.getEContainingClass().getName()
						.compareTo(ref.getEOpposite().getEContainingClass().getName()) > 0) {
					return 0.0;
				}
				return 0.1;
			} else if (ref.getEContainingClass().getEPackage().getNsURI()
					.compareTo(ref.getEOpposite().getEContainingClass().getEPackage().getNsURI()) <= 0) {
				return 0.05;
			}
			return 0;
		}
		
		private double calcCollectionWeight(Set<EReference> refs) {
			float weight = 0 ;
			for (EReference ref : refs) {
				weight += calcWeight(ref);
			}
			return weight;
		}		

		public void finish() {
			side1Weight = calcCollectionWeight(side);
			side2Weight = calcCollectionWeight(otherSide);
			if (side1Weight>side2Weight) {
				filterCollection(otherSide);
			}else {
				filterCollection(side);
			}
			
		}

		private void filterCollection(Set<EReference> refs) {
			for (EReference ref : refs ) {
				toSkip.add(ref);
			}
		}
		
	}
	
	public ERefGroups() {
		ID.set(0);
	}
	
	private String getRefLabel(EReference ref) {
		return  ref.getEContainingClass().getName() + "_" + ref.getName();
	}
	
	public void addERef(EReference ref) {
		if (getRefLabel(ref).contains("substitution")) {
			System.out.println("Bad");
		}
		// if it is handled already then skip
		if (ref.getEOpposite()!=null) {
			addRefToGraph(ref);
			addRefToGraph(ref.getEOpposite());
		}
	}


	private void addRefToGraph(EReference ref) {
		if (ref!=null) {
			Set<EReference> subSets = getSubSets(ref);
			pushTograph(ref,subSets);
		}
	}
	


	private void pushTograph(EReference ref, Collection<EReference> subs) {
		if (subs.isEmpty()) {
			graph.getOrCreateNode(ref);
			graph.getOrCreateNode(ref.getEOpposite());
		}
		for (EReference sub : subs) {
			graph.addRef(ref, sub);
		}		
	}

	private Set<EReference> getSubSets(EReference ref) {
		// this can be memoized
		if (memory.containsKey(ref)) {
			return memory.get(ref);
		}
		EAnnotation subsets = Util.getAnnotation(ref, SUBSETS);
		if (subsets!=null) {
			Set<EReference> ret = new HashSet<>();
			EList<EObject> refs = subsets.getReferences();
			for (EObject sub : refs) {
				EReference superRef = (EReference)sub;
				ret.add(superRef);
				ret.addAll(getSubSets(superRef));
			}
			memory.put(ref, ret);
			return ret;
		}
		memory.put(ref, Collections.emptySet());
		return Collections.emptySet();
	}
	
	private Group getERefGroup(EReference ref) {
		return refToGroup.get(ref);
	}
	
	public void finish() {
		graph.buildGroups();
		
		groups = groups.stream().filter(g-> g.side.size() >=1 && g.otherSide.size() >= 1).collect(Collectors.toSet());
		for (Group group : groups) {
			group.finish();
		}
		System.out.println(this);
	}
	
	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		for (Group g : groups) {
			bldr.append("Group: " + g.id + "\n");
			bldr.append("\tSide1: " + "(Weight: " + g.side1Weight + ")\n");
			for (EReference ref : g.side) {
				bldr.append("\t\t" + ref.getEContainingClass().getName() + ":" + ref.getName() + "\n");
			}
			bldr.append("\tSide2: " + "(Weight: " + g.side2Weight + ")\n");
			for (EReference ref : g.otherSide) {
				bldr.append("\t\t" + ref.getEContainingClass().getName() + ":" + ref.getName()+ "\n");
			}
		}
		return bldr.toString();
	}


	public boolean shouldSkip(EReference object) {
		return toSkip.contains(object);
	}

}

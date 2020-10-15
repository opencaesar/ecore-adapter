package io.opencaesar.ecore2oml.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;

import io.opencaesar.ecore2oml.preprocessors.RelationshipInfo;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public class RelationshipUtil {
	
	static private Logger LOGGER = LogManager.getLogger(RelationshipUtil.class);
	private Map<String,RelationshipInfo> relationShips = new HashMap<>();
	static private RelationshipUtil _instance = new RelationshipUtil();
	
	static public RelationshipUtil getInstance() {
		return _instance;
	}
	
	static public void init(String filePath) {
		synchronized (RelationshipUtil.class) {
			try {
				List<String> allLines = Files.readAllLines(Paths.get(filePath));
				for (String line : allLines) {
					String[] strings = line.split(" ");
					if (strings.length >= 2) {
						LOGGER.debug("Classifier:" + strings[0]);
						LOGGER.debug("Source:" + strings[1]);
						if (strings.length==3) LOGGER.debug("Target:" + strings[2]);
						_instance.addRelationship(strings[0], strings[1], strings.length==3 ? strings[2] : "");
					}
				}
			} catch (IOException e) {
				LOGGER.error(e.getLocalizedMessage());
			}
		}
	}
	
	public boolean isRelationship(EClass toCheck,OmlWriter oml, Vocabulary voc) {
		String[] matched = new String[1];
		RelationshipInfo[] matchedInfo = new RelationshipInfo[1];
		boolean bRetVal = _isRelationship(toCheck, oml, voc, matched,matchedInfo);
		if (bRetVal) {
			String iri = Util.getIri(toCheck,voc,oml);
			if (!relationShips.containsKey(iri)) {
				relationShips.put(iri, new RelationshipInfo(matchedInfo[0].getSourceIRI(), matchedInfo[0].getTargetIRI()));
			}
		}
		return bRetVal;
	}
	
	public boolean _isRelationship(EClass toCheck,OmlWriter oml, Vocabulary voc,String[] matchedIRI, RelationshipInfo[] matchedInfo) {
		boolean bRetVal = false;
		String iri = Util.getIri(toCheck,voc,oml);
		if (relationShips.containsKey(iri)) {
			matchedIRI[0] =  iri;
			matchedInfo[0] = relationShips.get(iri);
			return true;
		}
		EList<EClass> superTypes = toCheck.getEAllSuperTypes();
		for (EClass superType : superTypes) {
			bRetVal |= _isRelationship(superType, oml, voc,matchedIRI,matchedInfo);
			if (bRetVal) {
				return true;
			}
		}
		return false;
	}

	public void addRelationship(String classifierIRI, String sourceIRI, String targetIRI) {
		assert relationShips.containsKey(classifierIRI)==false : "Already added:" + classifierIRI;
		relationShips.put(classifierIRI, new RelationshipInfo(sourceIRI, targetIRI));
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, RelationshipInfo> entry : relationShips.entrySet()) {
			builder.append("Relationship: " + entry.getKey() + " => " + entry.getValue() + "\n");
		}
		return builder.toString();
	}

	public RelationshipInfo getInfo(EClass eContainingClass, OmlWriter oml, Vocabulary vocabulary) {
		return relationShips.get( Util.getIri(eContainingClass,vocabulary,oml));
	}
	

}

package io.opencaesar.ecore2oml.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

// TODO: check if using Ecore URI Converter is a better Choice
public class URIMapper {
	
	static private Logger LOGGER = LogManager.getLogger(URIMapper.class);
	private Map<String,String> mappedIRIs = new HashMap<>();
	static private URIMapper _instance = new URIMapper();
	
	static public URIMapper getInstance() {
		return _instance;
	}
	
	static public void init(String filePath) {
		synchronized (URIMapper.class) {
			try {
				List<String> allLines = Files.readAllLines(Paths.get(filePath));
				for (String line : allLines) {
					String[] strings = line.split("=");
					if (strings.length == 2) {
						LOGGER.debug("Key:" + strings[0]);
						LOGGER.debug("Val:" + strings[1]);
						_instance.mappedIRIs.put(strings[0].trim(), strings[1].trim());
					}
				}
			} catch (IOException e) {
				LOGGER.error(e.getLocalizedMessage());
			}
		}
	}
	
	public String getMappedIRI(String original) {
		String newIRI = mappedIRIs.get(original);
		return newIRI!=null? newIRI:original;
	}

}

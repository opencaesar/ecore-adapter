/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.ecore2oml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharStreams;
import com.google.inject.Injector;

import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlBuilder;

public class Ecore2OmlApp {

	static final String OML_EXTENSION = "oml";

	@Parameter(
		names= {"--input-folder-path","-i"}, 
		description="Location of input folder of Ecore files (Required)",
		validateWith=InputFolderPath.class, 
		required=true, 
		order=1)
	private String inputFolderPath = null;

	@Parameter(
		names= {"--output-catalog-path", "-o"}, 
		description="Location of the output OML catalog XML file (Required)", 
		validateWith=CatalogPath.class, 
		required=true, 
		order=2
	)
	private String outputCatalogPath;
	
	@Parameter(
		names= {"--options", "-op"}, 
		description="Location of the options (.json) file (Optional)",  
		order=3
	)
	private String optionsPath;
	
	@Parameter(
		names= {"--debug", "-d"}, 
		description="Shows debug logging statements", 
		order=4
	)
	private boolean debug;

	@Parameter(
		names= {"--help","-h"}, 
		description="Displays summary of options", 
		help=true, 
		order=5) 
	private boolean help;

	private Logger LOGGER = LogManager.getLogger(Ecore2OmlApp.class);

	/*
	 * Main method
	 */
	public static void main(String ... args) throws IOException {
		final Ecore2OmlApp app = new Ecore2OmlApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton)appender).setThreshold(Level.DEBUG);
		}
		if (app.inputFolderPath.endsWith(File.separator)) {
			app.inputFolderPath = app.inputFolderPath.substring(0, app.inputFolderPath.length()-1);
		}
		app.run();
	}

	/*
	 * Run method
	 */
	private void run() throws IOException {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                      Ecore to Oml "+getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Input Folder Path= " + inputFolderPath);
		LOGGER.info("Output Catalog Path= " + outputCatalogPath);
		LOGGER.info("Options Path= " + optionsPath);
		
		final File inputFolder = new File(inputFolderPath);
		final Collection<File> inputFiles = collectEcoreFiles(inputFolder);
		
		final Injector injector = new XcoreStandaloneSetup().createInjectorAndDoEMFRegistration();
		final XtextResourceSet inputResourceSet = injector.getInstance(XtextResourceSet.class);

		// load the input models and resolve their references
		for (File inputFile : inputFiles) {
			final URI inputURI = URI.createFileURI(inputFile.getAbsolutePath());
			LOGGER.info("Reading: "+inputURI);
			final Resource inputResource = inputResourceSet.getResource(inputURI, true);
			if (inputResource != null) {
				EcoreUtil.resolveAll(inputResource);
			}
		}

		// load the Oml registries here after the input have been read
		OmlStandaloneSetup.doSetup();
		final XtextResourceSet outputResourceSet = new XtextResourceSet();

		final URL catalogURL = new File(outputCatalogPath).toURI().toURL();
		final OmlCatalog catalog = OmlCatalog.create(catalogURL);
		ConversionContext conversionContext = new ConversionContext();		
		if (optionsPath!=null) {
			conversionContext.setOptions(optionsPath);
		}
		

		// create the Oml builder
		final OmlBuilder builder = new OmlBuilder(outputResourceSet);
		
		// start the Oml Builder
		builder.start();

		Map<String,EPackage> dependency = new HashMap<>();
		Set<String> handled = new HashSet<>();
		// create the new resources
		final List<URI> outputResourceURIs = new ArrayList<URI>();
		for (File inputFile : inputFiles) {
			final URI inputURI = URI.createFileURI(inputFile.getAbsolutePath());
			final Resource inputResource = inputResourceSet.getResource(inputURI, true);
			if (inputResource != null) {
				final TreeIterator<EObject> i = inputResource.getAllContents();
				while (i.hasNext()) {
					EObject content = i.next();
					if (content instanceof EPackage) {
						EPackage ePackage = (EPackage) content;
						final String relativePath = catalog.resolveURI(ePackage.getNsURI())+"."+OML_EXTENSION;
						final URI outputResourceURI = URI.createURI(relativePath);
						LOGGER.info("Creating: "+outputResourceURI);
						Ecore2Oml e2o = new Ecore2Oml(ePackage, outputResourceURI, builder,conversionContext);
						e2o.run();
						dependency.putAll(e2o.getDependencies());
						outputResourceURIs.add (outputResourceURI);
						String iri = Util.getIri(ePackage, conversionContext);
						handled.add(iri);
					}
				}
			}
		}
		
		handled.forEach(d -> {
			dependency.remove(d);
		});
		
		while (!dependency.isEmpty()) {
			for (Entry<String, EPackage> iri: dependency.entrySet()) {
				String ecoreRelativePath =  catalog.resolveURI(iri.getKey()) +"."+OML_EXTENSION;
				URI ecoreResourceURI = URI.createURI(ecoreRelativePath);
				Ecore2Oml e2o = new Ecore2Oml(iri.getValue(), ecoreResourceURI, builder,conversionContext);				
				e2o.run();
				dependency.putAll(e2o.getDependencies());
				outputResourceURIs.add (ecoreResourceURI);
				handled.add(iri.getKey());
			}
			
			handled.forEach(d -> {
				dependency.remove(d);
			});
		}
		
		// finish the Oml builder
		builder.finish();
		
		// save the output resources here instead of calling builder.save in order to log
		for (URI outputResourceURI : outputResourceURIs) {
			if (outputResourceURI.fileExtension().equals("oml")) {
				LOGGER.info("Saving: "+outputResourceURI);
				final Resource outputResource = outputResourceSet.getResource(outputResourceURI, false);
				outputResource.save(Collections.EMPTY_MAP);
			}
		}

		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

	// Utility methods
	
	private Collection<File> collectEcoreFiles(File directory) {
		final List<File> omlFiles = new ArrayList<File>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				final String ext = getFileExtension(file);
				if (ext.equals("ecore") || ext.equals("xcore")) {
					omlFiles.add(file);
				}
			} else if (file.isDirectory()) {
				omlFiles.addAll(collectEcoreFiles(file));
			}
		}
		return omlFiles;
	}

	private String getFileExtension(File file) {
        final String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
    }

	/**
	 * Get application version id from properties file.
	 * @return version string from build.properties or UNKNOWN
	 */
	private String getAppVersion() {
		String version = "UNKNOWN";
		try {
			final InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt");
			final InputStreamReader reader = new InputStreamReader(input);
			version = CharStreams.toString(reader);
		} catch (IOException e) {
			final String errorMsg = "Could not read version.txt file." + e;
			LOGGER.error(errorMsg, e);
		}
		return version;
	}

	static public class InputFolderPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File directory = new File(value);
			if (!directory.isDirectory()) {
				throw new ParameterException("Parameter " + name + " should be a valid folder path");
			}
	  	}
	}

	static public class CatalogPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OML catalog path");
			}
		}
		
	}

}

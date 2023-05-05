package io.opencaesar.ecore2oml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import io.opencaesar.oml.util.OmlConstants;

/**
 * A gradle task to invoke the ecore to oml tool 
 */
public abstract class Ecore2OmlTask extends DefaultTask {

	/**
	 * Creates a new Ecore2OmlTask object
	 */
	public Ecore2OmlTask() {
	}

	/**
	 * Path of input folder of Ecore files
	 * 
	 * @return File Property
	 */
	@Input
    public abstract Property<File> getInputFolderPath();

	/**
	 * Path of output OML catalog
	 * 
	 * @return String Property
	 */
	@Input
    public abstract Property<File> getOutputCatalogPath();

	/**
	 * The debug flag
	 * 
	 * @return Boolean Property
	 */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();
    
	/**
	 * The collection of input Ecore files
	 * 
     * @return ConfigurableFileCollection
	 */
	@InputFiles
    protected ConfigurableFileCollection getInputFiles() {
    	try {
    		File f = getInputFolderPath().get();
    		var files = f.exists() ? collectEcoreFiles(f) : Collections.emptyList();
    		return getProject().files(files);
    	} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
    	}
    }

	/**
	 * The collection of output OML files referenced by the output Oml catalog
	 * 
     * @return ConfigurableFileCollection
	 */
	@OutputFiles
	protected ConfigurableFileCollection getOutputFiles() {
    	try {
    		return getProject().files(collectOmlFiles(getOutputCatalogPath().get().getParentFile()));
    	} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
    	}
    }

   /**
    * The gradle task action logic.
    */
	@TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (getInputFolderPath().isPresent()) {
		    args.add("-i");
		    args.add(getInputFolderPath().get().getAbsolutePath());
        }
        if (getOutputCatalogPath().isPresent()) {
		    args.add("-o");
		    args.add(getOutputCatalogPath().get().getAbsolutePath());
        }
		if (getDebug().isPresent() && getDebug().get()) {
		    args.add("-d");
	    }
	    try {
	    	Ecore2OmlApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
    
	private static List<File> collectOmlFiles(File path) {
		final List<File> files;
		if (path.isDirectory()) {
			files = Arrays.asList(path.listFiles());
		} else {
			files = Collections.singletonList(path);
		}
		final List<File> omlFiles = new ArrayList<>();
		for (File file : files) {
			if (file.isDirectory()) {
				omlFiles.addAll(collectOmlFiles(file));
			} else if (file.isFile()) {
				String ext = getFileExtension(file);
				if (OmlConstants.OML_EXTENSION.equals(ext)) {
					omlFiles.add(file);
				}
			} else { // must be a file name with no extension
				File f = new File(path.toString()+'.'+OmlConstants.OML_EXTENSION);
				if (f.exists()) {
					omlFiles.add(f);
				}
			}
		}
		return omlFiles;
	}

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

	private static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
    }

}
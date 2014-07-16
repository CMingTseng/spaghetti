package com.prezi.spaghetti.gradle;

import com.prezi.spaghetti.config.ModuleConfiguration;
import org.apache.commons.io.FileUtils;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class GenerateHeaders extends AbstractDefinitionAwareSpaghettiTask {
	private File outputDirectory;

	@OutputDirectory
	public File getOutputDirectory() {
		return outputDirectory;
	}

	public void setOutputDirectory(Object outputDirectory) {
		this.outputDirectory = getProject().file(outputDirectory);
	}

	public void outputDirectory(Object directory) {
		setOutputDirectory(directory);
	}

	public GenerateHeaders() {
		this.getConventionMapping().map("outputDirectory", new Callable<File>() {
			@Override
			public File call() throws Exception {
				return new File(getProject().getBuildDir(), "spaghetti/generated-headers");
			}

		});
	}

	@TaskAction
	public void generate() throws IOException {
		ModuleConfiguration config = readConfig(getDefinitions());
		getLogger().info("Generating module headers for {}", config.getLocalModules());
		File directory = getOutputDirectory();
		FileUtils.forceDelete(directory);
		FileUtils.forceMkdir(directory);
		createGenerator(config).generateHeaders(directory);
	}
}
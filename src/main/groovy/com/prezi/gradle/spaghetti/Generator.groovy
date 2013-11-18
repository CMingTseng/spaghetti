package com.prezi.gradle.spaghetti

import org.gradle.api.Project

/**
 * Created by lptr on 12/11/13.
 */
public interface Generator {
	void initialize(Project project)
	String getPlatform();
	void generateInterfaces(ModuleConfiguration config, ModuleDefinition module, File outputDirectory);
	void generateClientModule(ModuleConfiguration config, File outputDirectory);
	String processModuleJavaScript(ModuleConfiguration config, ModuleDefinition module, String javaScript)
}

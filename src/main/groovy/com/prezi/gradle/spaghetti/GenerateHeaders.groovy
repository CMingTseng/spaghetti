package com.prezi.gradle.spaghetti

/**
 * Created by lptr on 12/11/13.
 */
class GenerateHeaders extends AbstractGenerateTask {

	@Override
	protected void generateInternal(Generator generator, ModuleDefinition moduleDef)
	{
		generator.generateInterfaces(moduleDef, outputDirectory)
	}
}

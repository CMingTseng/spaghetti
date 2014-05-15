package com.prezi.spaghetti.gradle

import com.prezi.spaghetti.ModubleBundleParameters
import com.prezi.spaghetti.ModuleBundle
import com.prezi.spaghetti.ModuleConfiguration
import com.prezi.spaghetti.ModuleDefinition
import com.prezi.spaghetti.Wrapper
import com.prezi.spaghetti.Wrapping
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Created by lptr on 19/04/14.
 */
class AbstractBundleModuleTask extends AbstractDefinitionAwareSpaghettiTask {

	@InputFile
	File inputFile

	def inputFile(Object f) {
		this.inputFile = project.file(f)
	}

	@OutputFile
	File outputFile

	def outputFile(Object f) {
		this.outputFile = project.file(f)
	}

	@Input
	@Optional
	String sourceBaseUrl
	void sourceBaseUrl(String source) {
		this.sourceBaseUrl = source
	}

	File sourceMap

	void sourceMap(Object sourceMap) {
		this.sourceMap = project.file(sourceMap)
	}

	@InputFile
	@Optional
	File getSourceMap() {
		if (!sourceMap) {
			// This should probably be done with convention mapping
			def defSourceMap = new File(getInputFile().toString() + ".map")
			if (defSourceMap.exists()) {
				sourceMap = defSourceMap
			}
		}
		return sourceMap
	}

	// @InputDirectories not yet supported (only @InputDirectory)
	// http://issues.gradle.org/browse/GRADLE-3051
	// This probably won't work with convention mapping
	Set<File> resourceDirs = []
	void setResourceDirs(Set<File> resourceDirs) {
		resourceDirs.each {
			inputs.dir it
		}
		this.resourceDirs = new LinkedHashSet<>(resourceDirs)
	}

	void resourceDir(File resourceDirectory) {
		inputs.dir resourceDirectory
		this.resourceDirs.add resourceDirectory
	}

	File workDir
	void workDir(String workDir) {
		this.workDir = project.file(workDir)
	}

	AbstractBundleModuleTask() {
		this.conventionMapping.inputFile = { new File(project.buildDir, "module.js") }
	}

	@TaskAction
	final bundle() {
		def moduleDefinitions = getDefinitions()
		if (moduleDefinitions.empty) {
			throw new IllegalArgumentException("No module definition present")
		}
		if (moduleDefinitions.files.size() > 1) {
			throw new IllegalArgumentException("Too many module definitions present: ${moduleDefinitions}")
		}
		def config = readConfig(moduleDefinitions)
		def module = config.getLocalModules().first()
		def processedJavaScript = createGenerator(config).processModuleJavaScript(module, getInputFile().text)
		def wrappedJavaScript = Wrapper.wrap(config.dependentModules*.name, Wrapping.module, processedJavaScript)

		// is a sourcemap present?
		def sourceMapText = getSourceMap()?.text

		def workDir = getWorkDir()
		workDir.delete() || workDir.deleteDir()
		workDir.mkdirs()
		new File(workDir, "module.js") << wrappedJavaScript

		def existingResourceDirs = getResourceDirs().findAll { it.exists() }
		createBundle(config, module, wrappedJavaScript, sourceMapText, existingResourceDirs)
	}

	protected ModuleBundle createBundle(
			ModuleConfiguration config,
			ModuleDefinition module,
			String javaScript,
			String sourceMap,
			Set<File> resourceDirs) {
		ModuleBundle.createZip(
				getOutputFile(),
				new ModubleBundleParameters(
						name: module.name,
						definition: module.definitionSource,
						version: String.valueOf(project.version),
						sourceBaseUrl: getSourceBaseUrl(),
						bundledJavaScript: javaScript,
						sourceMap: sourceMap,
						resourceDirs: resourceDirs
				)
		)
	}
}

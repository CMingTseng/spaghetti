package com.prezi.spaghetti.typescript

import com.prezi.spaghetti.ast.InterfaceNode
import com.prezi.spaghetti.ast.ModuleNode
import com.prezi.spaghetti.definition.ModuleConfiguration
import com.prezi.spaghetti.generator.AbstractGenerator
import com.prezi.spaghetti.generator.GeneratorParameters
import com.prezi.spaghetti.typescript.access.TypeScriptModuleAccessorGeneratorVisitor
import com.prezi.spaghetti.typescript.impl.TypeScriptModuleInitializerGeneratorVisitor
import com.prezi.spaghetti.typescript.impl.TypeScriptModuleProxyGeneratorVisitor
import com.prezi.spaghetti.typescript.stub.TypeScriptInterfaceStubGeneratorVisitor

import static com.prezi.spaghetti.generator.ReservedWords.SPAGHETTI_CLASS

class TypeScriptGenerator extends AbstractGenerator {

	public static final String CREATE_MODULE_FUNCTION = "__createSpaghettiModule"

	private final String header
	private final ModuleConfiguration config

	TypeScriptGenerator(GeneratorParameters params) {
		super(params)
		this.header = params.header
		this.config = params.moduleConfiguration
	}

	@Override
	void generateHeaders(File outputDirectory) {
		copySpaghettiClass(outputDirectory)
		generateLocalModule(config.localModule, outputDirectory, header)
		config.allDependentModules.each { dependentModule ->
			generateDependentModule(dependentModule, outputDirectory, header)
		}
	}

	@Override
	void generateStubs(File outputDirectory) throws IOException {
		config.allModules.each { module ->
			def contents = ""
			for (type in module.types) {
				if (type instanceof InterfaceNode) {
					contents += new TypeScriptInterfaceStubGeneratorVisitor().visit(type)
				}
			}
			TypeScriptUtils.createSourceFile(header, module, module.alias + "Stubs", outputDirectory, contents)
		}
	}

	@Override
	protected String processModuleJavaScriptInternal(ModuleNode module, String javaScript)
	{
"""${javaScript}
return ${module.name}.${CREATE_MODULE_FUNCTION}(${SPAGHETTI_CLASS});
"""
	}

	/**
	 * Copies Spaghetti.hx to the generated source directory.
	 */
	private static void copySpaghettiClass(File outputDirectory) {
		new File(outputDirectory, "${SPAGHETTI_CLASS}.ts") << TypeScriptGenerator.class.getResourceAsStream("/${SPAGHETTI_CLASS}.ts")
	}

	/**
	 * Generates local module.
	 */
	private static void generateLocalModule(ModuleNode module, File outputDirectory, String header)
	{
		def contents = ""
		contents += new TypeScriptDefinitionIteratorVisitor().visit(module)
		contents += new TypeScriptModuleProxyGeneratorVisitor(module).visit(module)
		contents += new TypeScriptModuleInitializerGeneratorVisitor().visit(module)
		TypeScriptUtils.createSourceFile(header, module, module.alias, outputDirectory, contents)
	}

	private static void generateDependentModule(ModuleNode module, File outputDirectory, String header) {
		def contents = "declare var ${SPAGHETTI_CLASS}:any;\n"
		contents += new TypeScriptModuleAccessorGeneratorVisitor(module).visit(module)
		contents += new TypeScriptDefinitionIteratorVisitor().visit(module)
		TypeScriptUtils.createSourceFile(header, module, module.alias, outputDirectory, contents)
	}
}

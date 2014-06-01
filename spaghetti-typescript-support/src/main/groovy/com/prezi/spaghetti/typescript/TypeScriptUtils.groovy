package com.prezi.spaghetti.typescript

import com.prezi.spaghetti.ast.ModuleNode

final class TypeScriptUtils {
	public static File createSourceFile(ModuleNode module, String name, File outputDirectory, String contents) {
		def namespace = module.name
		def file = new File(outputDirectory, name + ".ts")
		file.delete()
		file << "/*\n"
		file << " * Generated by Spaghetti.\n"
		file << " */\n"
		if (namespace)
		{
			file << "module ${namespace} {\n"
			file << contents
			file << "}"
		}
		else {
			file << contents
		}
		return file
	}
}

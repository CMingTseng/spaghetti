package com.prezi.spaghetti.packaging.internal;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.prezi.spaghetti.internal.Version;
import com.prezi.spaghetti.packaging.ModuleWrapper;
import com.prezi.spaghetti.packaging.ModuleWrapperParameters;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.prezi.spaghetti.generator.ReservedWords.DEPENDENCIES;
import static com.prezi.spaghetti.generator.ReservedWords.GET_MODULE_NAME;
import static com.prezi.spaghetti.generator.ReservedWords.GET_MODULE_VERSION;
import static com.prezi.spaghetti.generator.ReservedWords.GET_RESOURCE_URL;
import static com.prezi.spaghetti.generator.ReservedWords.GET_SPAGHETTI_VERSION;
import static com.prezi.spaghetti.packaging.internal.CommentUtils.appendAfterInitialComment;

public abstract class AbstractModuleWrapper implements ModuleWrapper {
	protected void wrapModuleObject(StringBuilder builder, ModuleWrapperParameters params, Iterable<String> dependencies, Collection<String> externalDependencies, boolean wrapExportedModule) throws IOException {
		int ix = externalDependencies == null ? 0 : externalDependencies.size();
		List<String> externalDependencyLines = ExternalDependencyGenerator.generateExternalDependencyLines(externalDependencies);
		List<String> dependencyLines = new LinkedList<String>();
		for (String dep : dependencies) {
			dependencyLines.add(String.format("\"%s\":dependencies[%d]", dep, ix++));
		}

		String moduleName = params.name;

		List<String> importedExternalDependencyVars = ExternalDependencyGenerator.getImportedVarNames(externalDependencies);

		builder.append("function(){");
		builder.append(Joiner.on("").join(externalDependencyLines));
		builder.append("var module=(function(dependencies){");
			builder.append("return function(init){");
				builder.append("return init.call({},(function(){"); // this will be the Spaghetti object in the module
					builder.append("return{");
						builder.append(GET_SPAGHETTI_VERSION).append(":function(){");
							builder.append("return \"").append(Version.SPAGHETTI_VERSION).append("\";");
						builder.append("},");
						builder.append(GET_MODULE_NAME).append(":function(){");
							builder.append("return \"").append(moduleName).append("\";");
						builder.append("},");
						builder.append(GET_MODULE_VERSION).append(":function(){");
							builder.append("return \"").append(params.version).append("\";");
						builder.append("},");
						builder.append(GET_RESOURCE_URL).append(":function(resource){");
							builder.append("if(resource.substr(0,1)!=\"/\"){");
								builder.append("resource=\"/\"+resource;");
							builder.append("}");
							builder.append("return baseUrl+resource;");
						builder.append("},");
						builder.append("\"").append(DEPENDENCIES).append("\":{");
							builder.append(Joiner.on(',').join(dependencyLines));
						builder.append("}");
					builder.append("};");
				builder.append("})()"); // end of Spaghetti object
				if (importedExternalDependencyVars.size() > 0) {
					builder.append(",").append(Joiner.on(",").join(importedExternalDependencyVars));
				}
				builder.append(");");
			builder.append("};");
		builder.append("})(arguments);");
		if (wrapExportedModule) {
			appendAfterInitialComment(builder, "return{\"module\":(function(){return ", params.javaScript);
			builder.append("\n})(),");
			builder.append("\"version\":\"").append(params.version).append("\",");
			builder.append("\"spaghettiVersion\":\"").append(Version.SPAGHETTI_VERSION).append("\"");
			builder.append("};");
		} else {
			appendAfterInitialComment(builder, "var moduleImpl=(function(){return ", params.javaScript);
			builder.append("\n})() || {};\nmoduleImpl[\"module\"]=moduleImpl;\nreturn moduleImpl;");
		}
		builder.append("}");
	}



	public String makeApplication(Map<String, Set<String>> dependencyTree, String modulesDirectory, final String mainModule, boolean execute, Map<String, String> externals) {
		StringBuilder result = new StringBuilder();
		makeConfig(result, modulesDirectory, dependencyTree, externals);
		if (!Strings.isNullOrEmpty(mainModule)) {
			makeMainModuleSetup(result, mainModule, execute);
		}
		return result.toString();
	}

	// Make the configuration for the application
	protected abstract StringBuilder makeConfig(StringBuilder result, String modulesDirectory, Map<String, Set<String>> dependencyTree, Map<String, String> externals);

	// Make the setup instructions for the main module
	protected abstract StringBuilder makeMainModuleSetup(StringBuilder result, String mainModule, boolean execute);
}

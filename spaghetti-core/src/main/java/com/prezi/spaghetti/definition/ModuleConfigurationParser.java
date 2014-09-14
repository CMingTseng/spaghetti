package com.prezi.spaghetti.definition;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.prezi.spaghetti.ast.ModuleNode;
import com.prezi.spaghetti.ast.internal.parser.AstParserException;
import com.prezi.spaghetti.ast.internal.parser.MissingTypeResolver;
import com.prezi.spaghetti.ast.internal.parser.ModuleParser;
import com.prezi.spaghetti.ast.internal.parser.ModuleTypeResolver;
import com.prezi.spaghetti.ast.internal.parser.TypeResolver;
import com.prezi.spaghetti.definition.internal.DefaultModuleConfiguration;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Parses module definitions for a module.
 */
public final class ModuleConfigurationParser {
	/**
	 * Parses module definitions for a module.
	 *
	 * @param localModuleSource                the source of the local module.
	 * @param directDependentModuleSources     the sources of direct dependent modules.
	 * @param transitiveDependentModuleSources the sources of transitive dependent modules.
	 * @return the loaded module configuration.
	 */
	public static ModuleConfiguration parse(ModuleDefinitionSource localModuleSource, Collection<ModuleDefinitionSource> directDependentModuleSources, Collection<ModuleDefinitionSource> transitiveDependentModuleSources) {
		Set<String> parsedModules = Sets.newLinkedHashSet();
		DefaultModuleConfiguration configNode = new DefaultModuleConfiguration();

		Collection<ModuleParser> transitiveParsers = createParsersFor(transitiveDependentModuleSources);
		Collection<ModuleParser> directParsers = createParsersFor(directDependentModuleSources);
		Collection<ModuleParser> localParsers = createParsersFor(Collections.singleton(localModuleSource));

		TypeResolver resolver = createResolverFor(Iterables.concat(localParsers, directParsers, transitiveParsers));

		Set<ModuleNode> localModules = Sets.newLinkedHashSet();
		parsedModules(resolver, transitiveParsers, configNode.getTransitiveDependentModules(), parsedModules);
		parsedModules(resolver, directParsers, configNode.getDirectDependentModules(), parsedModules);
		parsedModules(resolver, localParsers, localModules, parsedModules);
		if (localModules.isEmpty()) {
			throw new IllegalStateException("No local module found");
		}
		if (localModules.size() > 1) {
			throw new IllegalStateException("More than one local module found: " + localModules);
		}
		configNode.setLocalModule(Iterables.getOnlyElement(localModules));

		return configNode;
	}

	private static Collection<ModuleParser> createParsersFor(Collection<ModuleDefinitionSource> sources) {
		Set<ModuleParser> parsers = Sets.newLinkedHashSet();
		for (ModuleDefinitionSource source : sources) {
			parsers.add(ModuleParser.create(source));
		}
		return parsers;
	}

	private static TypeResolver createResolverFor(Iterable<ModuleParser> parsers) {
		TypeResolver resolver = MissingTypeResolver.INSTANCE;
		for (ModuleParser parser : parsers) {
			resolver = new ModuleTypeResolver(resolver, parser.getModule());
		}
		return resolver;
	}

	private static void parsedModules(TypeResolver resolver, Collection<ModuleParser> parsers, Collection<ModuleNode> moduleNodes, Set<String> allModuleNames) {
		for (ModuleParser parser : parsers) {
			ModuleNode module = parser.parse(resolver);
			if (allModuleNames.contains(module.getName())) {
				throw new AstParserException(module.getSource(), ": module loaded multiple times: " + module.getName());
			}

			allModuleNames.add(module.getName());
			moduleNodes.add(module);
		}
	}
}
package com.prezi.spaghetti.ast

import com.prezi.spaghetti.ast.internal.DefaultLocation
import com.prezi.spaghetti.ast.internal.parser.Locator
import com.prezi.spaghetti.ast.internal.parser.TypeResolutionContext
import com.prezi.spaghetti.ast.internal.parser.TypeResolver
import com.prezi.spaghetti.definition.internal.DefaultModuleDefinitionSource
import spock.lang.Specification

class AstSpecification extends Specification {
	protected static Location mockLoc = new DefaultLocation(DefaultModuleDefinitionSource.fromString("mock", ""), -1, -1)

	static Locator mockLocator(String definition) {
		return new Locator(DefaultModuleDefinitionSource.fromString("test", definition))
	}

	protected TypeResolver mockResolver(Map<String, Closure<TypeNode>> mocker = [:]) {
		def resolver = Mock(TypeResolver)
		resolver.resolveType(_) >> { TypeResolutionContext context ->
			def name = context.name
			def typeMocker = mocker.get(name.fullyQualifiedName)
			def type = typeMocker ? typeMocker() : null
			if (type) {
				return type
			}

			throw new IllegalStateException("Ran out of scope while looking for type: ${name}")
		}
		return resolver
	}
}

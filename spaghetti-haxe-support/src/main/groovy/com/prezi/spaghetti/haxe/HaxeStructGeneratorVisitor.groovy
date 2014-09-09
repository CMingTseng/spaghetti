package com.prezi.spaghetti.haxe

import com.prezi.spaghetti.ast.PropertyNode
import com.prezi.spaghetti.ast.StructNode

class HaxeStructGeneratorVisitor extends AbstractHaxeMethodGeneratorVisitor {

	@Override
	String visitStructNode(StructNode node) {
		def typeName = node.name
		if (node.typeParameters) {
			typeName += "<" + node.typeParameters*.name.join(", ") + ">"
		}
"""typedef ${typeName} = {
${visitChildren(node)}
}
"""
	}

	@Override
	String visitPropertyNode(PropertyNode node) {
		def mutable = node.annotations.contains("mutable")
		def name = node.name
		def modifiers = mutable ? "" : " (default, never)"
		def type = node.type.accept(this)
		def optional = node.optional ? "@:optional " : ""
		type = wrapNullableTypeReference(type, node)
		"""	${optional}var ${name}${modifiers}:${type};
"""
	}
}

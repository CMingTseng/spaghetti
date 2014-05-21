package com.prezi.spaghetti.haxe

import com.prezi.spaghetti.definition.ModuleDefinition
import com.prezi.spaghetti.definition.WithJavaDoc
import com.prezi.spaghetti.grammar.ModuleParser
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.NotNull

/**
 * Created by lptr on 16/11/13.
 */
class HaxeConstGeneratorVisitor extends AbstractHaxeGeneratorVisitor {

	private String constName

	protected HaxeConstGeneratorVisitor(ModuleDefinition module)
	{
		super(module)
	}

	@Override
	@WithJavaDoc
	String visitConstDefinition(@NotNull @NotNull ModuleParser.ConstDefinitionContext ctx)
	{
		this.constName = ctx.name.text
		def constants = visitChildren(ctx)
		def result = Deprecation.annotationFromCxt(Type.ConstantName, this.constName, ctx.annotations())
		result += \
"""@:final class ${constName} {
${constants}
}
"""
		this.constName = null
		return result
	}

	@Override
	@WithJavaDoc
	String visitConstEntry(@NotNull ModuleParser.ConstEntryContext ctx) {
		def result = Deprecation.annotationFromCxt(Type.ConstantName, this.constName, ctx.annotations())
		if (result) {
			result = "\t" + result
		}
		return result + super.visitConstEntry(ctx)
	}

	@Override
	String visitConstEntryDecl(@NotNull ModuleParser.ConstEntryDeclContext ctx) {
		String type
		Token value
		if (ctx.boolValue) {
			type = "Bool"
			value = ctx.boolValue
		} else if (ctx.intValue) {
			type = "Int"
			value = ctx.intValue
		} else if (ctx.floatValue) {
			type = "Float"
			value = ctx.floatValue
		} else if (ctx.stringValue) {
			type = "String"
			value = ctx.stringValue
		} else {
			throw new IllegalArgumentException("Unknown constant type: " + ctx.dump())
		}
		return "\tpublic static var ${ctx.name.text}(default, never):${type} = ${value.text};\n"
	}
}

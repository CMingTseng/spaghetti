package com.prezi.spaghetti.haxe

import com.prezi.spaghetti.definition.ModuleDefinition
import com.prezi.spaghetti.definition.WithJavaDoc
import com.prezi.spaghetti.grammar.ModuleParser
import org.antlr.v4.runtime.misc.NotNull

import static com.prezi.spaghetti.Generator.CONFIG
import static com.prezi.spaghetti.ReservedWords.MODULE
import static com.prezi.spaghetti.ReservedWords.MODULES

/**
 * Created by lptr on 16/11/13.
 */
class HaxeModuleProxyGeneratorVisitor extends AbstractHaxeMethodGeneratorVisitor {

	HaxeModuleProxyGeneratorVisitor(ModuleDefinition module)
	{
		super(module)
	}

	@WithDeprecation
	@WithJavaDoc
	@Override
	String visitModuleDefinition(@NotNull @NotNull ModuleParser.ModuleDefinitionContext ctx)
	{
		return \
"""@:final class ${module.alias} {
${super.visitModuleDefinition(ctx)}
}
"""
	}

	@Override
	String visitTypeDefinition(@NotNull @NotNull ModuleParser.TypeDefinitionContext ctx)
	{
		// Do not generate code for types
		return ""
	}

	@WithDeprecation
	@WithJavaDoc
	@Override
	String visitMethodDefinition(@NotNull @NotNull @NotNull @NotNull ModuleParser.MethodDefinitionContext ctx)
	{
		def returnType = ctx.returnTypeChain().accept(this)
		returnType = wrapNullable(ctx.annotations(), returnType)

		def params
		def callParams
		if (ctx.parameters) {
			params = ctx.parameters.accept(this)
			callParams = ctx.parameters.elements.collect { it.name.text }.join(", ")
		} else {
			params = ""
			callParams = ""
		}

		return \
"""	@:extern public static inline function ${ctx.name.text}(${params}):${returnType} {
		${returnType == "void"?"":"return "}untyped ${CONFIG}[\"${MODULES}\"][\"${module.name}\"].${MODULE}.${ctx.name.text}(${callParams});
	}
"""
	}
}

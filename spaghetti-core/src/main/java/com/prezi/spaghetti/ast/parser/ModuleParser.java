package com.prezi.spaghetti.ast.parser;

import com.prezi.spaghetti.ast.FQName;
import com.prezi.spaghetti.ast.ModuleMethodType;
import com.prezi.spaghetti.ast.ModuleNode;
import com.prezi.spaghetti.ast.internal.DefaultExternNode;
import com.prezi.spaghetti.ast.internal.DefaultImportNode;
import com.prezi.spaghetti.ast.internal.DefaultModuleMethodNode;
import com.prezi.spaghetti.ast.internal.DefaultModuleNode;
import com.prezi.spaghetti.definition.ModuleDefinitionParser;
import com.prezi.spaghetti.definition.ModuleDefinitionSource;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModuleParser {
	private final List<AbstractModuleTypeParser> typeParsers;
	private final List<com.prezi.spaghetti.grammar.ModuleParser.ModuleMethodDefinitionContext> moduleMethodsToParse;
	private final DefaultModuleNode module;

	public static ModuleParser create(ModuleDefinitionSource source) {
		com.prezi.spaghetti.grammar.ModuleParser.ModuleDefinitionContext context = ModuleDefinitionParser.parse(source);
		try {
			return new ModuleParser(source, context);
		} catch (InternalAstParserException ex) {
			throw new AstParserException(source, ex.getMessage(), ex);
		} catch (Exception ex) {
			throw new AstParserException(source, "Exception while pre-parsing", ex);
		}

	}

	protected ModuleParser(ModuleDefinitionSource source, com.prezi.spaghetti.grammar.ModuleParser.ModuleDefinitionContext moduleCtx) {
		this.typeParsers = new ArrayList<AbstractModuleTypeParser>();
		this.moduleMethodsToParse = new ArrayList<com.prezi.spaghetti.grammar.ModuleParser.ModuleMethodDefinitionContext>();

		String moduleName = moduleCtx.qualifiedName().getText();
		List<String> nameParts = Arrays.asList(moduleCtx.qualifiedName().getText().split("\\."));
		String moduleAlias = moduleCtx.Name() != null ? moduleCtx.Name().getText() : StringUtils.capitalize(nameParts.get(nameParts.size() - 1));
		this.module = new DefaultModuleNode(moduleName, moduleAlias, source);
		AnnotationsParser.parseAnnotations(moduleCtx.annotations(), module);
		DocumentationParser.parseDocumentation(moduleCtx.documentation, module);

		for (com.prezi.spaghetti.grammar.ModuleParser.ModuleElementContext elementCtx : moduleCtx.moduleElement()) {
			if (elementCtx.importDeclaration() != null) {
				FQName importedName = FQName.fromContext(elementCtx.importDeclaration().qualifiedName());
				TerminalNode aliasDecl = elementCtx.importDeclaration().Name();
				String importAlias = aliasDecl != null ? aliasDecl.getText() : importedName.localName;
				DefaultImportNode importNode = new DefaultImportNode(importedName, importAlias);
				module.getImports().put(FQName.fromString(null, importAlias), importNode);
			} else if (elementCtx.externTypeDefinition() != null) {
				com.prezi.spaghetti.grammar.ModuleParser.ExternTypeDefinitionContext context = elementCtx.externTypeDefinition();
				FQName fqName = FQName.fromContext(context.qualifiedName());
				DefaultExternNode extern = new DefaultExternNode(fqName);
				module.getExterns().add(extern, context);
			} else if (elementCtx.typeDefinition() != null) {
				com.prezi.spaghetti.grammar.ModuleParser.TypeDefinitionContext context = elementCtx.typeDefinition();
				AbstractModuleTypeParser typeParser = createTypeDef(context, moduleName);
				typeParsers.add(typeParser);
				module.getTypes().add(typeParser.getNode(), context);
			} else if (elementCtx.moduleMethodDefinition() != null) {
				moduleMethodsToParse.add(elementCtx.moduleMethodDefinition());
			} else {
				throw new InternalAstParserException(elementCtx, "Unknown module element");
			}

		}

	}

	public ModuleNode parse(TypeResolver resolver) {
		try {
			return parseInternal(resolver);
		} catch (InternalAstParserException ex) {
			throw new AstParserException(module.getSource(), ex.getMessage(), ex);
		} catch (Exception ex) {
			throw new AstParserException(module.getSource(), "Exception while pre-parsing", ex);
		}

	}

	protected DefaultModuleNode parseInternal(TypeResolver resolver) {
		// Let us use types from the local module
		resolver = new LocalModuleTypeResolver(resolver, module);

		// Parse each defined type
		for (AbstractModuleTypeParser<?, ?> parser : typeParsers) {
			parser.parse(resolver);
		}

		// Parse module methods
		for (com.prezi.spaghetti.grammar.ModuleParser.ModuleMethodDefinitionContext methodCtx : moduleMethodsToParse) {
			TerminalNode nameCtx = methodCtx.methodDefinition().Name();
			String methodName = nameCtx.getText();
			ModuleMethodType methodType = methodCtx.isStatic != null ? ModuleMethodType.STATIC : ModuleMethodType.DYNAMIC;
			DefaultModuleMethodNode method = new DefaultModuleMethodNode(methodName, methodType);
			AnnotationsParser.parseAnnotations(methodCtx.annotations(), method);
			DocumentationParser.parseDocumentation(methodCtx.documentation, method);
			MethodParser.parseMethodDefinition(resolver, methodCtx.methodDefinition(), method);
			module.getMethods().add(method, nameCtx);
		}

		return module;
	}

	protected static AbstractModuleTypeParser createTypeDef(com.prezi.spaghetti.grammar.ModuleParser.TypeDefinitionContext typeCtx, String moduleName) {
		if (typeCtx.constDefinition() != null) {
			return new ConstParser(typeCtx.constDefinition(), moduleName);
		} else if (typeCtx.enumDefinition() != null) {
			return new EnumParser(typeCtx.enumDefinition(), moduleName);
		} else if (typeCtx.structDefinition() != null) {
			return new StructParser(typeCtx.structDefinition(), moduleName);
		} else if (typeCtx.interfaceDefinition() != null) {
			return new InterfaceParser(typeCtx.interfaceDefinition(), moduleName);
		} else {
			throw new InternalAstParserException(typeCtx, "Unknown module element");
		}

	}

	public final DefaultModuleNode getModule() {
		return module;
	}
}
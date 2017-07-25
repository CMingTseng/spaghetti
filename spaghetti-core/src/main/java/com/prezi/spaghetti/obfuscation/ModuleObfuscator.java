package com.prezi.spaghetti.obfuscation;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.CompilationLevel;
import com.prezi.spaghetti.ast.ModuleNode;
import com.prezi.spaghetti.bundle.DefinitionLanguage;
import com.prezi.spaghetti.definition.ModuleConfiguration;
import com.prezi.spaghetti.definition.ModuleDefinitionSource;
import com.prezi.spaghetti.generator.ReservedWords;
import com.prezi.spaghetti.obfuscation.internal.ClosureCompiler;
import com.prezi.spaghetti.obfuscation.internal.SourceMap;
import com.prezi.spaghetti.obfuscation.internal.SymbolCollectVisitor;
import com.prezi.spaghetti.tsast.TypeScriptAstParserService;


import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Obfuscates a module's code with Google Closure Compiler.
 */
public class ModuleObfuscator {
	private static final Set<String> DEFAULT_PROTECTED_SYMBOLS = collectProtectedSymbols();
	private final Set<String> protectedSymbols;

	private static Set<String> collectProtectedSymbols() {
		ImmutableSet.Builder<String> builder = ImmutableSet.builder();
		builder.add("prototype");
		// Spaghetti protected words
		builder.addAll(ReservedWords.PROTECTED_WORDS);
		return builder.build();
	}

	/**
	 * Creates an obfuscator that will protect the given symbols.
	 *
	 * @param protectedSymbols the symbols to protect.
	 */
	public ModuleObfuscator(Collection<String> protectedSymbols) {
		ImmutableSet.Builder<String> builder = ImmutableSet.builder();
		builder.addAll(DEFAULT_PROTECTED_SYMBOLS);
		builder.addAll(protectedSymbols);
		this.protectedSymbols = builder.build();
	}

	/**
	 * Obfuscates a module and updates its source map (if any).
	 *
	 * @param params the obfuscation parameters.
	 * @return the obfuscated JavaScript with the updated source map.
	 */
	public ObfuscationResult obfuscateModule(ObfuscationParameters params) throws IOException {
		ModuleConfiguration config = params.config;
		ModuleNode module = params.module;

		// OBFUSCATE
		StringBuilder compressedJS = new StringBuilder();

		// Write JavaScript source
		File workDir = params.workingDirectory;
		FileUtils.deleteQuietly(workDir);
		FileUtils.forceMkdir(workDir);
		File closureFile = new File(workDir, "closure.js");
		FileUtils.write(closureFile, params.javaScript);

		Set<File> externs = Sets.newHashSet();
		externs.addAll(params.closureExterns);

		// Append @expose annotations for Closure Compiler
		if (params.compilationLevel == CompilationLevel.ADVANCED_OPTIMIZATIONS) {
			ImmutableSet.Builder<String> builder = ImmutableSet.builder();
			builder.addAll(protectedSymbols).addAll(params.additionalSymbols);
			for (ModuleNode moduleNode : config.getAllModules()) {
				if (moduleNode.getSource().getDefinitionLanguage() == DefinitionLanguage.TypeScript) {
					builder.addAll(getTypeScriptDtsSymbols(moduleNode.getSource(), workDir, params.tsCompilerPath, params.logger));
				} else {
					builder.addAll(new SymbolCollectVisitor().visit(moduleNode));
				}
			}
			ImmutableSet<String> symbols = builder.build();

			List<String> lines = Lists.newArrayList();
			lines.add("/**");
			lines.add(" * @fileoverview externs file.");
 			lines.add(" * @externs");
			lines.add(" */");
			lines.add(String.format("var %s = {};", ReservedWords.MODULE_WRAPPER_FUNCTION));
			lines.add("var __a = {};");
			for (String symbol : symbols) {
				lines.add("__a." + symbol + " = {};");
			}
			File externsFile = new File(workDir, "closure-externs.js");
			externs.add(externsFile);
			FileUtils.writeLines(externsFile, lines);
		}

		// Hand off for compilation
		StringBuilder sourceMapBuilder = new StringBuilder();
		Integer closureRet = ClosureCompiler.compile(closureFile.toString(), compressedJS, module.getName(), sourceMapBuilder, params.compilationLevel, externs);
		if (closureRet != 0) {
			throw new RuntimeException("Closure returned with exit code " + closureRet);
		}

		// SOURCEMAP
		String mapJStoMin = sourceMapBuilder.toString();
		String sourceMap = params.sourceMap;
		Object finalSourceMap;
		if (sourceMap != null) {
			try {
				finalSourceMap = SourceMap.compose(sourceMap, mapJStoMin, "module.map", params.nodeSourceMapRoot);
			} catch (InterruptedException ex) {
				throw new IOException(ex);
			}
		} else {
			finalSourceMap = mapJStoMin;
		}

		URI sourceMapRoot = params.sourceMapRoot;
		if (sourceMapRoot != null) {
			finalSourceMap = SourceMap.relativizePaths((String) finalSourceMap, sourceMapRoot);
		}

		return new ObfuscationResult(compressedJS.toString(), (String) finalSourceMap);
	}

	protected Set<String> getTypeScriptDtsSymbols(ModuleDefinitionSource source, File workDir, File compilerPath, Logger logger) {
		if (source.getDefinitionLanguage() == DefinitionLanguage.TypeScript) {
			if (compilerPath == null) {
				throw new RuntimeException("Cannot extract symbols from .d.ts for obfuscation task: compilerPath is null");
			}
			try {
				return TypeScriptAstParserService.collectExportedSymbols(workDir, compilerPath, source.getContents(), logger);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return Collections.emptySet();
	}
}

package com.prezi.spaghetti.closure;

import com.google.javascript.jscomp.deps.ModuleLoader;
import com.google.javascript.jscomp.AbstractCommandLineRunner;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.DependencyOptions;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.ModuleIdentifier;
import com.google.javascript.jscomp.SourceFile;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

class Args {
    @Option(name="--js_output_file")
    public File outputFile;

    @Option(name="--entry_point")
    public List<String> entryPoints = new ArrayList<String>();

    @Option(name="--js")
    public List<String> inputPatterns = new ArrayList<String>();

    @Option(name="--externs")
    public List<String> externsPatterns = new ArrayList<String>();

    @Option(name="--target")
    public String target = "none";
}

class ClosureWrapper {

    static DiagnosticType EARLY_REFERENCE = findDiagnosticType("JSC_REFERENCE_BEFORE_DECLARE");

    // VariableReferenceCheck is a protected class, so we have to access
    // VariableReferenceCheck.EARLY_REFERENCE the hacky way.
    static DiagnosticType findDiagnosticType(String key) {
        for (DiagnosticType t : DiagnosticGroups.CHECK_VARIABLES.getTypes()) {
            if (key.equals(t.key)) {
                return t;
            }
        }

        throw new RuntimeException("Cannot locate EARLY_REFERENCE");
    }


    private static List<ModuleIdentifier> getEntryPoints(List<String> entryFiles) {
        List<ModuleIdentifier> entryPoints = new ArrayList<ModuleIdentifier>();
        for (String s : entryFiles) {
            entryPoints.add(ModuleIdentifier.forFile(s));
        }
        return entryPoints;
    }

    public static void main(String[] args) throws IOException {
        Args parsedArgs = new Args();
        CmdLineParser parser = new CmdLineParser(parsedArgs);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        Compiler compiler = new Compiler(System.err);
        CompilerOptions options = new CompilerOptions();

        CompilationLevel level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
        level.setOptionsForCompilationLevel(options);
        level.setWrappedOutputOptimizations(options);
        options.setProcessCommonJSModules(true);
        options.setTrustedStrings(true);
        options.setEnvironment(CompilerOptions.Environment.BROWSER);
        options.setConvertToDottedProperties(false);
        options.setModuleResolutionMode(ModuleLoader.ResolutionMode.NODE);
        // Dependency mode STRICT
        options.setDependencyOptions(new DependencyOptions()
            .setDependencyPruning(true)
            .setDependencySorting(true)
            .setMoocherDropping(true)
            .setEntryPoints(getEntryPoints(parsedArgs.entryPoints)));

        if (parsedArgs.target.toUpperCase().equals("ES5")) {
            options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT);
            options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5_STRICT);
        } else {
            options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT_2015);
            options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT_2015);
        }

        options.setWarningLevel(DiagnosticGroups.CHECK_VARIABLES, CheckLevel.WARNING);
        // Report an error if there is an import cycle in the module resolution.
        // (ie. EARLY_REFERENCE, the module is referenced before it is defined).
        options.setWarningLevel(
            new DiagnosticGroup(EARLY_REFERENCE),
            CheckLevel.ERROR);

        List<SourceFile> externs = new ArrayList<SourceFile>();
        externs.addAll(AbstractCommandLineRunner.getBuiltinExterns(options.getEnvironment()));
        for (String path : CommandLineRunner.findJsFiles(parsedArgs.externsPatterns)) {
            externs.add(SourceFile.fromFile(path));
        }

        List<SourceFile> inputs = new ArrayList<SourceFile>();
        for (String path : CommandLineRunner.findJsFiles(parsedArgs.inputPatterns)) {
            inputs.add(SourceFile.fromFile(path));
        }

        compiler.compile(externs, inputs, options);

        if (compiler.hasErrors()) {
            JSError[] errors = compiler.getErrors();
            for (JSError e : errors) {
                if (e.getType() == EARLY_REFERENCE) {
                    System.err.println(String.format("The error '%s'", e.description));
                    System.err.println("  likely means that there is a cycle in the module import graph.");
                    System.err.println("  You must restructure the modules so there are no circular imports.");
                    break;
                }
            }
            System.exit(1);
        } else {
            Writer writer = new FileWriter(parsedArgs.outputFile);
            writer.write(compiler.toSource());
            writer.write("\n");
            writer.close();
            System.out.println("Wrote: " + parsedArgs.outputFile.getAbsolutePath());
        }
    }
}
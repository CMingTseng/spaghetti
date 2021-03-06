package com.prezi.spaghetti.tsast

import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

class TypeScriptAstParserServiceTest extends Specification {
    def "extract symbols from .d.ts"() {
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();
        Logger logger = LoggerFactory.getLogger(TypeScriptAstParserServiceTest.class)
        File compilerPath = new File("build/typescript/node_modules/typescript");

        when:
        def content = """
declare module a.b.c {
    enum d {
        e,
        f,
        g
    }

    interface XInterface {
        hh(xparam: string): void;
    }

    type XType = number | string;



    const ii:XInterface;
    function jj(xparam: number, xparam: string): { kk: number, ll: string };

    export type ElementReference = string | XInterface | {m: string} | {n?: string} | {o: "viewport"};
    export type FTypes = (no: string) => number;
}
        """
        Set<String> symbols = TypeScriptAstParserService.collectExportedSymbols(dir, compilerPath, content, logger);

        then:
        def l = symbols.toList()
        l.sort()
        l.join(",") == "a,b,c,d,e,f,g,hh,ii,jj,kk,ll,m,n,o"
    }

    def "classes not allowed"() {
        when:
        def lines = runVerify("""
module test {
    export class Test {
    }
}
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Classes are not allowed.")
    }

    def "var, let not allowed"() {
        when:
        def lines = runVerify("""
module test {
    export var test: number;
    export let test2: number;
}
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("'var' and 'let' are not allowed")
    }

    def "references are allowed"() {
        when:
        def lines = runVerify("""
/// <reference path="internal/other.ts"/>
/// <reference types="node" />
module test {}
""")
        then:
        lines == []
    }

    def "multiple top-level statements not allowed"() {
        when:
        def lines = runVerify("""
module test {}
interface A {}
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Expecting only one module declaration")
    }

    def "untyped variables are not allowed"() {
        when:
        def lines = runVerify("""
module test {
    export const a = "a";
    export const b;
    export const c: any;
}
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Variables without explicit types are not allowed")
        e.output[1].contains("Variables without explicit types are not allowed")
        e.output[2].contains("Variables should not have 'any' type")
    }

    def "commonjs: untyped variables are not allowed"() {
        when:
        def lines = runMergeDtsForJs("""
export const a = "a";
export const b;
export const c: any;
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Variables without explicit types are not allowed")
        e.output[1].contains("Variables without explicit types are not allowed")
        e.output[2].contains("Variables should not have 'any' type")
    }

    def "non-exported are ignored in non-ambient context"() {
        when:
        def lines = runVerify("""
module test {
    let a = "a";
    var b;
    class A {}
}
""")
        then:
        lines == []
    }

    def "ambient context members are implicitly exported"() {
        when:
        def lines = runVerify("""
declare module test {
    class A {}
}
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Classes are not allowed.")
    }

    def "ambient non-exported sub-context members are ignored"() {
        when:
        def lines = runVerify("""
module test {
    var a: any;

    declare module B {
        class C {}
    }
}
""")
        then:
        lines == []
    }

    def "ambient sub-context members are implicitly exported"() {
        when:
        def lines = runVerify("""
module test {
    var a: any;

    export module B {
        export declare module C {
            class D {}
        }
    }
}
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Classes are not allowed.")
    }


    def "non-exported sub-module members are ignored"() {
        when:
        def lines = runVerify("""
module test {
    var a: any;

    export module B {
        module C {
            class D {}
        }
    }
}
""")
        then:
        lines == []
    }

    def "var, let inside functions bodies are ignored"() {
        when:
        def lines = runVerify("""
module test {
    export function a(): number {
        var b = 1;
        let c = 2;
        return b + c;
    }
}
""")
        then:
        lines == []
    }

    def runVerify(String content) {
        return executeVerify(content, false)
    }

    def "commonjs: classes not allowed"() {
        when:
        def lines = runCommonJsVerify("""
export class Test {
}
""",
checkImported)

        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Classes are not allowed.")

        where:
        checkImported << [false, true]
    }

    def "commonjs: var, let not allowed"() {
        when:
        def lines = runCommonJsVerify("""
export var test: number;
export let test2: number;
""",
checkImported)
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("'var' and 'let' are not allowed")

        where:
        checkImported << [false, true]
    }

    def "commonjs: references are allowed"() {
        when:
        def lines = runCommonJsVerify("""
/// <reference path="internal/other.ts"/>
/// <reference types="node" />
module test {}
""",
checkImported)
        then:
        lines == []

        where:
        checkImported << [false, true]
    }

    def "commonjs: untyped variables are not allowed"() {
        when:
        def lines = runCommonJsVerify("""
export const a = "a";
export const b;
export const c: any;
""",
checkImported)

        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Variables without explicit types are not allowed")
        e.output[1].contains("Variables without explicit types are not allowed")
        e.output[2].contains("Variables should not have 'any' type")

        where:
        checkImported << [false, true]
    }

    def "commonjs: non-exported are ignored in non-ambient context"() {
        when:
        def lines = runCommonJsVerify("""
let a = "a";
var b;
class A {}
""",
checkImported)

        then:
        lines == []

        where:
        checkImported << [false, true]
    }

    def "commonjs: ambient context members are implicitly exported"() {
        when:
        def lines = runCommonJsVerify("""
export declare module test {
    class A {}
}
""",
checkImported)

        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Classes are not allowed.")

        where:
        checkImported << [false, true]
    }

    def "commonjs: ambient non-exported sub-context members are ignored"() {
        when:
        def lines = runCommonJsVerify("""
module test {
    var a: any;

    export declare module B {
        class C {}
    }
}
""",
checkImported)

        then:
        lines == []

        where:
        checkImported << [false, true]
    }

    def "commonjs: ambient sub-context members are implicitly exported"() {
        when:
        def lines = runCommonJsVerify("""
export module test {
    var a: any;

    export module B {
        export declare module C {
            class D {}
        }
    }
}
""",
checkImported)

        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Classes are not allowed.")

        where:
        checkImported << [false, true]
    }


    def "non-exported sub-module members are ignored"() {
        when:
        def lines = runCommonJsVerify("""
export module test {
    var a: any;

    export module B {
        module C {
            class D {}
        }
    }
}
""",
checkImported)

        then:
        lines == []

        where:
        checkImported << [false, true]
    }

    def "var, let inside functions bodies are ignored"() {
        when:
        def lines = runCommonJsVerify("""
export function a(): number {
    var b = 1;
    let c = 2;
    return b + c;
}
""",
checkImported)

        then:
        lines == []

        where:
        checkImported << [false, true]
    }

    def "commonjs: with no import statements"() {
        when:
        def lines = runMergeDtsForJs("""
export function foo(){};
""")
        then:
        lines == []
    }

    def "commonjs: with single import statement"() {
        when:
        def lines = runMergeDtsForJs("""
import * as a from './b';
export function foo(){};
""",
"""export interface Foo { }""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Missing export * from './b' statement")
    }

    def "commonjs: with single import and export statement"() {
        when:
        def lines = runMergeDtsForJs("""
import * as a from './b';
export * from './b'
""",
"""export interface Foo { }""")
        then:
        lines == []
    }

    def "commonjs: with relative export statement and named exports"() {
        when:
        def lines = runMergeDtsForJs("""
export { a } from './b'
""",
"""export interface Foo { }""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Named exports are not supported from relative modules: './b'");
    }

    def "commonjs: imported file cannot contain relative import"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
""",
"""
import * as a from './c';
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Relative imports are not permitted in a file being merged");
    }

    def "commonjs: imported file can contain non-relative import"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
""",
"""
import * as a from 'react';
""")
        then:
        lines == []
    }

    def "commonjs: conflicting import: default import style"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
import one from 'react';
""",
"""
import one from 'react2';
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Duplicate imported identifier: 'one'");
    }

    def "commonjs: conflicting import: star import style"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
import * as one from 'react';
""",
"""
import * as one from 'react2';
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Duplicate imported identifier: 'one'");
    }

    def "commonjs: conflicting import: named import style"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
import { one, two } from 'react';
""",
"""
import { one } from 'react2';
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Duplicate imported identifier: 'one'");
    }

    def "commonjs: conflicting import: named with alias import style"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
import { one, two } from 'react';
""",
"""
import { x as one } from 'react';
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Duplicate imported identifier: 'one'");
    }


    def "commonjs: merged import: default import style"() {
        when:
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();
        File outputFile = new File(dir, "output.d.ts");
        def lines = runMergeDtsForJs("""
import one from 'react';
import two from 'react';
export * from './b'
""",
"""import one from 'react';
import two from 'react';
export interface Foo { }
""", outputFile)
        then:
        lines == []
        outputFile.getText() == """
import one from 'react';
import two from 'react';
/* Start of inlined export: './b' */


export interface Foo { }

/* End of inlined export: './b' */
""";
    }

    def "commonjs: merged import: star import style"() {
        when:
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();
        File outputFile = new File(dir, "output.d.ts");
        def lines = runMergeDtsForJs("""
import * as one from 'react';
import * as two from 'react';
export * from './b'
""",
"""import * as one from 'react';
import * as two from 'react';
export interface Foo { }
""", outputFile)
        then:
        lines == []
        outputFile.getText() == """
import * as one from 'react';
import * as two from 'react';
/* Start of inlined export: './b' */


export interface Foo { }

/* End of inlined export: './b' */
""";
    }

    def "commonjs: merged import: named import style"() {
        when:
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();
        File outputFile = new File(dir, "output.d.ts");
        def lines = runMergeDtsForJs("""
import { one, two, three } from 'react';
export * from './b'
""",
"""import { one, two, four } from 'react';
import { three } from 'react';
export interface Foo { }
""", outputFile)
        then:
        lines == []
        outputFile.getText() == """
import { one, two, three } from 'react';
/* Start of inlined export: './b' */
import { four } from 'react';

export interface Foo { }

/* End of inlined export: './b' */
""";
    }


    def "commonjs: merged import: named with alias import style"() {
        when:
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();
        File outputFile = new File(dir, "output.d.ts");
        def lines = runMergeDtsForJs("""
import { one as r, three } from 'react';
export * from './b'
""",
"""import { one as r, two, three } from 'react';
export interface Foo { }
""", outputFile)
        then:
        lines == []
        outputFile.getText() == """
import { one as r, three } from 'react';
/* Start of inlined export: './b' */
import { two } from 'react';
export interface Foo { }

/* End of inlined export: './b' */
""";
    }

    def "commonjs: imported file cannot contain relative export"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
""",
"""
export * from './c';
""")
        then:
        def e = thrown(TypeScriptAstParserException)
        e.output[0].contains("Exports from relative paths are not permitted in a file being merged.");
    }

    def "commonjs: with no import and export statement"() {
        when:
        def lines = runMergeDtsForJs("""
export * from './b'
""",
"""export interface Foo { }""")
        then:
        lines == []
    }

    def "commonjs: with no import and export statement"() {
        when:
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();
        File outputFile = new File(dir, "output.d.ts");
        def lines = runMergeDtsForJs("""// a comment
/* pre import */ import { b } from './b'; /* post import */
/* above comment */
/* pre comment */ export * from './b'; /* post comment */
// another comment
export interface A { }
""",
"""export interface Foo { }""", outputFile)
        then:
        lines == []
        outputFile.getText() == """// a comment
/* pre import */  /* post import */
/* above comment */
/* pre comment */ /* Start of inlined export: './b' */
export interface Foo { }
/* End of inlined export: './b' */ /* post comment */
// another comment
export interface A { }
""";
    }

    def "commonjs: with reference path"() {
        when:
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();
        File outputFile = new File(dir, "output.d.ts");
        def lines = runMergeDtsForJs("""/// <reference path="./b" />
export * from './b';
""",
"""export interface Foo { }""", outputFile)
        then:
        lines == []
        outputFile.getText() == """/// <reference path="./b" />
/* Start of inlined export: './b' */
export interface Foo { }
/* End of inlined export: './b' */
""";
    }

    def runCommonJsVerify(String code, boolean inSubModule) {
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();

        def content = code;
        if (inSubModule) {
            File importFile = new File(dir, "b.d.ts");
            FileUtils.write(importFile, code);
            content = "export * from './b';"
        }

        File definitionFile = new File(dir, "definition.d.ts");
        FileUtils.write(definitionFile, content);

        return TypeScriptAstParserService.mergeDefinitionFileImports(
            dir,
            new File("build/typescript/node_modules/typescript"),
            definitionFile,
            new File(dir, "output.d.ts"),
            LoggerFactory.getLogger(TypeScriptAstParserServiceTest.class));
    }

    def runMergeDtsForJs(String content, String importedContent = null, File outputFile =  null) {
        File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
        dir.mkdirs();

        File definitionFile = new File(dir, "definition.d.ts");
        FileUtils.write(definitionFile, content);

        if (importedContent != null) {
            File importFile = new File(dir, "b.d.ts");
            FileUtils.write(importFile, importedContent);
        }

        Logger logger = LoggerFactory.getLogger(TypeScriptAstParserServiceTest.class);
        File compilerPath = new File("build/typescript/node_modules/typescript");

        if (outputFile == null) {
            outputFile = new File(dir, "output.d.ts");
        }

        return TypeScriptAstParserService.mergeDefinitionFileImports(dir, compilerPath, definitionFile, outputFile, logger);
    }

	def "lazy: LazyModule interface must exist"() {
		when:
		def lines = runLazyVerify("""
module test {
    export interface Test {
    }
}
""")
		then:
		def e = thrown(TypeScriptAstParserException)
		e.output[0].contains("Lazy module must have an interface named LazyModule.")
	}

	def "lazy: valid LazyModule interface"() {
		when:
		def lines = runLazyVerify("""
module test {
    export interface LazyModule {
    }
}
""")
		then:
		lines == []
	}

	def "lazy: should not contain enum"() {
		when:
		def lines = runLazyVerify("""
module test {
	export enum A {
		A1,
		A2
	}
    export interface LazyModule {
    }
}
""")
		then:
		def e = thrown(TypeScriptAstParserException)
		e.output[0].contains("Lazy module can contain only const enums.")
	}

	def "lazy: const enum is valid"() {
		when:
		def lines = runLazyVerify("""
module test {
	export const enum A {
		A1,
		A2
	}
    export interface LazyModule {
    }
}
""")
		then:
		lines == []
	}

	def runLazyVerify(String content) {
		return executeVerify(content, true)
	}


	def executeVerify(String content, boolean lazy) {
		File dir = Files.createTempDirectory("TypeScriptAstParserServiceTest").toFile();
		dir.mkdirs();

		File definitionFile = new File(dir, "definition.d.ts");
		FileUtils.write(definitionFile, content);

		Logger logger = LoggerFactory.getLogger(TypeScriptAstParserServiceTest.class);
		File compilerPath = new File("build/typescript/node_modules/typescript");

		if (lazy) {
			return TypeScriptAstParserService.verifyLazyModuleDefinition(dir, compilerPath, definitionFile, logger);
		}
		return TypeScriptAstParserService.verifyModuleDefinition(dir, compilerPath, definitionFile, logger);
	}
}

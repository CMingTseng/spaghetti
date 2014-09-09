package com.prezi.spaghetti.typescript.access

import com.prezi.spaghetti.ast.AstTestBase
import com.prezi.spaghetti.ast.parser.ModuleParser
import com.prezi.spaghetti.definition.ModuleDefinitionSource

class TypeScriptModuleAccessorGeneratorVisitorTest extends AstTestBase {
	def "generate"() {
		def definition = """module com.example.test

interface MyInterface<T> {
	/**
	 * This should have nothing to do with the results.
	 */
	void someDummyMethod(int x)
}
/**
 * Initializes module.
 */
@deprecated("use doSomething() instead")
void initModule(int a, ?int b)
string doSomething()
int doStatic(int a, int b)
<T> MyInterface<T> returnT(T t)
"""
		def module = ModuleParser.create(new ModuleDefinitionSource("test", definition)).parse(mockResolver())
		def visitor = new TypeScriptModuleAccessorGeneratorVisitor(module)

		expect:
		visitor.visit(module) == """export class Test {

	private static module:any = SpaghettiConfiguration["__modules"]["com.example.test"]["module"];

	/**
	 * Initializes module.
	 */
	static initModule(a:number, b?:number):void {
		Test.module.initModule(a, b);
	}
	static doSomething():string {
		return Test.module.doSomething();
	}
	static doStatic(a:number, b:number):number {
		return Test.module.doStatic(a, b);
	}
	static returnT<T>(t:T):com.example.test.MyInterface<T> {
		return Test.module.returnT(t);
	}

}
"""
	}
}

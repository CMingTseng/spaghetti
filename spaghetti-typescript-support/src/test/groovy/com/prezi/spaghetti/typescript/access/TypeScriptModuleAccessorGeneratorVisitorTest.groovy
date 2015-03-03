package com.prezi.spaghetti.typescript.access

import com.prezi.spaghetti.generator.ModuleGeneratorSpecification

class TypeScriptModuleAccessorGeneratorVisitorTest extends ModuleGeneratorSpecification {
	def "generate"() {
		def definition = """
module com.example.test

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

		def result = parseAndVisitModule(definition, new TypeScriptModuleAccessorGeneratorVisitor())

		expect:
		result == """export class TestModule {

	private static module:any = Spaghetti["dependencies"]["com.example.test"]["module"];

	/**
	 * Initializes module.
	 */
	static initModule(a:number, b?:number):void {
		TestModule.module.initModule(a, b);
	}
	static doSomething():string {
		return TestModule.module.doSomething();
	}
	static doStatic(a:number, b:number):number {
		return TestModule.module.doStatic(a, b);
	}
	static returnT<T>(t:T):com.example.test.MyInterface<T> {
		return TestModule.module.returnT(t);
	}

}
"""
	}
}

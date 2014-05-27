package com.prezi.spaghetti.typescript.access

import com.prezi.spaghetti.definition.DefinitionParserHelper
import spock.lang.Specification

/**
 * Created by lptr on 24/05/14.
 */
class TypeScriptModuleAccessorGeneratorVisitorTest extends Specification {
	def "generate"() {
		def module = new DefinitionParserHelper().parse("""module com.example.test

interface MyInterface<T> {}
/**
 * Initializes module.
 */
@deprecated("use doSomething() instead")
void initModule(int a, int b)
string doSomething()
static int doStatic(int a, int b)
<T> MyInterface<T> returnT(T t)
""")
		def visitor = new TypeScriptModuleAccessorGeneratorVisitor(module)

		expect:
		visitor.processModule() == """export class Test {

	private static __instance:any = SpaghettiConfiguration["__modules"]["com.example.test"]["__instance"];
	private static __static:any = SpaghettiConfiguration["__modules"]["com.example.test"]["__static"];

	/**
	 * Initializes module.
	 */
	initModule(a:number, b:number):void {
		Test.__instance.initModule(a, b);
	}
	doSomething():string {
		return Test.__instance.doSomething();
	}
	static doStatic(a:number, b:number):number {
		return Test.__static.doStatic(a, b);
	}
	returnT<T>(t:T):com.example.test.MyInterface<T> {
		return Test.__instance.returnT(t);
	}

}
"""
	}
}
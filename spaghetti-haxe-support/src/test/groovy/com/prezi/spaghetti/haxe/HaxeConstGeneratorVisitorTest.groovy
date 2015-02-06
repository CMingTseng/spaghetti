package com.prezi.spaghetti.haxe

import com.prezi.spaghetti.ast.AstSpecification
import com.prezi.spaghetti.ast.internal.parser.ModuleParser

class HaxeConstGeneratorVisitorTest extends AstSpecification {
	def "generate"() {
		def definition = """module com.example.test

/**
 * My dear constants.
 */
@deprecated
const MyConstants {
	int alma = 1
	/**
	 * Bela is -123.
	 */
	@deprecated("lajos")
	int bela = -123
	geza = -1.23
	tibor = "tibor"
}
"""
		def locator = mockLocator(definition)
		def parser = ModuleParser.create(locator.source)
		def module = parser.parse(mockResolver())
		def visitor = new HaxeConstGeneratorVisitor()

		expect:
		visitor.visit(module) == """/**
 * My dear constants.
 */
@:deprecated
@:final class MyConstants {
	public static inline var alma:Int = 1;
	/**
	 * Bela is -123.
	 */
	@:deprecated("lajos")
	public static inline var bela:Int = -123;
	public static inline var geza:Float = -1.23;
	public static inline var tibor:String = "tibor";

}
"""
	}
}

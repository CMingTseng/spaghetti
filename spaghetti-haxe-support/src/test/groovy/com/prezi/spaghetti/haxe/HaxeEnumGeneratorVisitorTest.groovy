package com.prezi.spaghetti.haxe

import com.prezi.spaghetti.generator.EnumGeneratorSpecification

class HaxeEnumGeneratorVisitorTest extends EnumGeneratorSpecification {
	def "generate"() {
		def definition = """enum MyEnum {
	/**
	 * Alma.
	 */
	ALMA
	@deprecated("escape \\"this\\"!")
	BELA
	GEZA
}
"""
		def result = parseAndVisitEnum(definition, new HaxeEnumGeneratorVisitor())

		expect:
		result == """abstract MyEnum(Int) {
	/**
	 * Alma.
	 */
	public static var ALMA = new MyEnum(0);
	@:deprecated("escape \\"this\\"!")
	public static var BELA = new MyEnum(1);
	public static var GEZA = new MyEnum(2);

	static var _values:Array<MyEnum> = [ ALMA, BELA, GEZA ];
	static var _names:Array<String> =  [ "ALMA", "BELA", "GEZA" ];

	inline function new(value:Int) {
		this = value;
	}

	@:to public function value():Int {
		return this;
	}

	@:from public static function fromValue(value:Int) {
		if (value < 0 || value >= _values.length) {
			throw "Invalid value for MyEnum: " + value;
		}
		var result = _values[value];
		return result;
	}

	@:to public inline function name():String {
		return _names[this];
	}

	@:from public static inline function valueOf(name:String) {
		return switch(name)
		{
			case "ALMA": ALMA;
			case "BELA": BELA;
			case "GEZA": GEZA;
			default: throw "Invalid name for MyEnum: " + name;
		};
	}

	public static function values():Array<MyEnum> {
		return _values.copy();
	}
}
"""
	}
}

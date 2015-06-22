package com.prezi.spaghetti.haxe

import com.prezi.spaghetti.ast.EnumNode
import com.prezi.spaghetti.ast.EnumValueNode
import com.prezi.spaghetti.ast.StringModuleVisitorBase

class HaxeEnumGeneratorVisitor extends StringModuleVisitorBase {
	@Override
	String visitEnumNode(EnumNode node) {
		def enumName = node.name

		return \
"""abstract ${enumName}(Int) {
${node.values*.accept(new EnumValueVisitor(node.name)).join("\n")}

	static var _values = { ${node.values.collect { entry -> "\"${entry.value}\": ${entry.name}" }.join(", ")} };
	static var _names =  { ${node.values.collect { entry -> "\"${entry.value}\": \"${entry.name}\"" }.join(", ")} };

	inline function new(value:Int) {
		this = value;
	}

	@:to public function value():Int {
		return this;
	}

	@:from public static function fromValue(value:Int) {
		var key: String = Std.string(value);
		if (!Reflect.hasField(_values, key)) {
			throw "Invalid value for ${enumName}: " + value;
		}
		return Reflect.field(_values, key);
	}

	@:to public inline function name():String {
		return Reflect.field(_names, Std.string(this));
	}

	@:from public static inline function valueOf(name:String) {
		return switch(name)
		{
${node.values.collect {"			case \"${it}\": ${it};"}.join("\n")}
			default: throw "Invalid name for ${enumName}: " + name;
		};
	}

	public static function values():Array<${enumName}> {
		return [${node.values.collect { it }.join(", ")}];
	}
}
"""
	}

	private static class EnumValueVisitor extends AbstractHaxeGeneratorVisitor {
		private final String enumName

		EnumValueVisitor(String enumName) {
			this.enumName = enumName
		}

		@Override
		String visitEnumValueNode(EnumValueNode node) {
			return "\tpublic static var ${node.name} = new ${enumName}(${node.value});"
		}
	}
}

package com.prezi.spaghetti.packaging

import spock.lang.Specification

class CommonJsWrapperTest extends Specification {
	def "CommonJS module"() {
		def originalScript = "/* Generated by Spaghetti */ __spaghetti(function(SpaghettiConfiguration){});"
		def result = new CommonJsWrapper().wrap("com.example.test", ["com.example.alma", "com.example.bela"], originalScript)

		expect:
		result == [
		        'module.exports=function(){',
					'var SpaghettiConfiguration={',
						'"__baseUrl":__dirname,',
						'"__modules":{',
							'"com.example.alma":arguments[0],',
							'"com.example.bela":arguments[1]',
						'},',
						'getName:function(){',
							'return "com.example.test";',
						'},',
						'getResourceUrl:function(resource){',
							'if(resource.substr(0,1)!="/"){',
								'resource="/"+resource;',
							'}',
							'return __dirname+resource;',
						'}',
					'};',
					'var __spaghetti=function(){',
						'return arguments[0](SpaghettiConfiguration);',
					'};',
					'/* Generated by Spaghetti */ ',
					'return __spaghetti(function(SpaghettiConfiguration){});',
				'};'
		].join("")
	}

	def "CommonJS application"() {
		def dependencyTree = [
				"com.example.test": ["com.example.alma", "com.example.bela"].toSet(),
				"com.example.alma": ["com.example.bela"].toSet(),
				"com.example.bela": [].toSet()
		]
		def result = new CommonJsWrapper().makeApplication("lajos", "mods", dependencyTree, "com.example.test", true)

		expect:
		result == [
				'var modules=[];',
				'modules.push(require("lajos/mods/com.example.bela")());',
				'modules.push(require("lajos/mods/com.example.alma")(modules[0]));',
				'modules.push(require("lajos/mods/com.example.test")(modules[1],modules[0]));',
				'modules[2]["module"]["main"]();',
				'\n'
		].join("")
	}
}

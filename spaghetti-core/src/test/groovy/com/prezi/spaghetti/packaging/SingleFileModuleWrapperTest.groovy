package com.prezi.spaghetti.packaging

import com.prezi.spaghetti.internal.Version
import com.prezi.spaghetti.packaging.internal.SingleFileModuleWrapper

class SingleFileModuleWrapperTest extends WrapperTestBase {
	def "Single file module"() {
		def originalScript = "/* Generated by Spaghetti */ module(function(Spaghetti){})"
		def result = new SingleFileModuleWrapper().wrap(mockParams("com.example.test", "1.0", ["com.example.alma", "com.example.bela"], originalScript))

		expect:
		result == [
		        'function(){',
					'var module=(function(dependencies){',
						'return function(init){',
							'return init.call({},(function(){',
								'var baseUrl=__dirname;',
								'return{',
									'getSpaghettiVersion:function(){return "' + Version.SPAGHETTI_BUILD + '";},',
									'getName:function(){',
										'return "com.example.test";',
									'},',
									'getVersion:function(){return "1.0";},',
									'getResourceUrl:function(resource){',
										'if(resource.substr(0,1)!="/"){',
											'resource="/"+resource;',
										'}',
										'return baseUrl+resource;',
									'},',
									'"dependencies":{',
										'"com.example.alma":dependencies[0],',
										'"com.example.bela":dependencies[1]',
									'}',
								'};',
							'})());',
						'};',
					'})(arguments);',
					'/* Generated by Spaghetti */ ',
					'return{',
						'"module":module(function(Spaghetti){}),',
						'"version":"1.0",',
						'"spaghettiVersion":"' + Version.SPAGHETTI_BUILD + '"',
					'};',
				'}'
		].join("")
	}

	def "Single file application"() {
		def dependencyTree = [
				"com.example.test": ["com.example.alma", "com.example.bela"].toSet(),
				"com.example.alma": ["com.example.bela"].toSet(),
				"com.example.bela": [].toSet()
		]
		def result = new SingleFileModuleWrapper().makeApplication(dependencyTree, "com.example.test", true)

		expect:
		result == [
				'modules["com.example.test"]["module"]["main"]();'
		].join("")
	}
}

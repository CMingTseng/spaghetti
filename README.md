**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Gradle Spaghetti Plugin](#gradle-spaghetti-plugin)
- [What is this?](#what-is-this)
	- [Workflow to write a module](#workflow-to-write-a-module)
	- [Workflow to use a module](#workflow-to-use-a-module)
- [Module System](#module-system)
- [Example](#example)
	- [Interface Language](#interface-language)
	- [Build the module](#build-the-module)
	- [Use your module](#use-your-module)
- [Versioning](#versioning)
- [Challenges](#challenges)

Gradle Spaghetti Plugin
=======================

JS modularization prototype

# What is this?

This is a proof-of-concept implementation of a Gradle plugin that helps in modularizing JS applications. This is how it's supposed to work:

## Workflow to write a module

* write IDL
* choose an implementation language (only Haxe now, Typescript coming)
* generate interfaces: IDL->implementation language (use Gradle plugin)
* write implementation
* deploy artifacts to artifactory.prezi.com (use Gradle plugin)

## Workflow to use a module

* declare dependency on module in Gradle
* choose an implementation language (only Haxe now, Typescript coming)
* generate interfaces: IDL->implementation language (use Gradle plugin)
* generate client proxy code in the implementation language (use Gradle plugin)
* use client proxy code to access module code

# Module System

Reference-style: 
![Module System](http://prezi.github.io/gradle-spaghetti-plugin/images/Module System.png "Module System")

# Example

There is an example project under [in the source code of the plugin](tree/master/src/test/at).

## Interface Language

Language defined in [ANTLR](http://antlr.org/), grammar can be reused without modification if needed in creating an Xtext editor.

```
/**
 * Layout module.
 */
module prezi.graphics.text.Layout {

    /**
     * Describes a block of text.
     */
    type Text {
        /**
         * Inserts the given String at <code>offset</code>.
         */
        void insert(int offset, String text)
        void delete(int offset, int end)
        String getRawText()
    }

    Text createText()
}
```

## Build the module

You can generate Haxe interfaces for your module:

```groovy
task generateHeaders(type: com.prezi.gradle.spaghetti.GenerateHeaders) {
	definition "Layout.module"
	platform "haxe"
	outputDirectory "${buildDir}/spaghetti-module"
}
```

You can then implement these interfaces, and compile all your code to a JavaScript file. Now you only have to bundle your code into a Spaghetti-compatible module, and you're all set:

```groovy
task bundleModule(type: com.prezi.gradle.spaghetti.BundleModule) {
	dependsOn compileHaxe
	definition "Layout.module"
	inputFile compileHaxe.outputFile
	outputFile "${buildDir}/module.zip"
}
```

## Use your module

You can generate Haxe externs to use this module in your application or another module:

```groovy
task generateClient(type: com.prezi.gradle.spaghetti.GenerateClient) {
	configuration configurations.modules
	platform "haxe"
	outputDirectory "$buildDir/haxe"
}
```

Build your application, and then bundle it for [RequireJS](http://requirejs.org/):

```groovy
task bundleApplication(type: com.prezi.gradle.spaghetti.BundleApplication) {
	dependsOn compileHaxe
	configuration configurations.modules
	inputFile compileHaxe.outputFile
	outputFile "${buildDir}/app.js"
}
```

You can also extract all modules to a directory so that they are readily available to RequreJS:

```groovy
task packApplication(type: com.prezi.gradle.spaghetti.ExtractModules) {
	dependsOn bundleApplication
	configuration configurations.modules
	def testWebappDir = file("${buildDir}/webapp")
	outputDirectory testWebappDir
}
```

This is how you can access your module from an application or another module:

```haxe
package prezi.test.client;

class Client {
	public static function main() {
		var layout = Modules.getLayout();
		var text = layout.createText(2);
		text.insert(0, "World");
		text.insert(0, "Hello ");
		trace(text.getRawText());
	}
}
```

# Versioning

![Versioning](http://prezi.github.io/gradle-spaghetti-plugin/images/Versioning.png "Versioning")


# Challenges

* Should we define objects as live things or just placeholders, and let services do everything?
	* Advantage of the former is that things work like OOP, so it's simple for everybody to understand.
	* Advantage of the latter is that things are simpler on the tooling side, and we can create APIs that work across platforms (think of Ruby calling a JS module, like with Cucumber). It also means that we can support non-OOP languages like Elm easier at the cost of clumsier clients in OOP languages.
* How to define FRP signals? Should they be first-class citizens of the DSL, or should we add methods to services like `subscribe(signal:Signal)`?
* How to publish constants and enums?
* How to define dependencies on other modules?

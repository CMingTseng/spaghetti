Spaghetti
=========

Spaghetti enables type-safe communication between JavaScript modules.

[![Build Status](https://travis-ci.org/prezi/spaghetti.svg)](https://travis-ci.org/prezi/spaghetti)
[![Analytics](https://ga-beacon.appspot.com/UA-54695510-1/github.com/prezi/spaghetti)](https://github.com/igrigorik/ga-beacon)

Because of the untyped nature of JavaScript, modularizing large, evolving JavaScript applications into a micro-service architecture is difficult. This has motivated the development of a multitude of languages which compile to JavaScript, all of which excell at different tasks, and none of which is guaranteed to be around in a few years time. Spaghetti bridges between the type systems of compile-to-JS languages, to allow compile-time type checks of module APIs without forcing an overcommitment to any one language.

## How Does it Work?

Spaghetti modules are *implemented* in compile-to-JS languages like [TypeScript](http://typescriptlang.org) and [Haxe](http://haxe.org), but each module's public API is *specified* in the [Spaghetti Interface Definition Language](/../../wiki/Spaghetti Syntax). Here's an example of a typical API definition:

```
module com.example.greeter

interface Greeter {
    string sayHello(string user)
}

Greeter createGreeter()
```

Based on this abstract definition, Spaghetti ensures type safety on both the provider and the user side of an API:

* **checking if a module implements its API properly** is done via generated interfaces that the module must implement. This way the compiler can check if you've made a mistake or have forgotten something. From the above example, the generated code for the `Greeter` Spaghetti interface in a TypeScript module looks like this:

    ```typescript
    /* Generated by Spaghetti */
    module com.example.greeter {
        export interface Greeter {
            sayHello(user:string):string;
        }
    }
    ```

* **checking if a module is calling the right API of its dependency** is also done via code generation. Spaghetti generates language-specific proxy classes to access other modules in a type-safe way, based on those modules' Spaghetti APIs. Here's how you would use `Greeter` from an Haxe module:

    ```haxe
    import com.example.greeter.GreeterModule;
    // ...
    var greeter = GreeterModule.createGreeter();
    trace(greeter.sayHello("World"));
    ```

If you make a typo in `sayHello`, or try to pass a number as its parameter, the Haxe compiler will fail with an error.

For a detailed explanation of the steps to building a Spaghetti application, see [Workflow on the wiki](../../wiki/Workflow).

## How to Use It?

Spaghetti is a [Java](http://java.oracle.com)-based tool, and requires Java 7 or newer. It has multiple interfaces:

* The [command-line tool](spaghetti) is the quickest way to get working with Spaghetti.
* [Gradle plugins](gradle-spaghetti-plugin/README.md) make it easy to integrate Spaghetti into your workflow.

## Try It

* Follow [the tutorial](/../../wiki/Tutorial) for a step-by-step introduction to Spaghetti.
* Check the [demo application](http://prezi.github.io/spaghetti/demo).
* See the [code behind the demo](spaghetti-gradle-example), which uses Gradle integration.

## Documentation

The documentation is [available on the wiki](/../../wiki).

## Contribution

Issues, suggestions and pull-requests are welcome. You can build Spaghetti yourself by running:

    ./gradlew install

### Prerequisites

You will need the following installed to build Spaghetti locally:

* Java 7 or newer
* [TypeScript](http://typescriptlang.org), [Kotlin](http://kotlinlang.org) and [Haxe](http://haxe.org) to run the integration tests

To install Haxe:

* on Mac OS use Homebrew: `brew install haxe`
* on Linux and Windows you can download installers from [http://haxe.org](http://haxe.org/download)

To install TypeScript:

* you'll need Node.js and NPM first, then run: `npm install -g typescript`

To install Kotlin:

* you can download the Kotlin compiler from [GitHub](https://github.com/JetBrains/kotlin/releases/). Make sure `kotlinc` is available on the `PATH`, and `KOTLIN_HOME` is set.

## Mailing List

Get in touch with Spaghetti developers at: [spaghetti-dev@googlegroups.com](https://groups.google.com/forum/#!forum/spaghetti-dev).

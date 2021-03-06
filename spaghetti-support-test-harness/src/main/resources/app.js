var expect = require("chai").expect;

return {
	main: function () {
		var d = Spaghetti["dependencies"];
		/**
		 * We try to load both dotted and underscore package names because we have multiple test configurations:
		 * - Haxe with Spaghetti definitions must use dotted package names
		 * - Typescript definitions must use an identifier without dots
		 */
		var module = d["spaghetti.test"] || d["spaghetti_test"];
		var dependencyModule = d["spaghetti.test.dependency"] || d["spaghetti_test_dependency"];
		describe("Module", function () {

			describe("#addTwoNumbers()", function () {
				it("should add numbers", function () {
					expect(module.addTwoNumbers(1,2)).to.equal(3);
				});
			});

			describe("#getNextEnumValue()", function () {
				it("should return the next element in the DependentEnum enum", function () {
					expect(module.getNextEnumValue(0 /* Apple */)).to.equal(1 /* Pear */);
				});
			});

			describe("#getPositionInPrimes()", function () {
				it("should return the name of the prime enum value", function () {
					expect(module.getPositionInPrimes(3 /* Second */)).to.equal("Second");
				});
			});

			describe("#getValueOfTwo()", function () {
				it("should return the value of the constant Numbers.TWO", function () {
					expect(module.getValueOfTwo()).to.equal("two");
				});
			});

			describe("#getValueOfDependentConstant()", function () {
				it("should return the value of the constant from the dependent module", function () {
					expect(module.getValueOfDependentConstant()).to.equal("constant");
				});
			});

			describe("#createPoint3dWithGivenValues()", function () {
				it("should create a struct with the given values", function () {
					var point3d = module.createPoint3dWithGivenValues(1, 2, 3);
					expect(point3d.x).to.equal(1);
					expect(point3d.y).to.equal(2);
					expect(point3d.z).to.equal(3);
				});
			});

			describe("#getPointFromDependencyModule()", function () {
				it("should return the same object as the one returned by the dependency module", function () {
					var dependencyPoint = dependencyModule.getPoint();
					var point = module.getPointFromDependencyModule();
					expect(point).to.equal(dependencyPoint);
				});
			});

			describe("#getPointFromDependencyModuleViaCallback()", function () {
				it("should return the point", function () {
					var point = module.getPointFromDependencyModuleViaCallback(1, 2);
					expect(point).to.exist;
					expect(point.x).to.equal(1);
					expect(point.y).to.equal(2);
				});
			});

			describe("#returnPointViaCallback()", function () {
				it("should return a point via the callback", function (done) {
					module.returnPointViaCallback(1, 2, 3, function (point) {
						expect(point).to.exist;
						expect(point.x).to.equal(1);
						expect(point.y).to.equal(2);
						expect(point.z).to.equal(3);
						done();
					});
				});
			});

			describe("#getExternalDependencyVersion()", function () {
				it("should return the right version for external dependency", function () {
					expect(module.getExternalDependencyVersion()).to.equal(require("chai").version);
				})
			});

			describe("enum proxying", function () {
				it("should expose the locally defined 'Exported' enum", function () {
					expect(module.Exported.Target).to.equal(42);
				})
			});
		});
	}
};

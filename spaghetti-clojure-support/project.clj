(defproject casserole "0.1.0-SNAPSHOT"
  :description "ClojureScript support for Spaghetti"
  :url "https://github.com/prezi/spaghetti"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "casserole"
              :source-paths ["src"]
              :compiler {
                :output-to "target/casserole.js"
                :output-dir "target"
                :optimizations :none
                :source-map true}}]})
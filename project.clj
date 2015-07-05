(defproject carpet "0.0.1"
  :description "Web and mobile interface for a remmitance application."
  :url         "TODO"
  :license {:name "Unlicense"
            :url  "http://unlicense.org/"}
  :min-lein-version "2.3.3"
  :global-vars {*warn-on-reflection* true
                *assert*             true}
  :dependencies
  [ ;; core libraries
   [org.clojure/clojure       "1.7.0-master-SNAPSHOT"]
   [org.clojure/clojurescript "0.0-3308"]
   [org.clojure/core.async    "0.1.346.0-17112a-alpha"]
   ;; use channels to communicate with clients and users
   [com.taoensso/sente        "1.4.1"]
   ;; logging
   [com.taoensso/timbre       "4.0.2"]
   ;; web server
   [http-kit                  "2.1.19"]
   [ring                      "1.3.2"]
   [ring/ring-defaults        "0.1.3"] ; Includes `ring-anti-forgery`, etc.
   [compojure                 "1.3.4"] ; Or routing lib of your choice
   [hiccup                    "1.0.5"] ; Optional, just for HTML
   ;; Transit deps optional; may be used to aid perf. of larger data payloads
   ;; (see reference example for details):
   ;; http://blog.cognitect.com/blog/2014/7/22/transit
   [com.cognitect/transit-clj  "0.8.259"]
   [com.cognitect/transit-cljs "0.8.199"]
   ;; types are used mainly to enforce the communication protocol
   ;; between client<->server
   [org.clojure/core.typed "0.3.0"]
   [environ "1.0.0"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-environ "1.0.0"]]

  :source-paths ["src/clj" "src/cljc"]
  :cljsbuild
  {:builds                              ; Compiled in parallel
   [{:id           :main
     :source-paths ["src/cljs" "src/cljc"]
     :compiler     {:output-to     "resources/public/main.js"
                    :optimizations :whitespace #_:advanced
                    :pretty-print  true}}]}
  ;; clojure 1.7 needs this repo
  :repositories
  {"sonatype-oss-public"
   "https://oss.sonatype.org/content/groups/public/"})

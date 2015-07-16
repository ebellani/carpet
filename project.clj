(defproject carpet "0.0.1"              ; http://semver.org/
  ;; the comments here are intended to clarify the purpose of each of
  ;; the dependencies. Most of the complexity is related to the
  ;; dev/test environments.
  :description "Web and mobile interface for a remmitance application."
  ;; The point of unlicense is to increase the awareness that IP will
  ;; not protect this project. Speed, competence and secrecy will.
  :license {:name "Unlicense" :url  "http://unlicense.org/"}
  :min-lein-version "2.5.1"
  :global-vars {*warn-on-reflection* true
                *assert*             true
                *print-length*       72}
  :dependencies
  [ ;; core libraries
   [org.clojure/clojure       "1.7.0-master-SNAPSHOT"]
   [org.clojure/clojurescript "0.0-3308"]
   [org.clojure/core.async    "0.1.346.0-17112a-alpha"]
   ;; use channels to communicate with clients and users
   [com.taoensso/sente        "1.5.0"]
   ;; logging
   [com.taoensso/timbre "4.0.2"]
   ;; [com.taoensso/timbre "3.4.0"]
   ;; web server
   [http-kit                  "2.1.19"]
   [ring                      "1.3.2"]
   [ring/ring-defaults        "0.1.3"] ; Includes `ring-anti-forgery`, etc.
   [compojure                 "1.3.4"] ; Or routing lib of your choice
   ;; Transit deps optional; may be used to aid perf. of larger data payloads
   ;; (see reference example for details):
   ;; http://blog.cognitect.com/blog/2014/7/22/transit
   [com.cognitect/transit-clj  "0.8.259"]
   [com.cognitect/transit-cljs "0.8.199"]
   ;; types are used mainly to enforce the communication protocol
   ;; between client<->server
   [org.clojure/core.typed "0.3.4"]
   ;; auth
   [buddy/buddy-hashers "0.4.2"]
   [buddy/buddy-auth "0.5.3"]
   [crypto-random "1.2.0"]
   ;; controls environment-based variables, such as database
   ;; connections.
   [environ "1.0.0"]
   ;; templating system, enabling the usage of plain HTML files.
   [enlive "1.1.5"]
   ;; UI building
   [reagent "0.5.0"]
   ;; attaching a unique key to every item in a dynamically
   ;; generated list of components is good practice, and helps React
   ;; to improve performance for large lists. The key can be given
   ;; either (as in this example) as meta-data, or as a :key item in
   ;; the first argument to a component (if it is a map). See
   ;; React’s documentation [2] for more info.[1]
   ;; [1] https://holmsand.github.io/reagent/index.html
   ;; [2] http://facebook.github.io/react/docs/multiple-components.html#dynamic-children
   [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-environ "1.0.0"]]
  :source-paths ["src/clj" "src/cljc"]

  :cljsbuild
  {:builds
   {:app {:source-paths ["src/cljc"
                         "src/cljs"]
          ;; https://github.com/clojure/clojurescript/wiki/Compiler-Options
          :compiler {:output-to     "resources/public/js/app.js"
                     :output-dir    "resources/public/js/out"
                     :main          "carpet.main"
                     :asset-path    "js/out"
                     :optimizations :none
                     :source-map    true
                     :pretty-print  true}}}}
  :profiles
  {:dev
   {:source-paths ["env/dev/cljc"
                   "env/dev/clj"]
    :test-paths   ["test/clj"]
    :dependencies
    [;; figwheel builds the ClojureScript code and hot loads it into
     ;; the browser as you code
     [lein-figwheel    "0.3.7"]
     ;; use figwheel from the repl. See the file dev.clj file
     [figwheel-sidecar "0.3.7"]
     [com.cemerick/piggieback "0.2.1"]
     ;; Piggieback provides an alternative ClojureScript REPL entry point
     ;; (cemerick.piggieback/cljs-repl) that changes an nREPL session into
     ;; a ClojureScript REPL for eval and load-file operations, while
     ;; accepting all the same options as cljs.repl/repl. When the
     ;; ClojureScript REPL is terminated (by sending :cljs/quit for
     ;; evaluation), the nREPL session is restored to it original state. [1]
     [org.clojure/tools.nrepl "0.2.10"]
     ;; Weasel uses WebSockets to communicate between a
     ;; ClojureScript REPL, which is typically hosted on
     ;; nREPL using piggieback, and an environment which
     ;; can execute compiled ClojureScript, which can be
     ;; a web browser or any JavaScript environment that
     ;; supports the WebSocket APIs. [2]
     [weasel "0.7.0"]
     ;; testing framework
     [midje "1.7.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
     ;; static types
     [org.clojure/core.typed "0.3.4"]]
    :repl-options {:init-ns carpet.server
                   ;; The :repl-options bit causes lein repl to automagically mix the
                   ;; Piggieback nREPL middleware into its default stack. (Yes, you need
                   ;; to explicitly declare a local nREPL dependency to use piggieback,
                   ;; due to a Leiningen bug.) [1]
                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
    :plugins [[lein-figwheel "0.2.1-SNAPSHOT"]]
    :figwheel {:http-server-root "public"
               :server-port 3449
               :css-dirs ["resources/public/css"]}
    :env {:is-dev true
          ;; used in generating the tokens
          :secret "mysupersecret"}
    ;; TODO: cljs test env. Adapt the chestnut example to casper and midje
    :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"
                                              "env/dev/cljc"]}}}}
   ;; apply the maximum level of optimizations.
   :uberjar
   {:source-paths ["env/prod/clj"]
    :hooks        [leiningen.cljsbuild]
    :env          {:production true}
    :omit-source  true
    :aot          :all
    :cljsbuild    {:builds {:app
                            {:source-paths ["env/prod/cljs"]
                             :compiler {:optimizations :advanced
                                        :pretty-print false}}}}}}
  ;; newer stuff may need this repository.
  :repositories
  {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})

;; [1] https://github.com/cemerick/piggieback
;;
;; [2] https://github.com/tomjakubowski/weasel

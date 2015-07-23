;; dependencies are separated by functional concerns and documented
;; here as vars. See the bottom of the file for the declaration of the
;; structure of the project.

;;;;;;;;;;
;; app  ;;
;;;;;;;;;;

(def plugins
  "The leiningen plugins that are necessary for this project to run stand
  alone."
  '[[lein-cljsbuild "1.0.6"]
    [lein-environ   "1.0.0"]])

(def language-deps
  "Languages used in the system"
  '[[org.clojure/clojure       "1.7.0-master-SNAPSHOT"] ;; <- conditional readers
    [org.clojure/clojurescript "0.0-3308"]])

(def channel-deps
  "Used to build communication channels both inter and intra the client<->server
  barrier."
  '[[org.clojure/core.async    "0.1.346.0-17112a-alpha"]
    [com.taoensso/sente        "1.5.0"]
    ;; Transit deps used to aid perf. of larger data payloads
    ;; (see reference example for details):
    ;; http://blog.cognitect.com/blog/2014/7/22/transit
    [com.cognitect/transit-clj  "0.8.259"]
    [com.cognitect/transit-cljs "0.8.199"]])

(def server-deps
  "Regular HTTP/WS server related deps."
  '[[http-kit           "2.1.19"]
    [ring               "1.3.2"]
    [ring/ring-defaults "0.1.3"] ; Includes `ring-anti-forgery`
    [fogus/ring-edn     "0.3.0"]
    [compojure          "1.3.4"]
    [enlive             "1.1.5"]])

(def logging-deps
  '[[com.taoensso/timbre "4.0.2"]])

(def auth-deps
  "Dealing with authentication and authorization."
  '[[buddy/buddy-hashers "0.4.2"]
    [buddy/buddy-auth    "0.5.3"]
    [crypto-random       "1.2.0"]])

(def license
  "The point of unlicense is to increase the awareness that IP will not protect
  this project. Speed, competence and secrecy will."
  {:name "Unlicense" :url "http://unlicense.org/"})

(def environment-deps
  "Controls environment-based variables, such as database connections."
  '[[environ "1.0.0"]])

(def ui-deps
  "Used to build the user interface widgets and their relationship with data."
  '[[reagent "0.5.0"]
    ;; attaching a unique key to every item in a dynamically
    ;; generated list of components is good practice, and helps React
    ;; to improve performance for large lists. The key can be given
    ;; either (as in this example) as meta-data, or as a :key item in
    ;; the first argument to a component (if it is a map). See
    ;; React’s documentation [2] for more info.[1]
    ;; [1] https://holmsand.github.io/reagent/index.html
    ;; [2] http://facebook.github.io/react/docs/multiple-components.html#dynamic-children
    [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]])

(def cljsbuild-compiler-config
  "Used to compile regularly and also for hot loading of cljs code in
  development. For the options themselves see
  https://github.com/clojure/clojurescript/wiki/Compiler-Options"
  {:output-to            "resources/public/js/app.js"
   :output-dir           "resources/public/js/out"
   :main                 "carpet.main"
   :asset-path           "/js/out"
   :source-map-timestamp true
   :optimizations        :none
   :source-map           true
   :pretty-print         true
   :verbose              true})

;;;;;;;;;;;;;;;;;
;; development ;;
;;;;;;;;;;;;;;;;;

(def css-dirs
  "Used to hot load CSS modifications."
  ["resources/public/css"])

(def invariance-deps
  "Quality control by ensuring some invariances. Both existential (contracts and
  tests) and universal (static types) enforcers should be here."
  '[[org.clojure/core.typed "0.3.4"]
    [midje "1.7.0-SNAPSHOT" :exclusions [org.clojure/clojure]]])

(def workflow-deps
  "Used to stablish a workflow where the CLJS is automatically builded
  and hot loaded, and a CLJS repl is possible."
  '[[lein-figwheel    "0.3.7"]
    [figwheel-sidecar "0.3.7"]
    [com.cemerick/piggieback "0.2.1"]
    [org.clojure/tools.nrepl "0.2.10"]
    [weasel "0.7.0"]])

;;;;;;;;;;;;;;;;;;
;; source tree  ;;
;;;;;;;;;;;;;;;;;;

(defn get-source-path [root]
  "to understand the common-path, read the file organization part of
  http://www.danielcompton.net/2015/06/10/clojure-reader-conditionals-by-example"
  (let [common-path (str root "cljc")]
    {:clj  [common-path (str root "clj")]
     :cljs [common-path (str root "cljs")]}))

(def source-paths
  {:app (get-source-path "src/")
   :dev (get-source-path "env/dev/")})

;;;;;;;;;;;;;;;;
;; definition ;;
;;;;;;;;;;;;;;;;

(defproject carpet "0.0.3" ; For version semantics, see http://semver.org/
  :description "Web and mobile interface for a remmitance application."
  :license     ~license
  :dependencies ~(concat language-deps
                         channel-deps
                         server-deps
                         logging-deps
                         auth-deps
                         environment-deps
                         ui-deps)
  :plugins plugins
  :source-paths ~(-> source-paths :app :clj)
  :cljsbuild {:builds {:app {:source-paths ~(-> source-paths :app :cljs)
                             :compiler ~cljsbuild-compiler-config}}}
  :profiles
  {:dev {:global-vars {*warn-on-reflection* true
                       *print-length*       32
                       *print-level*        4}
         :source-paths ~(-> source-paths :dev :clj)
         :test-paths   ["test/clj"]
         :dependencies ~(concat invariance-deps
                                workflow-deps)
         :repl-options {:init-ns carpet.server
                        ;; Causes lein repl to automagically mix the
                        ;; Piggieback nREPL middleware concat its
                        ;; default stack.
                        :nrepl-middleware
                        [cemerick.piggieback/wrap-cljs-repl]}
         :env {:is-dev true
               ;; used in generating the tokens
               :secret "mysupersecret"
               ;; used by the hot loader (figwheel)
               :hot-loading {:css-dirs     ~css-dirs
                             :source-paths
                             ~(concat (-> source-paths :app :cljs)
                                      (-> source-paths :dev :cljs))
                             :cljsbuild-compiler-config
                             ~cljsbuild-compiler-config}}}
   :uberjar {:source-paths ["env/prod/clj"]
             :hooks        [leiningen.cljsbuild]
             :env          {:production true}
             :omit-source  true
             :aot          :all
             :cljsbuild    {:builds {:app
                                     {:source-paths ["env/prod/cljs"]
                                      :compiler {:optimizations :advanced
                                                 :pretty-print false}}}}}}
  ;; newer stuff may need this repository.
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"})

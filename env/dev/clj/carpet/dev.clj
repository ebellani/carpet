(ns carpet.dev
  "Serves to create a browser repl"
  (:require [environ.core                   :refer [env]]
            [net.cgrand.enlive-html         :refer [set-attr prepend append html]]
            [cemerick.piggieback            :as piggieback]
            [weasel.repl.websocket          :as weasel]
            [figwheel-sidecar.auto-builder  :as fig-auto]
            [figwheel-sidecar.core          :as fig]
            [clojurescript-build.auto       :as auto]
            [clojure.java.shell             :refer [sh]]
            [carpet.location                :as dev-location]))

(def is-dev? (env :is-dev))

(def inject-devmode-html
  (comp
     (set-attr :class "is-dev")
     (prepend (html [:script {:type "text/javascript" :src "/js/react.js"}]))))

(defn browser-repl []
  (let [repl-env
        (weasel/repl-env :ip dev-location/repl-env-ip
                         :port dev-location/repl-env-port)]
    (piggieback/cljs-repl repl-env)))

(defn start-figwheel
  "The whole point of this is to start the figwheel server in the same process
  that the regular development takes place."
  []
  (let [server (fig/start-server { :css-dirs ["resources/public/css"] })
        config {:builds [{:id "dev"
                          :source-paths ["env/dev/cljs"
                                         "env/dev/cljc"
                                         "src/cljs"
                                         "src/cljc"]
                          ;; copied from project.clj
                          :compiler
                          {:output-to     "resources/public/js/app.js"
                           :output-dir    "resources/public/js/out"
                           :main          "carpet.main"
                           :asset-path    "js/out"
                           :source-map-timestamp true
                           :optimizations :none
                           :source-map    true
                           :pretty-print  true}}]
                :figwheel-server server}]
    (fig-auto/autobuild* config)))

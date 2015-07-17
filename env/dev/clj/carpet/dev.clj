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
  (let [server (fig/start-server {:css-dirs (-> env :hot-loading :css-dirs)})
        config
        {:builds [{:id "dev"
                   :source-paths (-> env
                                     :hot-loading
                                     :source-paths)
                   :compiler     (-> env
                                     :hot-loading
                                     :cljsbuild-compiler-config)}]
         :figwheel-server server}]
    (fig-auto/autobuild* config)))

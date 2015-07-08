(ns carpet.main
  (:require [figwheel.client  :as figwheel :include-macros true]
            [weasel.repl      :as weasel]
            [carpet.location  :as dev-location]
            [carpet.core      :as core]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback (fn []
                     (core/main)))

(weasel/connect (str "ws://"
                     dev-location/repl-env-ip
                     ":"
                     dev-location/repl-env-port)
                :verbose true
                :print #{:repl :console})

(core/main)

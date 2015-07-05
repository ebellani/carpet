(ns carpet.communication
  "Holds the concepts relating to the shared channel, containing the symbols
  used to communicate information users<->server. The purpose is to centralize
  all the necessary communication symbols in order to facilitate reference from
  across the application and to make the communication layer easier to
  understand.

  All this is required because the communication is an aspect of the whole
  application."
  (:require [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            #?(:clj [taoensso.sente.server-adapters.http-kit
                     :refer (sente-web-server-adapter)])))

(def ^:private packer
  (sente-transit/get-flexi-packer :edn)) ;; Experimental, needs Transit dep

(def communication-url
  "Used both by the server as a URL to create the communication channel in."
  "/chsk")

(let [{:keys [ch-recv
              send-fn
              #?@(:clj
                  [ajax-post-fn
                   ajax-get-or-ws-handshake-fn
                   connected-uids]
                  :cljs [chsk
                         state])]}
      ;; todo
      #?(:clj (sente/make-channel-socket! sente-web-server-adapter
                                          {:packer packer}))
      #?(:cljs (sente/make-channel-socket! communication-url
                                           {:type   :auto
                                            :packer packer}))]
  (def ch-chsk ch-recv)    ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  #?@(:clj
      [(def connected-uids                connected-uids) ; Watchable, read-only atom
       (def ring-ajax-post                ajax-post-fn)
       (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)]
      :cljs
      [(def chsk-state state)   ; Watchable, read-only atom
       (def chsk chsk)]))

;;; communication contract

;;; Shared contract that the server and its user/clients will use to
;;; interact with each other. This is used in order to avoid typos and
;;; to catch at compile time errors in building the communication
;;; messages.

(def currency-rate
  "Used to figure out the rate of a given currency. A payload for this message
  will use currency symbols below in order to communicate the rate as a map."
  :currency-rate)

;;; The currency symbols below are
(def bitcoin
  "Symbol for representing bitcoin "
  :bitcoin)

(def currency-rate
  "Used to figure out the rate of a given currency."
  :currency-rate)

;;; end communication contract

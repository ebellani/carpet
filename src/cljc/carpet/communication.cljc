(ns carpet.communication
  "Holds the concepts relating to the shared channel, containing the symbols
  used to communicate information users<->server. The purpose is to centralize
  all the necessary communication symbols in order to facilitate reference from
  across the application and to make the communication layer easier to
  understand.

  All this is required because the communication is an aspect of the whole
  application."
  (:require [taoensso.sente :as sente]
            [#?(:clj
                taoensso.timbre
                :cljs
                taoensso.encore)
             :as log]
            [taoensso.sente.packers.transit :as sente-transit]
            #?(:clj [taoensso.sente.server-adapters.http-kit
                     :refer (sente-web-server-adapter)])))

(def ^:private packer-payload
  {:packer (sente-transit/get-flexi-packer :edn)})

(def communication-path
  "Path for the communication channel between users<->server."
  "/chsk")

(def csrf-token-name
  "Used to create and retrieve the CSRF [1] token.
  [1] https://en.wikipedia.org/wiki/Cross-site_request_forgery"
  :csrf-token)

(def session-path
  "The path where the login functionality will take place."
  "/login")

;;;;;;;;;;;;;;;;;;;;;;;;
;; channel socket API ;;
;;;;;;;;;;;;;;;;;;;;;;;;

;; Builds and defines symbols for the server<->user communication channels.
(let [{:keys [ch-recv
              send-fn
              #?@(:clj
                  [ajax-post-fn
                   ajax-get-or-ws-handshake-fn
                   connected-uids]
                  :cljs
                  [chsk
                   state])]}
      #?(:clj
         (sente/make-channel-socket! sente-web-server-adapter
                                     packer-payload)
         :cljs
         (sente/make-channel-socket! communication-path
                                     (merge packer-payload
                                            {:type :auto
                                             :wrap-recv-evs? false})))]
  ;; common API for both languages
  (defonce receive  ch-recv)
  (defonce send!    send-fn)
  #?@(:clj
      [(defonce connected-uids connected-uids)
       (defonce ring-ajax-post ajax-post-fn)
       (defonce ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)]
      :cljs
      [(defonce status  state)
       (defonce connection chsk)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; communication contract ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; contract that the server and its user/clients will use to interact
;; with each other. This is used in order to avoid typos and to catch
;; at compile time errors in building the communication messages.

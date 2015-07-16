(ns carpet.core
  "Client side of the app. Dispatches to the correct om component based on the
  current component root name."
  (:require-macros
   [cljs.core.async.macros  :as asyncm :refer [go-loop]])
  (:require
   [reagent.core            :as reagent]
   [clojure.string          :as str]
   [cljs.core.async         :as async   :refer [timeout]]
   [taoensso.encore         :as log]
   [taoensso.sente          :as sente   :refer [cb-success?]]
   [carpet.router           :as router  :refer [event-msg-handler]]
   [carpet.communication    :as comm]
   [carpet.login            :as login]
   [carpet.notification     :as notification]))

(log/debugf "ClojureScript appears to have loaded correctly.")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic sente client side events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod event-msg-handler :chsk/state
  ;; Indicates when Sente is ready client-side.
  [{:as ev-msg :keys [?data]}]
  (if (= ?data {:first-open? true})
    (log/debugf "Channel socket successfully established!")
    (log/debugf "Channel socket state change: %s" ?data)))

(defmethod event-msg-handler :chsk/recv
  ;; default custom push event
  [{:as ev-msg :keys [?data]}]
  (log/debugf "Push event from server: %s" ?data))

(defmethod event-msg-handler :chsk/handshake
  ;; handshake for WS
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (log/debugf "Handshake: %s" ?data)))

;;;;;;;;;;;;;;;;;;;;
;; custom events  ;;
;;;;;;;;;;;;;;;;;;;;

;; none yet

;;;;;;;;;;;;;
;; Root UI ;;
;;;;;;;;;;;;;

(defn- application
  "Renders a view based on the current session state."
  []
  [:div
   [notification/panel]
   [login/form]])

(defn main []
  (router/start!)
  (reagent/render-component [application]
                            (. js/document (getElementById "app"))))

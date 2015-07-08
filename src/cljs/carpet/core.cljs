(ns carpet.core
  "Client side of the app. Dispatches to the correct om component based on the
  current component root name."
  (:require-macros
   [cljs.core.async.macros  :as asyncm :refer (go go-loop)])
  (:require
   [om.core                 :as om :include-macros true]
   [clojure.string          :as str]
   [cljs.core.async         :as async   :refer (<! >! put! chan)]
   [taoensso.encore         :as enc     :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente          :as sente   :refer (cb-success?)]
   [carpet.router           :as router  :refer (event-msg-handler start!)]
   [carpet.communication    :as comm]
   [carpet.login            :as login]))

;;;; Client-side setup

(debugf "ClojureScript appears to have loaded correctly.")

(defonce app-state (atom {}))

(defmethod event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (= ?data {:first-open? true})
    (debugf "Channel socket successfully established!")
    (debugf "Channel socket state change: %s" ?data)))

(defmethod event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (debugf "Push event from server: %s" ?data))

(defmethod event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (debugf "Handshake: %s" ?data)))

;; Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here...

;;;; Client-side UI

(defn main []
  "Dispatches based on the component root. See carpet.router for a
  documentation of the component root concept."
  (let [component-el
        (aget (. js/document (querySelectorAll router/root-element-selector)) 0)
        component-root-name (. component-el -className)]
    (om/root
     ;; dispatch to the application function
     (condp = component-root-name
       router/login-component-name login/maker
       ;; default
       (do (js/console.error "Unknown component: " component-root-name)
           login/maker))
     app-state
     {:target component-el})))

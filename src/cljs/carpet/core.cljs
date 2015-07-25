(ns carpet.core
  "Client side of the app. Dispatches to the correct om component based on the
  current component root name."
  (:require
   [reagent.core            :as reagent]
   [clojure.string          :as str]
   [cljs.core.async         :as async   :refer [timeout]]
   [taoensso.encore         :as log]
   [taoensso.sente          :as sente   :refer [cb-success?]]
   [carpet.router           :as router  :refer [message-handler]]
   [carpet.communication    :as comm]
   [carpet.login            :as login]
   [carpet.dashboard        :as dashboard]
   [carpet.session          :as session]))

(log/debugf "ClojureScript appears to have loaded correctly.")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic sente client side events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod message-handler :chsk/state
  ;; Indicates when Sente is ready client-side.
  [{:keys [?data]}]
  (if (= ?data {:first-open? true})
    (log/debugf "Channel socket successfully established!")
    (log/debugf "Channel socket state change: %s" ?data)))

(defmethod message-handler :chsk/handshake
  ;; handshake for WS
  [{:keys [?data]}]
  (let [[?uid] ?data]
    (log/debugf "Handshake done for: %s" ?uid)))

;;;;;;;;;;;;;
;; Root UI ;;
;;;;;;;;;;;;;

(defn- application
  "Renders a view based on the current session state."
  []
  [:div
   (if (session/alive?)
     [dashboard/main]
     [login/main])])

(defn main []
  (router/start!)
  (reagent/render-component [application]
                            (. js/document (getElementById "app"))))

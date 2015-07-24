(ns carpet.router
  "This is the common parts of routing and handling the channel's messages and
  the different app view components."
  (:require [taoensso.sente :as sente]
            #?(:clj
               [taoensso.timbre :refer (debugf)]
               :cljs
               [taoensso.encore :refer (debugf)])
            [carpet.communication :as comm]))

;;;;;;;;;;;;;;;;;;;;;;;;
;; root msg handlers  ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :default
  #?@(:clj
      [[{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
       (let [session (:session ring-req)
             uid     (:uid     session)]
         (debugf "Unhandled event in server: %s with session %s" event session)
         (when ?reply-fn
           (?reply-fn {:umatched-event-as-echoed-from-from-server event})))]
      :cljs
      [[{:as ev-msg :keys [event]}]
       (debugf "Unhandled event: %s" event)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; application message handlers  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; these are used in order to separate handling messages that are
;; related to establishing the channels (events) from handling the
;; messages that come through these channels

(defmulti application-msg-handler :id)

(defmethod application-msg-handler :default
  [{:keys [id data]}]
  (debugf "Unhandled application message: %s" id))

;;;;;;;;;;;;;;;
;; interface ;;
;;;;;;;;;;;;;;;

(defonce router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start!
  "Installs a wrapper over the event-msg-handler"
  []
  (stop-router!)
  (reset! router_
          (sente/start-chsk-router! comm/receiver event-msg-handler)))

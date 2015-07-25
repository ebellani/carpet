(ns carpet.router
  "Provides a root handler with a default behavior for the channel's
  messages. Also installs this root handler as the default router for sente."
  (:require [taoensso.sente :as sente]
            [#?(:clj
                taoensso.timbre
                :cljs
                taoensso.encore) :refer (debugf)]
            [carpet.communication :as comm]))

;;;;;;;;;;;;;;;;;;;;;;;;
;; root msg handlers  ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti message-handler :id)

(defmethod message-handler :default
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

;;;;;;;;;;;;;;;;;;;;;;
;; router interface ;;
;;;;;;;;;;;;;;;;;;;;;;

(defonce ^:private
  router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start!
  "Installs a wrapper over the message-handler"
  []
  (stop-router!)
  (reset! router_
          (sente/start-chsk-router! comm/receive message-handler)))

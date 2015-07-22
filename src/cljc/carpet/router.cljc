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

(defmulti event-msg-handler :id) ; Dispatch on event-id

(defn event-msg-handler-wrapper [{:as ev-msg :keys [id ?data event]}]
  "Wrapper of the event-msg-handler, emulating something like CLOS :around method combination[1].
[1] http://www.aiai.ed.ac.uk/~jeff/clos-guide.html#meth-comb"
  (debugf "Event: %s" event)
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default ; Fallback
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
          (sente/start-chsk-router! comm/receiver
                                    event-msg-handler-wrapper)))

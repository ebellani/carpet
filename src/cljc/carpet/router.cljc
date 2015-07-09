(ns carpet.router
  "This is the common parts of routing and handling the channel's messages and
  the different app view components."
  (:require [taoensso.sente :as sente]
            #?(:clj
               [taoensso.timbre :refer (debugf)]
               :cljs
               [taoensso.encore :refer (debugf)])
            [carpet.communication :as comm]))

;;;; Routing handlers

;;; So you'll want to define one server-side and one client-side (fn
;;; event-msg-handler [ev-msg]) to correctly handle incoming
;;; events. How you actually do this is entirely up to you. In this
;;; example we use a multimethod that dispatches to a method based on
;;; the `event-msg`'s event-id. Some alternatives include a simple
;;; `case`/`cond`/`condp` against event-ids, or `core.match` against
;;; events.

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
         (debugf "Unhandled event: %s" event)
         (when ?reply-fn
           (?reply-fn {:umatched-event-as-echoed-from-from-server event})))]
      :cljs
      [[{:as ev-msg :keys [event]}]
       (debugf "Unhandled event: %s" event)]))

;;;; Init

#?(:clj
   (defonce router_ (atom nil))
   :cljs
   (def router_ (atom nil)))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start!
  "Installs a wrapper over the event-msg-handler"
  []
  (stop-router!)
  (reset! router_
          (sente/start-chsk-router! comm/ch-chsk
                                    event-msg-handler-wrapper)))

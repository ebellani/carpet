(ns carpet.core
  "This is the client part in the study about using channels to create the
  communication between client <-> server."
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require
   [clojure.string         :as str]
   [cljs.core.async        :as async   :refer (<! >! put! chan)]
   [taoensso.encore        :as enc     :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente         :as sente   :refer (cb-success?)]
   [carpet.router         :as router  :refer (event-msg-handler start!)]
   [carpet.communication  :as comm]))

;;;; Client-side setup

(debugf "ClojureScript appears to have loaded correctly.")

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

(when-let [target-el (.getElementById js/document "btn1")]
  (.addEventListener
   target-el
   "click"
   #(do
      (debugf "Button 1 was clicked (won't receive any reply from server)")
      (comm/chsk-send!
       [comm/button1 {}]))))

(when-let [target-el (.getElementById js/document "btn2")]
  (.addEventListener
   target-el
   "click"
   #(do
      (debugf
       "Button 2 was clicked (will receive reply from server)")
      (comm/chsk-send!
       [comm/button2 {:data-for-transfer 999999999}]
       5000
       (fn [cb-reply]
         (js/alert cb-reply)

         ;; (debugf "Callback reply: %s"
         ;;         cb-reply)
         )))))

(when-let [target-el (.getElementById js/document "btn-login")]
  (.addEventListener
   target-el
   "click"
   #(let [user-id (.-value (.getElementById js/document "input-login"))]
      (if (str/blank? user-id)
        (js/alert "Please enter a user-id first")
        (do
          (debugf "Logging in with user-id %s" user-id)
          ;; Use any login procedure you'd like. Here we'll trigger an Ajax
          ;; POST request that resets our server-side session. Then we ask
          ;; our channel socket to reconnect, thereby picking up the new
          ;; session.
          (sente/ajax-call
           "/login"
           {:method :post
            :params {:user-id    (str user-id)
                     :csrf-token (:csrf-token @comm/chsk-state)}}
           (fn [ajax-resp]
             (debugf "Ajax login response: %s" ajax-resp)
             (let [login-successful? true ; Your logic here
                   ]
               (if-not login-successful?
                 (debugf "Login failed")
                 (do
                   (debugf "Login successful")
                   (sente/chsk-reconnect! comm/chsk)))))))))))

(start!)

(ns carpet.server
  "This is the server part in the study about using channels to create the
  communication between client <-> server."
  (:require
   [ring.middleware.defaults]
   [clojure.string     :as str]
   [compojure.core     :as comp :refer [POST GET defroutes]]
   [compojure.route    :as route]
   [hiccup.core        :as hiccup]
   [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
   [taoensso.timbre    :as timbre :refer (tracef debugf infof warnf errorf)]
   [taoensso.sente     :as sente]
   [org.httpkit.server :as http-kit]
   [carpet.router     :as router :refer (event-msg-handler)]
   [carpet.communication    :as comm]
   [carpet.views      :as views]))

;; Logging config

;; (sente/set-logging-level! :trace) ; Uncomment for more logging

;; ---> Choose (uncomment) a supported web server and adapter <---

;;; http-kit
(defn start-web-server!* [ring-handler port]
  (println "Starting http-kit...")
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server  nil ; http-kit doesn't expose this
     :port    (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(defroutes my-routes
  (GET  "/"      req (views/landing-pg-handler req))
  ;;
  (GET  comm/communication-url  req (comm/ring-ajax-get-or-ws-handshake req))
  (POST comm/communication-url  req (comm/ring-ajax-post                req))
  (POST "/login" req (views/login! req))
  ;;
  (route/resources "/") ; Static files, notably public/main.js (our cljs target)
  (route/not-found "<h1>Page not found</h1>"))

(def my-ring-handler
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
                  {:read-token (fn [req] (-> req :params :csrf-token))})]
    ;; NB: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
    ;; middleware to work. These are included with
    ;; `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
    ;; that they're included yourself if you're not using `wrap-defaults`.
    ;;
    (ring.middleware.defaults/wrap-defaults my-routes
                                            ring-defaults-config)))

;; Server-side methods
(defmethod event-msg-handler :default ; Fallback
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (= ?data {:first-open? true})
    (debugf "Channel socket successfully established!")
    (debugf "Channel socket state change: %s" ?data)))

(defmethod event-msg-handler comm/button1
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (debugf "%s event: %s" comm/button1 event))

(defmethod event-msg-handler comm/button2
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (debugf "HELLO: %s" event)
  (when ?reply-fn
    (?reply-fn "XXXXXXXXXXX")))

;; Add your (defmethod event-msg-handler <event-id> [ev-msg] <body>)s here...


;;;; Carpet: broadcast server>user

;; As an carpet of push notifications, we'll setup a server loop to broadcast
;; an event to _all_ possible user-ids every 10 seconds:
(defn start-broadcaster! []
  (go-loop [i 0]
    (<! (async/timeout 10000))
    (println (format "Broadcasting server>user: %s" @comm/connected-uids))
    (doseq [uid (:any @comm/connected-uids)]
      (comm/chsk-send! uid
                       [:some/broadcast
                        {:what-is-this "A broadcast pushed from server"
                         :how-often    "Every 10 seconds"
                         :to-whom uid
                         :i i}]))
    (recur (inc i))))

;; Note that this'll be fast+reliable even over Ajax!:
(defn test-fast-server>user-pushes []
  (doseq [uid (:any @comm/connected-uids)]
    (doseq [i (range 100)]
      (comm/chsk-send! uid [:fast-push/is-fast (str "hello " i "!!")]))))

;;;; Init

(defonce web-server_ (atom nil)) ; {:server _ :port _ :stop-fn (fn [])}
(defn stop-web-server! [] (when-let [m @web-server_] ((:stop-fn m))))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map}
        (start-web-server!* (var my-ring-handler)
                            (or port 0) ; 0 => auto (any available) port
                            )
        uri (format "http://localhost:%s/" port)]
    (debugf "Web server is running at `%s`" uri)
    (reset! web-server_ server-map)))

(defn start! []
  (router/start! #((start-web-server!)
                   (start-broadcaster!))))

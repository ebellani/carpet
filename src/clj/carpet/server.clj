(ns carpet.server
  "This is the server part in the study about using channels to create the
  communication between client <-> server."
  (:require
   [ring.middleware.defaults]
   [clojure.string                  :as str]
   [compojure.core                  :as comp :refer [POST GET defroutes]]
   [compojure.route                 :as route]
   [hiccup.core                     :as hiccup]
   [clojure.core.async              :as async :refer [<! <!! >! >!! put! chan go go-loop]]
   [taoensso.timbre                 :as timbre :refer [tracef debugf infof warnf errorf]]
   [taoensso.timbre.appenders.core  :as appenders]
   [taoensso.sente                  :as sente]
   [org.httpkit.server              :as http-kit]
   [clojure.java.io                 :as io]
   [net.cgrand.enlive-html          :as html :refer [deftemplate]]
   [net.cgrand.reload :refer [auto-reload]]
   [carpet.router                   :as router :refer [event-msg-handler]]
   [carpet.communication            :as comm]
   [environ.core                    :refer [env]]
   [carpet.dev                      :as dev]))

;;;; Logging config

;; (:require [taoensso.timbre.appenders.core :as appenders]) ; Add to ns

;; move logging config to its own ns and use environments to config
;; it.
(timbre/merge-config!
  {:appenders {:spit (appenders/spit-appender {:fname "log/server.log"})}})

;;;; components

(defmacro define-component-root [component-definition-symbol component-name]
  "A component root is a page with a element with an id equal to the
  router/root-element-id. This serves as a dispatch point to the
  view."
  `(deftemplate ~component-definition-symbol (io/resource "index.html") []
     [:body] (if dev/is-dev? dev/inject-devmode-html identity)
     [(keyword ~router/root-element-selector)]
     (comp (html/add-class ~component-name)
           (html/remove-attr :class))))

(define-component-root login-component-root router/login-component-name)

(defroutes my-routes
  (GET comm/login-path req (login-component-root))
  ;;
  (GET comm/communication-path req
    (comm/ring-ajax-get-or-ws-handshake req))
  (POST comm/communication-path req
    (comm/ring-ajax-post req))
  ;;
  (route/resources "/") ; Static files, notably the cljs targets
  (route/not-found "not found"))

(def my-ring-handler
  "Adds an acessor that takes into consideration the custom csrf toke name."
  (let [ring-defaults-config
        (assoc-in ring.middleware.defaults/site-defaults
                  [:security :anti-forgery]
                  {:read-token (fn [req]
                                 (-> req :params comm/csrf-token-name))})]
    ;; NB: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
    ;; middleware to work. These are included with
    ;; `ring.middleware.defaults/wrap-defaults`
    (ring.middleware.defaults/wrap-defaults my-routes
                                            ring-defaults-config)))

;;; http-kit
(defn start-web-server!* [ring-handler port]
  (println "Starting http-kit...")
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server  nil ; http-kit doesn't expose this
     :port    (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

;;;; Server-side channel-connected methods

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
                            (or port (:port env) 10555))
        uri (format "http://localhost:%s/" port)]
    (debugf "Web server is running at `%s`" uri)
    (reset! web-server_ server-map)))

(defn start! []
  (router/start! #((start-web-server!)
                   (start-broadcaster!))))

(defn run-auto-reload []
  (auto-reload *ns*)
  (dev/start-figwheel))

(defn run [& [port]]
  (when dev/is-dev?
    (run-auto-reload))
  (start-web-server! port))

(defn -main [& [port]]
  (run port))

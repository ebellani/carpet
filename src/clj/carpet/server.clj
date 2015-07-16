(ns carpet.server
  "This is the server part in the study about using channels to create the
  communication between client <-> server."
  (:require
   [ring.middleware.defaults        :refer [wrap-defaults site-defaults]]
   [compojure.core                  :as comp :refer [POST GET defroutes]]
   [compojure.route                 :as route]
   [hiccup.core                     :as hiccup]
   [clojure.core.async              :as async :refer [<! <!! >! >!! put! chan go go-loop]]
   [taoensso.timbre                 :as log]
   [taoensso.timbre.appenders.core  :refer [spit-appender]]
   [taoensso.sente                  :as sente]
   [org.httpkit.server              :as http-kit]
   [clojure.java.io                 :as io]
   [net.cgrand.enlive-html          :as html :refer [deftemplate]]
   [net.cgrand.reload               :refer [auto-reload]]
   [environ.core                    :refer [env]]
   [buddy.auth.middleware           :refer [wrap-authentication]]
   [carpet.router                   :as router :refer [event-msg-handler]]
   [carpet.communication            :as comm]
   [carpet.auth                     :as auth]
   [carpet.dev                      :as dev]
   [carpet.request                  :as req]))

;;;;;;;;;;;
;; utils ;;
;;;;;;;;;;;

(defn log&return [level datum]
  (log/log level datum)
  datum)

;;;;;;;;;;;;
;; config ;;
;;;;;;;;;;;;

(log/merge-config!
  {:appenders {:spit (spit-appender {:fname "log/server.log"})}})

;;;;;;;;;;;;;;
;; handlers ;;
;;;;;;;;;;;;;;

(defn login
  [request]
  (let [user-name (get-in request [:params :user-name])]
    (log/info (format "'%s' attempting login" user-name))
    (if-let [token (auth/login user-name
                               (get-in request [:params :password]))]
      (req/ok (assoc token
                     :message
                     (log&return :info
                                 (format "'%s' login success"
                                         user-name))))
      (req/denied {:message
                   (log&return :info
                               (format "'%s' login failure"
                                       user-name))}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes and middleware ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftemplate root-template (io/resource "index.html") []
  [:body] (if dev/is-dev? dev/inject-devmode-html identity))

(defroutes routes
  (GET  "/" req (root-template))
  (POST comm/login-path [] login)
  ;;
  (GET comm/communication-path req
    (comm/ring-ajax-get-or-ws-handshake req))
  (POST comm/communication-path req
    (comm/ring-ajax-post req))
  ;;
  (route/resources "/") ; Static files, notably the cljs targets
  (route/not-found "not found"))

;; see https://github.com/funcool/buddy-auth/blob/master/examples/token-jws/src/authexample/web.clj

(def csrf-modified-defaults
  "Adds an acessor that takes into consideration the custom csrf toke name for
  channel/websocket communication."
  (let [ring-defaults-config
        (assoc-in site-defaults
                  [:security :anti-forgery]
                  {:read-token (fn [req]
                                 (-> req :params comm/csrf-token-name))})]
    ;; NB: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
    ;; middleware to work. These are included with
    ;; `ring.middleware.defaults/wrap-defaults`
    (wrap-defaults routes ring-defaults-config)))

(def app (-> csrf-modified-defaults
             (wrap-authentication auth/backend)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server-side channel-connected methods ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Add your (defmethod event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;;;;;;;;;;;;;;;;;
;; channel loops ;;
;;;;;;;;;;;;;;;;;;;

;; As a way of verifying push notifications, we'll setup a server loop
;; to broadcast an event to _all_ possible user-ids every 10 seconds:
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

;;;;;;;;;;
;; Init ;;
;;;;;;;;;;

(defonce web-server_ (atom nil)) ; {:server _ :port _ :stop-fn (fn [])}

(defn stop-web-server! [] (when-let [m @web-server_] ((:stop-fn m))))

(defn start-web-server!* [ring-handler port]
  (println "Starting http-kit...")
  (let [http-kit-stop-fn (http-kit/run-server ring-handler {:port port})]
    {:server  nil ; http-kit doesn't expose this
     :port    (:local-port (meta http-kit-stop-fn))
     :stop-fn (fn [] (http-kit-stop-fn :timeout 100))}))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map}
        (start-web-server!* (var app)
                            (or port (:port env) 10555))
        uri (format "http://localhost:%s/" port)]
    (log/debugf "Web server is running at `%s`" uri)
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

(ns carpet.server
  "This is the server part in the study about using channels to create the
  communication between client <-> server."
  (:require
   [ring.middleware.defaults        :refer [wrap-defaults site-defaults]]
   [ring.middleware.edn             :refer [wrap-edn-params]]
   [ring-ttl-session.core           :refer [ttl-memory-store]]
   [buddy.auth.middleware           :refer [wrap-authentication]]
   [taoensso.sente                  :as sente]
   [compojure.core                  :refer [POST GET PUT defroutes]]
   [compojure.route                 :as route]
   [clojure.core.async              :as async :refer [<! go-loop]]
   [taoensso.timbre                 :as log]
   [taoensso.timbre.appenders.core  :refer [spit-appender]]
   [org.httpkit.server              :as http-kit]
   [clojure.java.io                 :as io]
   [net.cgrand.enlive-html          :as html :refer [deftemplate]]
   [net.cgrand.reload               :refer [auto-reload]]
   [environ.core                    :refer [env]]
   [carpet.router                   :as router :refer [message-handler]]
   [carpet.communication            :as comm]
   [carpet.auth                     :as auth]
   [carpet.request                  :as req]
   [carpet.dev                      :as dev]))

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
  {:appenders {:spit (spit-appender {:fname (:log-file-name env)})}})

;;;;;;;;;;;;;;
;; handlers ;;
;;;;;;;;;;;;;;

(defn login
  "Used to authenticate an user and to create an associated uid. This uid will
  be used to send messages through the channels. See
  https://github.com/ptaoussanis/sente/issues/118#issuecomment-87378277 for some
  insight into logging with sente."
  [request]
  (let [{:keys [session params]} request
        {:keys [user-name]}      params]
    (log/infof "'%s' attempting login" user-name)
    (if-let [token (auth/login user-name
                               (get-in request [:params :password]))]
      (assoc (req/ok {:message (log&return :info
                                           (format "'%s' login success"
                                                   user-name))})
             :session (assoc session :uid user-name))
      (req/denied {:message
                   (log&return :info
                               (format "'%s' login failure"
                                       user-name))}))))

(defn logout
  [request]
  (let [session (:session request)
        uid     (:uid session)]
    (comm/send! uid [:session/destroy])  ; <- logs out of all sessions.
    (assoc (req/ok
            {:message (log&return :info (format "'%s' logout" uid))})
           :session (dissoc session :uid))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; routes and middleware ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftemplate root-template (io/resource "index.html") []
  [:body] (if dev/is-dev? dev/inject-devmode-html identity))

(defroutes routes
  (GET  "/" req (root-template))
  (POST comm/session-path req (login  req))
  (PUT  comm/session-path req (logout req))
  ;;
  (GET comm/communication-path req
    (comm/ring-ajax-get-or-ws-handshake req))
  (POST comm/communication-path req
    (comm/ring-ajax-post req))
  ;;
  (route/resources "/") ; Static files, notably the cljs targets
  (route/not-found "not found"))

;; see https://github.com/funcool/buddy-auth/blob/master/examples/token-jws/src/authexample/web.clj

(def session-store
  "In-memory session store with time-to-live keys (30 minutes).
  References:
  * https://github.com/ring-clojure/ring/wiki/Sessions
  * https://github.com/boechat107/ring-ttl-session"
  (ttl-memory-store (* 60 30)))

(def modified-site-defaults
  "Modifications to site-defaults:
  * Adds an acessor that takes into consideration the custom csrf toke name for
  channel/websocket communication.
  * Replaces the default in-memory session store by a memory-based TTL session 
  store."
  (let [ring-defaults-config
        (-> site-defaults
            (assoc-in [:security :anti-forgery]
                      {:read-token (fn [req]
                                     (-> req :params comm/csrf-token-name))})
            (assoc-in [:session :store] session-store))]
    ;; NB: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
    ;; middleware to work. These are included with
    ;; `ring.middleware.defaults/wrap-defaults`
    (wrap-defaults routes ring-defaults-config)))

(def app (-> modified-site-defaults
             (wrap-authentication auth/backend)
             wrap-edn-params))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; basic sente server side events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod message-handler :chsk/ws-ping
  [_]
  ;; nothing for now, just here to avoid polluting the logs.
  )

(defmethod message-handler :chsk/uidport-open
  [{:as ev-msg :keys [?data]}]
  (let [[?uid] ?data]
    (log/debugf "Port open for: %s" ?uid)))

(defmethod message-handler :chsk/uidport-close
  [{:as ev-msg :keys [?data]}]
  (let [[?uid] ?data]
    (log/debugf "Port close for: %s" ?uid)))

;;;;;;;;;;;;;;;;;;;
;; channel loops ;;
;;;;;;;;;;;;;;;;;;;

(defn start-btc-broadcaster! []
  "This loop sends a test BTC package to all connected users as a way
  to test push notifications."
  (go-loop [i 0]
    (<! (async/timeout 10000))
    ;; only logged users
    (doseq [uid (filter (fn [uid]
                          (not= uid ::sente/nil-uid))
                        (:any @comm/connected-uids))]
      (comm/send! uid
                  [:currency/broadcast
                   {:from :btc
                    :to   :usd
                    :quantity (rand 100)}]))
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

(defn start! [& [port]]
  (router/start!)
  (start-web-server! port)
  ;; (start-broadcaster!)
  )

(defn run-auto-reload []
  (auto-reload *ns*)
  (dev/start-figwheel))

(defn run [& [port]]
  (when dev/is-dev?
    (run-auto-reload))
  (start! port))

(defn -main [& [port]]
  (run port))

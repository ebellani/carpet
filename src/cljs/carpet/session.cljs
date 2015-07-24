(ns carpet.session
  "Module that holds the actual storage variable for the session token, and the
  process to create/destroy it."
  (:require [taoensso.sente        :as sente]
            [reagent.core          :refer [atom]]
            [carpet.communication  :as comm]
            [taoensso.sente        :as sente]
            [taoensso.encore       :as log]
            [carpet.notification   :as noty]
            [carpet.communication  :as comm]))

;;;;;;;;;;;;;;;;;;;;
;; state variable ;;
;;;;;;;;;;;;;;;;;;;;

(defonce ^:private token
  ;; JWS token for maintaining session state without server side
  ;; storage.
  (atom false))

;;;;;;;;;;;;;;;
;; operators ;;
;;;;;;;;;;;;;;;

(defn alive? []
  (not (false? @token)))

(defn try-login!
  "Attemps to create a new session by storing the token that the server sends in
  case of valid authentication. Based on option 1 of
  https://github.com/ptaoussanis/sente/issues/62#issuecomment-58790741. This
  also asks the channel socket to reconnect on success, thereby picking up the
  new session."
  [user-name password]
  (sente/ajax-call
   comm/session-path
   {:method :post
    :params {:user-name user-name
             :password  password
             :csrf-token (:csrf-token @comm/status)}}
   (fn [resp]
     (let [{:keys [success? ?content]} resp]
       (reset! token (if success?
                       (do (sente/chsk-reconnect! comm/connection)
                           (:token ?content))
                       false))
       (noty/add! (if success? "success" "danger")
                  (:message ?content))))))

(defn try-logout!
  "Actually communicates user termination to the server."
  []
  (reset! token false)
  (sente/ajax-call
   comm/session-path
   {:method :put
    :params {:csrf-token (:csrf-token @comm/status)}}
   (fn [resp]
     (let [{:keys [success? ?content]} resp]
       (when success? (sente/chsk-reconnect! comm/connection))
       (noty/add! (if success? "success" "danger")
                  (:message ?content))))))

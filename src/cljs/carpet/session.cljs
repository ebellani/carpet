(ns carpet.session
  "The login process is "
  (:require [reagent.core          :refer [atom]]
            [carpet.communication  :as comm]
            [taoensso.sente        :as sente]
            [taoensso.encore       :as log]
            [carpet.notification   :as noty]
            [carpet.communication  :as comm]))

;;;;;;;;;;;;;;;;;;;;
;; state variable ;;
;;;;;;;;;;;;;;;;;;;;

(defonce token
  ;; JWS token for maintaining session state without server side
  ;; storage.
  (atom false))

;;;;;;;;;;;;;;;
;; operators ;;
;;;;;;;;;;;;;;;

(defn create!
  "Attemps to create a new session by storing the token that the server sends in
  case of valid authentication. Based on option 1 of
  https://github.com/ptaoussanis/sente/issues/62#issuecomment-58790741"
  [user-name password]
  (log/debugf "Sending auth data for user: %s" user-name)
  (sente/ajax-call
   comm/login-path
   {:method :post
    :params {:user-name user-name
             :password  password
             :csrf-token (:csrf-token @comm/chsk-state)}}
   (fn [resp-map]
     (let [{:keys [success? ?status ?error ?content ?content-type]}
           resp-map]
       (log/debugf "resp-map %s" resp-map)
       (if success?
         (do
           (reset! token "the new token")
           (swap! noty/notifications
                  #(conj % (noty/make-notification "success"
                                                   "Login successful."))))
         (do (reset! token false)
             (swap! noty/notifications
                    #(conj % (noty/make-notification "danger"
                                                     "Login failed.")))))))))

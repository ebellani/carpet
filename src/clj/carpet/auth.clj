(ns carpet.auth
  "This namespace provides authentication and authorization. These terms are
  defined as[1]:

  Authentication is the process of ascertaining that somebody really is who he
  claims to be.

  Authorization refers to rules that determine who is allowed to do
  what. E.g. Adam may be authorized to create and delete databases, while Usama
  is only authorised to read.

  The two concepts are completely orthogonal and independent, but both are
  central to security design, and the failure to get either one correct opens up
  the avenue to compromise.

  In terms of web apps, very crudely speaking, authentication is when you check
  login credentials to see if you recognize a user as logged in, and
  authorization is when you look up in your access control whether you allow the
  user to edit, delete or create content.

  The backend for the authentication is JWS [2]

  NOTE: There is no authorization yet.

  [1] http://stackoverflow.com/a/6556548
  [2] http://funcool.github.io/buddy-auth/latest/#jws-token-stateless"
  (:require [environ.core               :refer [env]]
            [buddy.sign.jws             :as jws]
            [clj-time.core              :as time]
            [buddy.auth.backends.token  :refer [jws-backend]]
            [buddy.hashers              :as hashers]
            [carpet.storage             :as storage]
            [clojure.core.typed         :as t :refer
             [ann defalias HMap Keyword Map AnyInteger U]])
  (:import [org.joda.time DateTime Period]))

;;;;;;;;;;;
;; types ;;
;;;;;;;;;;;

(defalias SigningAlgorithm ':hs512)
(defalias AlgorithmPayload (HMap :mandatory {:alg SigningAlgorithm}))

(defalias Claim (HMap :mandatory {:user Keyword :exp  DateTime}))
(defalias SignedToken String)
(defalias TokenPayload (HMap :mandatory {:token SignedToken}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; remedial typing of external libraries ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; foreign libs need the full name

(ann ^:no-check environ.core/env
     (HMap :mandatory {:secret String} :complete? false))
(ann ^:no-check buddy.hashers/check
     [String String -> Boolean])
(ann ^:no-check clj-time.core/plus
     [DateTime Period -> DateTime])
(ann ^:no-check clj-time.core/now
     [-> DateTime])
(ann ^:no-check clj-time.core/seconds
     [AnyInteger -> Period])
(ann ^:no-check buddy.sign.jws/sign
     [Claim String AlgorithmPayload -> SignedToken])

;;;;;;;;;;;;;;;;;;;
;; configuration ;;
;;;;;;;;;;;;;;;;;;;

(ann jws-algorithm AlgorithmPayload)
(def ^:private jws-algorithm-payload {:alg :hs512})

(ann seconds-until-expiration AnyInteger)
(def ^:private seconds-until-expiration 3600)

(ann secret String)
(def ^:private secret (:secret env))

;;;;;;;;;;;;;;;;;;;;;
;; authentication  ;;
;;;;;;;;;;;;;;;;;;;;;

(ann match? [String String -> Boolean])
(defn- match?
  "Performs the actual matching of the passwords. Returns a boolean. Giving this
  function nil as CLEAR-PASSWORD is the same as a failed match. This ensures
  that a non existing user/password is treated the same as a failed password."
  [password-digest clear-password]
  (and clear-password
       (hashers/check clear-password
                      password-digest)))

(ann is-authorized? [String String -> Boolean])
(defn- is-authorized?
  "Vefifies if a given user with an USER-NAME is authorized to enter the
  system. Authorization for now is an all or nothing affair."
  [user-name clear-password]
  (if (some-> user-name
              storage/users
              (match? clear-password))
    true
    false))

(ann get-expiration! [-> DateTime])
(defn- get-expiration!
  "The bang (!) indicates not side effects, but a procedure instead of a
  function. This is because of the 'time/now' call."
  []
  (time/plus (time/now)
             (time/seconds seconds-until-expiration)))

(ann make-claim [String -> Claim])
(defn- make-claim
  "A claim is the content of the token. As per [1], the tokens are timed based,
  and the approach chosen here is storing the expiration time, for the reasons
  made in the article.
  [1] http://lucumr.pocoo.org/2013/11/17/my-favorite-database/#time-based-anchors"
  [user-name]
  {:user (keyword user-name)
   :exp  (get-expiration!)})

(ann make-token [Claim -> SignedToken])
(defn- make-token
  [claim]
  (jws/sign claim
            secret
            jws-algorithm-payload))

;;;;;;;;;;;;;;;
;; interface ;;
;;;;;;;;;;;;;;;

(ann login [String String -> (U TokenPayload false)])
(defn login
  "The unification of all of the authentication functions. This is the accessing
  point for authentication."
  [user-name clear-password]
  (if (is-authorized? user-name clear-password)
    {:token (-> user-name
                make-claim
                make-token)}
    false))

(t/tc-ignore
 (def backend
   (jws-backend {:secret secret
                 :options jws-algorithm-payload})))

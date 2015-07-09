(ns carpet.test.auth
  (:require [midje.sweet :refer :all]
            [carpet.auth :as auth]))

(fact @#'auth/seconds-until-expiration => 3600)

(facts "Logging in"
       (fact "the mock user is authorized"
             (@#'auth/is-authorized? "mock" "mock") => true)
       (fact "the mock user is logs in and receives a token"
             (:token (auth/login "mock" "mock")) => string?)
       (fact "an invalid user does is not authorized"
             (@#'auth/is-authorized? "invalid" "invalid") => false)
       (fact "an invalid user does not receive a token"
             (auth/login "invalid" "invalid") => false))

(ns carpet.test.request
  (:require [midje.sweet :refer :all]
            [carpet.request :as request]))

(fact (request/ok) =>
      {:status 200 :body {:message "Request successful"}})
(fact (request/ok {:message "Login successfull"}) =>
      {:status 200 :body {:message "Login successfull"}})

(fact (request/denied) =>
      {:status 400 :body {:message "Access denied"}})
(fact (request/denied {:message "Wrong auth credentials"}) =>
      {:status 400 :body {:message "Wrong auth credentials"}})

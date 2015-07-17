(ns carpet.test.request
  (:require [midje.sweet     :refer :all]
            [carpet.request  :as request]))

(fact (request/ok) =>
      {:status 200
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:message "Request successful"})})
(fact (request/ok {:message "Login successfull"}) =>
      {:status 200
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:message "Login successfull"})})

(fact (request/denied) =>
      {:status 400
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:message "Access denied"})})
(fact (request/denied {:message "Wrong auth credentials"}) =>
      {:status 400
       :headers {"Content-Type" "application/edn"}
       :body (pr-str {:message "Wrong auth credentials"})})

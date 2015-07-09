(ns carpet.storage
  "A poor man's in memory database.

  TODO: replace this with a proper storage mechanism. Eliminate this and replace
  it with an aspect oriented approach, where the entire app can store data, and
  some namespaces appropriated to each relation. Research good persistent
  storage possibilities. https://github.com/kendru/restful-clojure uses korma.
  "
  (:require [clojure.core.typed :as t :refer [ann defalias Map]]))

(ann user (Map String String))
(def users
  {;; password is 'mock'
   "mock" "bcrypt+sha512$adf5c14e3af69fc4b5477ca5$12$2432612431322462426d586849504c356168564e764f4c684a6e5139654e71324f494243654d3132796b484134656333456a4a50774438586d697032"})

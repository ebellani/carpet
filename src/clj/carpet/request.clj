(ns carpet.request
  "Semantic response wrappers for the server."
  (:require [clojure.core.typed :as t :refer
             [ann defalias HMap IFn]]))

;;;;;;;;;;;
;; types ;;
;;;;;;;;;;;

(defalias StatusBody (HMap :mandatory {:message String}
                           :complete? false))
(defalias Status (HMap :mandatory
                       {:status AnyInteger
                        :body   StatusBody}))
(defalias StatusMaker (IFn [-> Status]
                           [StatusBody -> Status]))
;;;;;;;;;;;;;;;;;;;;;
;; instantiators   ;;
;;;;;;;;;;;;;;;;;;;;;

(ann ok StatusMaker)
(defn ok
  ([] (ok {:message "Request successful"}))
  ([d] {:status 200 :body d}))

(ann denied StatusMaker)
(defn denied
  ([] (denied {:message "Access denied"}))
  ([d] {:status 400 :body d}))

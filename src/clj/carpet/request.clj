(ns carpet.request
  "Semantic EDN response wrappers for the server."
  (:require [clojure.core.typed :as t :refer
             [ann defalias HMap IFn Map AnyInteger]]))

;;;;;;;;;;;
;; types ;;
;;;;;;;;;;;

(defalias Headers (Map String String))
(defalias Status (HMap :mandatory
                       {:status  AnyInteger
                        :headers Headers
                        :body    String}))

(defalias StatusMaker (IFn [-> Status]
                           [Any -> Status]))
;;;;;;;;;;;;;;;;;;;;;
;; instantiators   ;;
;;;;;;;;;;;;;;;;;;;;;

(ann default-headers Headers)
(def default-headers {"Content-Type" "application/edn"})

(ann make [AnyInteger Headers Any -> Status])
(defn- make [code headers body]
  {:status  code
   :headers default-headers
   :body    (pr-str body)})

(ann ok StatusMaker)
(defn ok
  ([] (ok {:message "Request successful"}))
  ([body]
   (make 200 default-headers body)))

(ann denied StatusMaker)
(defn denied
  ([] (denied {:message "Access denied"}))
  ([body]
   (make 400 default-headers body)))

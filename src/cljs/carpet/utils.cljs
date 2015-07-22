(ns carpet.utils
  "Cross-cutting concerns.")

(defn get-event-value [e]
  (-> e .-target .-value))

(ns carpet.notification
  "Provides contextual feedback messages for typical user actions with alert
  boxes. See http://getbootstrap.com/components/#alerts "
  (:require [reagent.core         :refer [atom]]
            [cljs-uuid-utils.core :refer [make-random-uuid]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; data definition and state variable ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce notifications (atom '()))

;; TODO: add assertion for types
;; #{"success" "info" "warning" "danger"}
(defn- make-notification
  [type message]
  {:id      (make-random-uuid) ;; artificial ID to create a key
   :type    type
   :message message})

;;;;;;;;;;;;
;; config ;;
;;;;;;;;;;;;

(def ^:private max-notifications 3)

(def ^:private default-duration 5000)

;;;;;;;;;;;;;;;
;; operators ;;
;;;;;;;;;;;;;;;

(defn- dismiss-notification! [notification]
  (swap! notifications
         #(filter (fn [existing-notification]
                    (not= existing-notification notification))
                  %)))

(defn add! [type message]
  (swap! notifications #(conj % (make-notification type message))))

;;;;;;;;;;;;;;;;
;; components ;;
;;;;;;;;;;;;;;;;

(defn- dismissible-alert [{:keys [type message] :as notification}]
  (let [dismiss! #(dismiss-notification! notification)]
    (js/setTimeout dismiss! default-duration)
    [:div {:class (str "alert alert-dismissable alert-" type)}
     [:button
      {:type        "button"
       :class       "close"
       :aria-hidden "true"
       :on-click    dismiss!}
      "×"]
     message]))

(defn main
  []
  [:div {:class "notifications-panel"}
   (for [notification (take max-notifications @notifications)]
     ^{:key (:id notification)} [dismissible-alert notification])])

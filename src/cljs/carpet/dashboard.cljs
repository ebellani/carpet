(ns carpet.dashboard
  "Provides components for dealing with the dashboard, which is the main user
  facing part of the application."
  (:require [reagent.core         :refer [atom]]
            [taoensso.encore      :as log :refer [format]]
            [carpet.session       :as session]
            [carpet.notification  :as notification]
            [carpet.router        :refer [application-msg-handler]]))

;;;;;;;;;;;;;;;;;;;;
;; state variable ;;
;;;;;;;;;;;;;;;;;;;;

(defonce quotes
  (atom '()))

;;;;;;;;;;;;;;;;;;;;
;; custom events  ;;
;;;;;;;;;;;;;;;;;;;;

(defmethod application-msg-handler :currency/broadcast
  [{:keys [data]}]
  (swap! quotes #(conj % data)))

;;;;;;;;;;;;;;;;
;; components ;;
;;;;;;;;;;;;;;;;

(defn- user
  []
  (let [{:keys [from to quantity]} (first @quotes)]
    [:div {:class "col-sm-9 col-md-10 main"}
     [:h1 {:class "page-header"} "Dashboard"]
     [:div {:class "row placeholders"}
      [:div {:class "col-xs-6 col-sm-3 placeholder"}
       [:h4 "Quote"]
       [:span {:class "text-muted"} (format "%s -> %s : %s" from to quantity)]]]]))

(defn- body
  []
  [:div {:class "dashboard-body container-fluid"}
   [:div {:class "row"}
    [user]]])

(defn- navigation-bar
  []
  [:nav {:class "navbar-inverse navbar-fixed-top"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"}
     [:button {:type "button"
               :class "navbar-toggle collapsed"
               :data-toggle "collapse"
               :data-target "#navbar"
               :aria-expanded "false"
               :aria-controls "navbar"}
      [:span {:class "sr-only"} "Toggle navigation"]
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]
      [:span {:class "icon-bar"}]]
     [:a {:class "navbar-brand" :href "#"} "Project name"]]
    [:div {:id "navbar"
           :class "navbar-collapse collapse"
           :aria-expanded "false"}
     [:ul {:class "nav navbar-nav navbar-right"}
      [:li [:a {:href "#"} "Dashboard"]]
      [:li [:a {:href "#"} "Settings"]]
      [:li [:a {:href "#"} "Profile"]]
      [:li [:a {:href "#"
                :on-click session/try-logout!} "Logout"]]]
     [:form {:class "navbar-form navbar-right"}
      [:input {:type "text" :class "form-control" :placeholder "Search..."}]]]]])

(defn main
  "Builds up the dashboard based on the example found at
  http://getbootstrap.com/examples/dashboard/ "
  []
  [:div {:class "dashboard"}
   [notification/main]
   [navigation-bar]
   [body]])

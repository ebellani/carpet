(ns carpet.login
  "Provides functionality and templates for registering and logging in the
  application"
  (:require [om.core      :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn maker
  "Builds up a login form based on the snippet found at
  http://bootsnipp.com/snippets/featured/login-and-register-tabbed-form."
  [cursor owner]
  (om/component
   (html [:form {:class "form-signin"}
          [:h2 {:class "form-signin-heading"} "Please sign in"]
          [:label {:class "sr-only"
                   :for "input-email"} "Email address"]
          [:input {:type "email"
                   :id "inputEmail"
                   :class "form-control"
                   :placeholder "Email address"}]
          [:label {:for "input-password" :class "sr-only"} "Password"]
          [:input {:type "password"
                   :id "inputPassword"
                   :class "form-control"
                   :placeholder "Password"}]
          [:div {:class "checkbox"}]
          [:label
           [:input {:type "checkbox"
                    :value "remember-me"} "Remember me"]]
          [:button {:class "btn btn-lg btn-primary btn-block"
                    :type "submit"}  "Sign in"]])))

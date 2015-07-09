(ns carpet.login
  "Provides functionality and templates for registering and logging in the
  application. "
  (:require [reagent.core   :refer [atom]]
            [carpet.session :as session]))

(defn form
  "Builds up a login form based on the snippet at
  [1] http://bootsnipp.com/snippets/featured/login-and-register-tabbed-form"
  []
  (let [user-auth (atom {:user-name ""
                         :password  ""})
        value-acessor (fn [e]
                        (-> e .-target .-value))
        updater! (fn [key]
                   #(swap! user-auth
                           (fn [old] (assoc old key (value-acessor %)))))]
    (fn []
      [:form {:class "form-signin"
              :on-submit #(do (session/create! (:user-name @user-auth)
                                               (:password @user-auth))
                              ;; always suppress form submission
                              false)}
       [:h2 {:class "form-signin-heading"} "Please log in"]
       [:label {:class "sr-only"
                :for "input-email"} "Email address"]
       [:input {:type "input" ;; should be email
                :ref "user-name"
                :id "inputEmail"
                :class "form-control"
                :placeholder "User email"
                :value (:user-name @user-auth)
                :on-change (updater! :user-name)}]
       [:label {:for "input-password" :class "sr-only"} "Password"]
       [:input {:type "password"
                :ref "password"
                :id "inputPassword"
                :class "form-control"
                :placeholder "User password"
                :value (:password @user-auth)
                :on-change (updater! :password)}]
       [:button {:class "btn btn-lg btn-primary btn-block"
                 :type "submit"}  "Login"]])))

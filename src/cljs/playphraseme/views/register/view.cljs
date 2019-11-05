(ns playphraseme.views.register.view
  (:require [clojure.string :as string]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs.core.async :as async :refer [<! >! put! chan timeout]]
            [playphraseme.common.util :as util]
            [playphraseme.common.rest-api :refer [success? error? register-user]]
            [playphraseme.views.register.model :as model])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [re-frame-macros.core :as mcr :refer [let-sub]]))

(declare form-completed?)

(defn form-data []
  [@(rf/subscribe [::model/full-name])
   @(rf/subscribe [::model/email])
   @(rf/subscribe [::model/password])
   @(rf/subscribe [::model/confirm-password])])

(defn clear-error! []
  (rf/dispatch [::model/error-message nil]))

(defn on-register [e]
  (-> e .preventDefault)
  (clear-error!)
  (when (form-completed?)
    (let [[name email password] (form-data)]
      (go
        (let [res (<! (register-user name email password))]
          (if (success? res)
            (util/go-url! "/#/")
            (rf/dispatch [::model/error-message (-> res :body :error)]))))))
  false)

(defn valid-full-name? []
  (let [[full-name _ _] (form-data)]
    (cond (string/blank? full-name) false
          :else true)))

(defn invalid-full-name? []
  (let [[full-name _ _] (form-data)]
    (cond (string/blank? full-name) false
          :else false)))

(defn valid-email? []
  (let [[_ email _ _] (form-data)]
    (cond (string/blank? email) false
          :else true)))

(defn invalid-email? []
  (let [[_ email _ _] (form-data)]
    (cond (string/blank? email) false
          :else false)))

(defn valid-password? []
  (let [[_ _ password confirm-password] (form-data)]
    (cond (or (string/blank? password)
              (string/blank? confirm-password)) false
          :else (= password confirm-password))))

(defn invalid-password? []
  (let [[_ _ password confirm-password] (form-data)]
    (cond (or (string/blank? password)
              (string/blank? confirm-password)) false
          :else (not= password confirm-password))))

(defn form-completed? []
  (and (valid-email?) (valid-password?)))

(defn page []
  [:div.form-page
   [:form {:on-submit on-register}
    [:h1 "Register"]
    (when-let [error-message @(rf/subscribe [::model/error-message])]
      [:div.alert.alert-danger
       {:role "alert"} error-message])
    [:div.d-flex
     [:input.input {:type        "text" :id "input-name"
                    :on-change   (fn [e]
                                   (clear-error!)
                                   (rf/dispatch [::model/full-name (-> e .-target .-value)]))
                    :placeholder "Full Name"
                    :class       (util/class->str (when (valid-full-name?) :is-valid)
                                                  (when (invalid-full-name?) :is-invalid))
                    :auto-focus  true}]]
    [:div.d-flex
     [:input.input {:type        "email" :id "input-email"
                    :on-change   (fn [e]
                                   (clear-error!)
                                   (rf/dispatch [::model/email (-> e .-target .-value)]))
                    :placeholder "Email Address"
                    :class       (util/class->str (when (valid-email?) :is-valid)
                                                  (when (invalid-email?) :is-invalid))}]]
    [:div.d-flex
     [:input.input {:type        "password" :id "input-password"
                    :on-change   (fn [e]
                                   (clear-error!)
                                   (rf/dispatch [::model/password (-> e .-target .-value)]))
                    :class       (util/class->str (when (valid-password?) :is-valid)
                                                  (when (invalid-password?) :is-invalid))
                    :placeholder "Password"}]]
    [:div.d-flex
     [:input.input {:id          "input-confirm-password" :type "password"
                    :on-change   (fn [e]
                                   (clear-error!)
                                   (rf/dispatch [::model/confirm-password (-> e .-target .-value)]))
                    :class       (util/class->str (when (valid-password?) :is-valid)
                                                  (when (invalid-password?) :is-invalid))
                    :placeholder "Confirm Password"      }]]
    [:div.d-flex
     [:div.grow]
     [:button.form-button {:type "submit" :disabled (not (form-completed?))} "Register"]
     [:div.grow]]
    [:div.page-footer-links
     [:p.text-center "Already registered? " [:a {:href "/#/login"} "Sign in."]]]]]
  )

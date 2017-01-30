(ns reschedul2.views.users
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            ; [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [ajax.core :refer [GET POST]]
            [reschedul2.ajax :refer [load-interceptors!]]
            [reschedul2.handlers]
            [reschedul2.subscriptions]))
  ; (:import goog.History))

(defn users-page []
  [:div.content-wrapper
    [:section.content-header
      [:h2 "Users"]]
    [:section.content
      [:div.row
        [:div.col-md-4.col-xs-12
          [:div.row
            [:p
              "There are "
              (-> [@(rf/subscribe [:logged-in-user])]
                  (count))
              " users registered."]]]
        [:div.col-md-4.col-xs-12
          [:div.row
            [:p "There are 0 organizers registered."]]]
        [:div.col-md-4.col-xs-12
          [:div.row
            [:p "There are 0 admins registered."]]]]]])

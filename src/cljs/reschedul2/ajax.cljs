(ns reschedul2.ajax
  (:require [re-frame.core :refer [dispatch]]
            [ajax.core :as ajax]))

(defn local-uri? [{:keys [uri]}]
  (not (re-find #"^\w+?://" uri)))

(defn default-headers [request]
  (if (local-uri? request)
    ; (dispatch [:set-loading])
    (-> request
        (update :uri #(str js/context %))
        (update
          :headers ;#(merge {"x-csrf-token" js/csrfToken} %)))
          #(merge
            %
            {"Accept" "application/transit+json"
             "x-csrf-token" js/csrfToken})))
    request))

(defn response-defaults [response]
  ; (dispatch [:unset-loading])
  response)

(defn load-interceptors! []
  (swap! ajax/default-interceptors
         conj
         (ajax/to-interceptor {:name "default headers"
                               :request default-headers})))

(ns reschedul2.route-functions.permission.add-user-permission
  (:require [reschedul2.db.core :as db]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [ring.util.http-response :as respond]
            [taoensso.timbre :as timbre]))

(def permission-levels ["basic" "organizer" "admin"])

; (def permission "basic")
(defn add-user-permission
  "Create user permission"
  [id permission]
  (if (contains? (set permission-levels) permission)
    (do
      (if-let [added-permission (db/insert-permission-for-user! (.toString id) permission)]
        (respond/ok        {:message (format "Permission '%s' for user %s successfully added" permission id)})
        (respond/not-found {:error (format "Permission '%s' does not exist" permission)})))
    (respond/not-found {:error (format "Permission '%s' does not exist" permission)})))

(defn add-user-permission-response
  "Generate response for permission creation"
  [request id permission]
  (let [auth (get-in request [:identity :permission-level])]
    (if (.contains auth "admin")
      (add-user-permission id permission)
      (respond/unauthorized {:error "Not authorized"}))))

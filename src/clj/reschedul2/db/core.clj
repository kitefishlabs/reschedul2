(ns reschedul2.db.core
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :refer :all]
            [monger.result :refer [acknowledged?]]
            [mount.core :refer [defstate]]
            [buddy.hashers :as hashers]
            [reschedul2.config :refer [env]]
            ; [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [ring.util.http-response :refer [not-found]]
            [clj-time.local :as l]
            [monger.joda-time :refer :all])

  (:import (org.bson.types ObjectId)))
;
; Helpers
;

(defn warn-log
  "label is req'd at this point"
  [label payload]
  (timbre/warn (str "\n\n\n" label ":\n" payload "\n\n\n")))

(defn json-friendly [doc]
  (let [friendly-id (:_id doc)]
    (assoc doc :_id (.toString friendly-id))))

(defn mongo-friendly [doc]
  (if (string? (:_id doc))
    (assoc doc :_id (ObjectId. (:_id doc)))
    doc))


(defstate db*
  :start (-> env :database-url mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

;;;;;;;;;;;;;;;;;
; create-password-reset-key-table-if-not-exists! []
; create-permission-table-if-not-exists! []
; create-basic-permission-if-not-exists! []
; create-registered-user-table-if-not-exists! [])
; truncate-all-tables-in-database! [])
; create-user-permission-table-if-not-exists! [])

;;;;;;;;;;;;;;;;;

(defn clear-users! []
  (mc/drop db "users"))

(defn clear-permissions! []
  (mc/drop db "permission"))

(defn clear-user-permissions! []
  (mc/drop db "user_permission"))

(defn clear-password-reset-keys! []
  (mc/drop db "password-reset-key"))

(defn update-registered-user-password! [id hashedpass]
  (mc/update db "users" {:_id (mongo-friendly id)} {$set {:password hashedpass}}))

(defn get-password-reset-keys-for-userid [qmap]
  (mc/find-one-as-map db "password-reset-key" qmap))

(defn get-reset-row-by-reset-key [rkey]
  (mc/find-one-as-map db "password-reset-key" {:reset_key rkey}))

(defn insert-password-reset-key-with-default-valid-until! [rkey uid]
  (mc/insert db "password-reset-key" {:reset_key rkey
                                      :_id uid}))

(defn insert-password-reset-key-with-valid-until-date! [rkey uid valid]
  (mc/insert db "password-reset-key" {:reset_key rkey
                                      :_id uid
                                      :valid_until valid}))

(defn invalidate-reset-key! [rk]
  (mc/update db "users" {:reset_key rk} $set :already_used true))


(defn insert-permission! [perm]
  (mc/insert db "permission" {:permission perm}))



;
; REGISTERED USERS, GET++, POST, PATCH, DELETE
;

(defn get-all-registered-users []
  (mq/with-collection db "users"
    (mq/find {})
    ; (mq/fields [:_id])
    (mq/sort {:_id -1})))
    ; (mq/paginate :page 1 :per-page 10)))
    ;(mq/fields [:_id :email :username])
    ;(sort (array-map :username 1))
    ;(mq/paginate :page 1 :per-page 10)))

(defn get-registered-user-by-id [id]
  (mc/find-one-as-map db "users" {:_id (ObjectId. id)}))

(defn get-registered-user-by-username [uname]
  (mc/find-one-as-map db "users" {:username uname}))

(defn get-registered-user-by-email [eml]
  (mc/find-one-as-map db "users" {:email eml}))


(defn get-registered-user-details-by-username [uname]
  (get-registered-user-by-username uname))

(defn get-registered-user-details-by-email [email]
  (get-registered-user-by-email email))



(defn get-registered-user-details-by-refresh-token [rtkn]
  (let [refreshed (mc/find-one-as-map db "users" {:refresh_token rtkn})]
    (warn-log "refreshed:" refreshed)
    refreshed))



(defn insert-registered-user!
  [eml uname pass perm created-on]
  (mc/insert-and-return db "users" {:_id (ObjectId.)
                                    :email eml
                                    :username uname
                                    :password pass
                                    :permission perm
                                    :last-login created-on
                                    :created-at created-on}))

(defn update-registered-user-permission! [id new-perm]
  (timbre/warn (str "new perms: " new-perm))
  (let [reguser (get-registered-user-by-id (.toString id))]
    (mc/save-and-return
      db
      "users"
      (-> reguser
        (assoc :permission new-perm)
        (mongo-friendly)))))

(defn update-registered-user!
  [id new-email new-username new-password new-refresh-token]
  (let [orig-user (get-registered-user-by-id (.toString id))]
    (warn-log "URU" orig-user)
    (mc/save-and-return db "users"  (conj
                                      orig-user
                                       {:email new-email
                                        :username new-username
                                        :password new-password
                                        :refresh_token new-refresh-token}))))


(defn update-registered-user-password!
  [id new-password]
  (mc/update db "users" {:_id (ObjectId. id)} {$set {:password new-password}}))

(defn update-registered-user-refresh-token! [id refresh-token]
  (timbre/warn (str "update-registered-user-refresh-token: " id))
  (let [user-to-refresh (get-registered-user-by-id (.toString id))]
    (timbre/warn (str "\n\n\n user-to-refresh: " user-to-refresh "\n\n\n\n\n"))
    (timbre/warn (str "\n\n\n user-to-refresh: " (assoc-in user-to-refresh [:refresh_token] refresh-token) "\n\n\n\n\n"))
    (mc/save-and-return db "users"
      (assoc-in user-to-refresh [:refresh_token] refresh-token))))


(defn nullify-refresh-token! [rtkn]
  (let [user-to-nullify (get-registered-user-details-by-refresh-token rtkn)]
    (timbre/warn (str "user-to-nullify" user-to-nullify "\n\n\n"))
    (if (nil? user-to-nullify)
      nil
      (mc/save-and-return db "users"
        (assoc-in user-to-nullify [:refresh_token] 0)))))


(defn delete-registered-user! [id]
  (mc/remove db "user_permission" {:user_id id})
  (mc/remove db "users" {:_id (ObjectId. id)}))



(defn get-permission-for-user [uid]
  (warn-log "USER PERM GET" uid)
  (mc/find-one-as-map db "user_permission" {:user_id (.toString uid)}))


(defn insert-permission-for-user! [uid perm]
  (when-let [user-id (:_id (json-friendly (get-registered-user-by-id (.toString uid))))]
    (let [uidstr (.toString uid)
          user-perm (get-permission-for-user uidstr)
          new-perm {:_id (if (nil? user-perm) (ObjectId.) (:_id user-perm))
                    :user_id uidstr
                    :permission perm}]
      (warn-log "USER PERM INSERTED FOR ID" new-perm)
      {
        :user-perm (mc/save-and-return db "user_permission" new-perm)
        :user-updated (update-registered-user-permission!)})))


; insert-permission-for-username is the route to upgrade the perm, although this
; just creates a fresh ObjectId!!!
(defn update-permission-for-username! [uname perm]
  (when-let [user-id (:_id (json-friendly (get-registered-user-by-username uname)))]
    (let [user-perm (get-permission-for-user user-id)]
      (warn-log "USER PERM" user-perm)
      (mc/save-and-return db "user_permission"
        {:_id (if (nil? user-perm) (ObjectId.) (:_id user-perm))
         :user_id user-id
         :permission perm}))))


(defn delete-user-permission! [uid perm]
  (let [uidstr (.toString uid)]
    (do
      (mc/remove db "user_permission" {:user_id uidstr
                                       :permission perm})
      (insert-permission-for-user! uidstr "basic"))))


(defn seed-database! []
  (clear-permissions!)
  (clear-users!)
  (clear-user-permissions!)
  (insert-permission! "basic")
  (insert-permission! "organizer")
  (insert-permission! "admin")
  (insert-registered-user! "tms@kitefishlabs.com" "owner" (hashers/encrypt "pass") "basic" (l/local-now))
  (update-permission-for-username! "owner" "admin"))




; (defn get-users-stats []
;   {:unique-users (mc/count @db "users")
;    :admin-users (mc/count @db "users" {:admin true})
;    :unique-venues (mc/count @db "venues")
;    :unique-proposals (mc/count @db "propsal")})

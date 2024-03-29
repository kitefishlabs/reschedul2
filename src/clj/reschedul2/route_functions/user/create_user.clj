(ns reschedul2.route-functions.user.create-user
  (:require [reschedul2.db.core :as db]
            [buddy.hashers :as hashers]
            [ring.util.http-response :as respond]
            [taoensso.timbre :as timbre]
            [clj-time.local :as l]
            [monger.joda-time :refer :all]))

(defn create-new-user
  "Create user with `email`, `username`, `password`"
  [email username password]
  (let [hashed-password (hashers/encrypt password)
        new-user        (db/json-friendly (db/insert-registered-user! email username hashed-password "basic" (l/local-now)))
        inserted-perm   (db/insert-permission-for-user! (:_id new-user) "basic")]
    (respond/created "" new-user)))

; (create-new-user "EMAIL" "USER" "PASS")
; (db/get-registered-user-by-username "USER")
(defn create-user-response
  "Generate response for user creation"
  [email username password]
  (let [username-query   (db/get-registered-user-by-username username)
        email-query      (db/get-registered-user-by-email email)
        email-exists?    (not (nil? email-query))
        username-exists? (not (nil? username-query))]

    (cond
      (and username-exists? email-exists?) (respond/conflict {:error "Username and Email already exist"})
      username-exists?                     (respond/conflict {:error "Username already exists"})
      email-exists?                        (respond/conflict {:error "Email already exists"})
      :else                                (create-new-user email username password))))

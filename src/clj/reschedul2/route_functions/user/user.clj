(ns reschedul2.route-functions.user.user
  (:require [reschedul2.db.core :as db]
            [buddy.hashers :as hashers]
            [ring.util.http-response :refer :all]
            [taoensso.timbre :as timbre]
            ; [clj-time.core :as t]
            ; [clj-time.coerce :as c]
            [reschedul2.route-functions.common :refer [handler]]
            [schema.core :as s])
            ; [joda-time :as j])
  (import org.joda.time.DateTime))

(def User
  {:_id                               s/Str
   :username                          s/Str
   :email                             s/Str
   :permission                        s/Str
   (s/optional-key :fullname)         s/Str
   :last-login                        DateTime
   :created-at                        DateTime

   (s/optional-key :refresh_token)    s/Str
   

    ;  (s/optional-key :cell-phone)               s/Str
    ;  (s/optional-key :second-phone)             s/Str
    ;  (s/optional-key :email)                    s/Str
    ;  (s/optional-key :address)                  s/Str
    ;  (s/optional-key :preferred_contact_method) (s/enum :cell :email :second-phone)

    ;  (s/optional-key :client-ip)        s/Str
    ;  (s/optional-key :source-address)   s/Str

   (s/optional-key :password)         (s/maybe s/Str)
   (s/optional-key :password-confirm) (s/maybe s/Str)})

  ; (s/defschema NewUser (dissoc User :_id))
  ; (s/defschema UpdatedUser NewUser)

(def SearchResponse
 {(s/optional-key :users) [User]
  (s/optional-key :error) s/Str})

(def LoginResponse
 {(s/optional-key :user)  User
  (s/optional-key :error) s/Str})

(def LogoutResponse
 {:result s/Str})


(handler find-user [query type]
  (ok
    (db/json-friendly
      (if (= type "email")
        (db/get-registered-user-by-email query)
        (db/get-registered-user-by-username query)))))

(handler get-all-users []
 (ok
   (map db/json-friendly (db/get-all-registered-users))))


(defn warn-log
  "label is req'd at this point"
  [label payload]
  (timbre/warn (str "\n\n\n" label ":\n" payload "\n\n\n")))

(defn authenticate-local [userid pass]
 (when-let [user (db/get-registered-user-by-username userid)]
  (warn-log "auth-local" user)
  ; (warn-log "auth-local" (str pass " " (:password user)))
  (when (hashers/check pass (:password user))
    (dissoc user :password))))

(defn login [userid pass {:keys [session]}]                             ; remote-addr server-name
  (if-let [registered-user (authenticate-local userid pass)]
    (do
      (let [reguser-perm (db/get-permission-for-user (.toString (:_id registered-user)))]
        (do
          (warn-log "registered-user" registered-user)
          (warn-log "registered-user" reguser-perm)
          (->
            (ok {:user (db/json-friendly (assoc registered-user :permission (:permission reguser-perm)))})
            (assoc :session (assoc session :identity registered-user))))))
    (do
      (timbre/info "login failed for" userid)                                      ; remote-addr server-name)
      (unauthorized {:error "The username or password was incorrect."}))))

(handler logout []
  (assoc (ok {:result "ok"}) :session nil))

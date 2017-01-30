(ns reschedul2.routes.services
  (:require [ring.util.http-response :refer [ok not-found unauthorized]]
            [ring.util.http-status :as http-status]
            [compojure.api.sweet :refer :all]
            [compojure.api.meta :refer [restructure-param]]
            [schema.core :as s]
            [taoensso.timbre :as timbre]
            [reschedul2.db.core :as db]
            ; [reschedul2.routes.services.users :refer [User ContactInfo NewUser UpdatedUser]]
            [reschedul2.routes.services.auth :refer :all]
            [reschedul2.routes.services.password :refer :all]
            [reschedul2.routes.services.permission :refer :all]
            [reschedul2.routes.services.preflight :refer :all]
            [reschedul2.routes.services.refresh-token :refer :all]
            [reschedul2.routes.services.user :refer :all]
            ; [reschedul2.routes.services.venue :refer :all]
            ; [reschedul2.routes.services.proposals :refer :all]

            ; [reschedul2.middleware.basic-auth :refer [basic-auth-mw]]
            ; [reschedul2.middleware.token-auth :refer [token-auth-mw]]
            ; [reschedul2.middleware.cors :refer [cors-mw]]

            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]

            [reschedul2.route-functions.user.user :as u]))

(def service-routes
  (api
    {:swagger
      {:ui   "/api-docs"
       :spec "/swagger.json"
       :data {:info {:title "reschedul2"
                     :version "0.0.1"}

              ;  NO IDEA, is there a way to make this work???
              :securityDefinitions {"Bearer" {:type "apiKey" :name "Authorization" :in "header"}}

              :tags [{:name "Login"         :description "Login"}
                     {:name "Preflight"     :description "Return successful response for all preflight requests"}
                     {:name "User"          :description "Create, delete and update user details"}
                     {:name "Permission"    :description "Add and remove permissions tied to specific users"}
                     {:name "Refresh-Token" :description "Get and delete refresh-tokens"}
                     {:name "Auth"          :description "Get auth information for a user"}
                     ;  {:name "Password"      :description "Request and confirm password resets"}
                     {:name "User"          :description "Create, delete and update user details"}]}}}
                     ;  {:name "Venue"         :description "Create, delete and update venue details"}
                     ;  {:name "Proposal"      :description "Create, delete and update proposal details"}]}}}

    (POST "/api/login" req
      :return u/LoginResponse
      :body-params [userid :- s/Str
                    pass :- s/Str]
      :summary "user login handler"
      (u/login userid pass req))

    preflight-route
    user-routes
    permission-routes
    refresh-token-routes
    auth-routes
    ; password-routes
    user-routes))
    ; venue-routes
    ; proposal-routes))

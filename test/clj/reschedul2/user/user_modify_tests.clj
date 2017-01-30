(ns reschedul2.user.user-modify-tests
   (:require [clojure.test :refer :all]
             [reschedul2.handler :refer [app]]
             [reschedul2.test-utils :as helper]
             [reschedul2.db.core :as db]
             [buddy.hashers :as hashers]
             [cheshire.core :as ch]
             [ring.mock.request :as mock]
             [mount.core :as mount]
             [taoensso.timbre :as timbre]))

(defn setup-teardown [f]
  (try
    (db/clear-users!)
    (db/clear-permissions!)
    (db/clear-user-permissions!)
    (db/insert-permission! "basic")
    (helper/add-users)
    (is (= 2 (count (db/get-all-registered-users))))))
    ; (f)))

(use-fixtures :once
  (fn [f]
    (mount/start
      #'reschedul2.config/env
      #'reschedul2.db.core/db
      #'reschedul2.db.core/db*)
    (f)
    (mount/stop)))

(use-fixtures :each setup-teardown)


(deftest can-modify-a-users-username-with-valid-token-and-admin-permissions
  (testing "Can modify a users username with valid token and admin permissions"
    (let [user-id-1  (:_id (db/get-registered-user-by-username "JarrodCTaylor"))
          _          (db/insert-permission-for-user! user-id-1 "admin")
          response   ((app) (-> (mock/request :patch (str "/api/v1/user/" (.toString user-id-1)))
                                (mock/content-type "application/json")
                                (mock/body (ch/generate-string {:username "Newman"}))
                                (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body       (helper/parse-body (:body response))]
        (is (= 200         (:status response)))
        (is (= "Newman"    (:username body)))
        (is (= "j@man.com" (:email body))))))

(deftest can-modify-a-users-email-with-valid-token-and-admin-permissions
  (testing "Can modify a users email with valid token and admin permissions"
    (let [user-id-1    (:_id (db/get-registered-user-by-username "JarrodCTaylor"))
          _            (db/insert-permission-for-user! user-id-1 "admin")
          response     ((app) (-> (mock/request :patch (str "/api/v1/user/" (.toString user-id-1)))
                                  (mock/content-type "application/json")
                                  (mock/body (ch/generate-string {:email "new@email.com"}))
                                  (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body         (helper/parse-body (:body response))
          updated-user (future (db/get-registered-user-by-id (.toString user-id-1)))]

      (timbre/warn (str "updated-user: " @updated-user "\n\n\n"))

      (is (= 200             (:status response)))
      (is (= "JarrodCTaylor" (:username body)))
      (is (= "new@email.com" (:email body)))
      (is (= "new@email.com" (str (:email @updated-user)))))))

(deftest can-modify-a-users-password-with-valid-token-and-admin-permissions
  (testing "Can modify a users password with valid token and admin permissions"
    (let [user-id-1    (:_id (db/get-registered-user-by-username "JarrodCTaylor"))
          _            (db/insert-permission-for-user! user-id-1 "admin")
          _            (is (= "admin" (:permission (db/get-permission-for-user user-id-1))))
          response     ((app) (-> (mock/request :patch (str "/api/v1/user/" (.toString user-id-1)))
                                  (mock/content-type "application/json")
                                  (mock/body (ch/generate-string {:password "newPass"}))
                                  (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body         (helper/parse-body (:body response))
          updated-user (db/get-registered-user-by-id (.toString user-id-1))]
        (is (= 200  (:status response)))
        (is (= true (hashers/check "newPass" (:password updated-user)))))))

(deftest can-modify-your-own-password-with-valid-token-and-no-admin-permissions
  (testing "Can modify your own password with valid token and no admin permissions"
    (let [user-id-1    (:_id (db/get-registered-user-by-username "JarrodCTaylor"))
          response     ((app) (-> (mock/request :patch (str "/api/v1/user/" (.toString user-id-1))))
                              (mock/content-type "application/json")
                              (mock/body (ch/generate-string {:password "newPass"}))
                              (helper/get-token-auth-header-for-user "JarrodCTaylor:pass"))
          body         (helper/parse-body (:body response))
          updated-user (db/get-registered-user-by-id (.toString user-id-1))]
        (is (= 200  (:status response)))
        (is (= true (hashers/check "newPass" (:password updated-user)))))))

(deftest can-not-modify-a-user-with-valid-token-and-no-admin-permissions
  (testing "Can not modify a user with valid token and no admin permissions"
    (let [user-id-2        (:_id (db/get-registered-user-by-username "Everyman"))
          response         ((app) (-> (mock/request :patch (str "/api/v1/user/" (.toString user-id-2)))
                                      (mock/content-type "application/json")
                                      (mock/body (ch/generate-string {:email "bad@mail.com"}))
                                      (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body             (helper/parse-body (:body response))
          non-updated-user (db/get-registered-user-by-id (.toString user-id-2))]
        (is (= 401              (:status response)))
        (is (= "e@man.com"      (str (:email non-updated-user))))
        (is (= "Not authorized" (:error body))))))

(deftest trying-to-modify-a-user-that-does-not-exist-return-a-404
  (testing "Trying to modify a user that does not exist returns a 404"
    (let [user-id-1  (:_id (db/get-registered-user-by-username "JarrodCTaylor"))
          _          (db/insert-permission-for-user! (.toString user-id-1) "admin")
          response   ((app) (-> (mock/request :patch "/api/v1/user/ecf331ab814e54d588a21396")
                                (mock/content-type "application/json")
                                (mock/body (ch/generate-string {:email "not@real.com"}))
                                (helper/get-token-auth-header-for-user "JarrodCTaylor:pass")))
          body       (helper/parse-body (:body response))]
      (is (= 404                     (:status response)))
      (is (= "Userid does not exist" (:error body))))))

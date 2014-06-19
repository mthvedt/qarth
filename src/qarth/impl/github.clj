(ns qarth.impl.github
  "A Github oauth impl. Type is github.com."
  (require [qarth.oauth :as oauth]
           qarth.auth
           clj-http.client
           cheshire.core
           clojure.java.io))

(qarth.auth/derive :github.com :oauth)

(defmethod oauth/build :github.com
  [service]
  (assoc service
         :request-url "https://github.com/login/oauth/authorize"
         :access-url "https://github.com/login/oauth/access_token"))

(defmethod oauth/id :github.com
  [requestor]
  ; TODO maybe with open macro?
  (with-open [body (:body (requestor {:url "https://api.github.com/user"}))]
    (-> body clojure.java.io/reader cheshire.core/parse-stream (get "id"))))

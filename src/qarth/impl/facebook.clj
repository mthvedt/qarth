(ns qarth.impl.facebook
  "A Facebook oauth impl. Type is facebook.com."
  (require [qarth.oauth :as oauth]
           qarth.auth
           clj-http.client
           cheshire.core
           clojure.java.io))

(qarth.auth/derive :facebook.com :oauth)

(defmethod oauth/build :facebook.com
  [service]
  (assoc service
         :request-url "https://www.facebook.com/dialog/oauth"
         :access-url "https://graph.facebook.com/oauth/access_token"))

(defmethod oauth/id :facebook.com
  [requestor]
  (oauth/with-resp-reader
    [body requestor {:url "https://graph.facebook.com/me"}]
    (-> body cheshire.core/parse-stream (get "id"))))

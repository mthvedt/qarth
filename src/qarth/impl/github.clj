(ns qarth.impl.github
  "A Github oauth impl. Type is :github."
  (require (qarth [oauth :as oauth])
           qarth.impl.oauth-v2
           cheshire.core))

(oauth/derive :github :oauth)

(defmethod oauth/build :github
  [service]
  (assoc service
         :request-url "https://github.com/login/oauth/authorize"
         :access-url "https://github.com/login/oauth/access_token"))

(defmethod oauth/id :github
  [requestor]
  (-> {:url "https://api.github.com/user"}
    requestor :body cheshire.core/parse-string (get "id")))

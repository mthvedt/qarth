(ns qarth.impl.google
  "A Google oauth impl. Type is google.com.
  Default OAuth scope is \"openid email\" (Google requires an OAuth scope)."
  (require (qarth [oauth :as oauth])
           [qarth.oauth.lib :as lib]
           qarth.impl.oauth-v2
           cheshire.core
           clojure.java.io))

(oauth/derive :google.com :oauth)

(defmethod oauth/build :google.com
  [{scope :scope :as service}]
  (assoc service
         :scope (or scope "openid email")
         :request-url "https://accounts.google.com/o/oauth2/auth"
         :access-url "https://accounts.google.com/o/oauth2/token"))

(defn google-parser [body]
  (let [json (cheshire.core/parse-stream (clojure.java.io/reader body))
        {access-token "access_token" jwt "id_token"} json
        {email "email" userid "sub" exp "exp"} (lib/jwt-read jwt)]
    {:access-token access-token
     :userid userid
     :email email
     ; Not expires-in
     :expires exp}))

(defmethod oauth/activate :google.com
  [service record code]
  (lib/do-activate service record code (:access-url service) google-parser))

(defmethod oauth/requestor :google.com
  [service record]
  (let [super (oauth/requestor (assoc service :type :oauth) record)]
    (vary-meta
      (fn [{special-key ::special :as req}]
        ; Some information is stored in the record
        (if-let [r (if special-key (record special-key))]
          r
          (super req)))
      assoc :type :google.com)))

(defmethod oauth/id :google.com
  [requestor]
  (requestor {::special :userid}))

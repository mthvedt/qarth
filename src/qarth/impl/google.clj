(ns qarth.impl.google
  "A Google oauth impl. Type is google.com.
  Default OAuth scope is \"openid email\" (Google requires an OAuth scope)."
  (require [qarth.oauth :as oauth]
           cheshire.core
           [qarth.oauth.lib :as lib]
           clj-http.client))

; TODO test github zeroconf
(qarth.lib/derive :google.com :oauth)

; TODO make example
; TODO verifier nomenclature? Actually all nomenclature
(defmethod oauth/build :google.com
  [{scope :scope :as service}]
  (assoc service
         :scope (or scope "openid email")
         :request-url "https://accounts.google.com/o/oauth2/auth"
         :verify-url "https://accounts.google.com/o/oauth2/token"))

(defn google-parser [body]
  (let [json (cheshire.core/parse-string body)
        {access-token "access_token" jwt "id_token"} json
        {email "email" userid "sub" exp "exp"} (-> jwt lib/jwt-read second)]
    ; TODO: jwts can be validated. Since we're getting them from a
    ; trusted https callback, that's theoretically not neccesary for now,
    ; but would be nice as an extra security hurdle.
    ; See http://openid.net/specs/draft-jones-json-web-token-07.html#ExampleJWT
    ; and https://developers.google.com/accounts/docs/OAuth2Login#validatinganidtoken
    ; TODO: standard expires-in format
    {:access-token access-token
     :userid userid
     :email email
     :expires-in exp}))

; TODO maybe better as multimethod?
(defmethod oauth/verify :google.com
  [service record verifier]
  (lib/do-verify service record verifier (:verify-url service) google-parser))

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
  ; TODO url
  (requestor {::special :userid}))

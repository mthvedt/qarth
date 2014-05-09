(ns qarth.oauth.scribe
  "An implementation of OAuth for Scribe, with type :scribe.
  The build method requires a Scribe OAuth provider class, under the key
  :provider."
  (require [qarth.oauth :as oauth]
           [qarth.oauth.common :as c]))

(oauth/derive :scribe :any)

(defn scribe-verb-from-opts
  [opts]
  org.scribe.model.Verb/GET) ; TODO

(defn scribe-token
  "Convert a token to a Scribe object.
  Tokens are stored as Clojure objects so that they are serializable
  and edn readable/writable."
  [[key secret]]
  (org.scribe.model.Token. key secret))

(defn unscribe-token
  [scribe-token]
  "Convert a Scribe token to a plain old Clojure object."
  [(.getToken scribe-token) (.getSecret scribe-token)])

(defn build-from-java
  [service]
  "Create a OAuth service from a Scribe OAuth service. Type will be :scribe."
  {:service service
   :type :scribe})

(defmethod oauth/build :scribe
  [{:keys [api-key api-secret provider type] :as service}]
  (let [builder (-> (org.scribe.builder.ServiceBuilder.)
                  (.provider provider)
                  (.apiKey api-key)
                  (.apiSecret api-secret))]
    (if-let [c (:callback service)] (.callback builder c))
    (assoc (build-from-java (.build builder)) :type type)))

(defmethod oauth/request-session :scribe
  [{service-type :type service :service}]
  (let [request-token (.getRequestToken service)]
  {:type service-type
   :request-token (unscribe-token request-token)
   :csrf-token (c/csrf-token) ; TODO needed?
   :url (.getAuthorizationUrl service request-token)}))

(defmethod oauth/verify-session :scribe
  [{service :service} {request-token :request-token :as oauth-session} verifier-token]
  (let [access-token (->> verifier-token
                       (org.scribe.model.Verifier.)
                       (.getAccessToken service (scribe-token request-token))
                       unscribe-token)]
    (-> oauth-session
      (dissoc :csrf-token :url :request-token)
      (assoc :access-token access-token))))

(defmethod oauth/request-raw :scribe
  [{service :service} {access-token :access-token} url params opts]
  (let [params (or params {})
        opts (or opts {})
        req (org.scribe.model.OAuthRequest. (scribe-verb-from-opts opts) url)]
    (.signRequest service (scribe-token access-token) req)
    (-> req .send .getBody)))

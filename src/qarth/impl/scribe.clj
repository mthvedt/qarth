(ns qarth.impl.scribe
  "An implementation of OAuth for Scribe, with type :scribe.
  The build method requires a Scribe OAuth provider class, under the key
  :provider."
  (require [qarth.oauth :as oauth]
           [qarth.oauth.lib :as lib]
           clojure.java.io)
  (import [java.lang String Boolean]
          [org.scribe.model OAuthRequest Token]
          [org.scribe.oauth OAuthService])
  (use [slingshot.slingshot :only [try+ throw+]]))

(lib/derive :scribe :any)

(defn scribe-verb-from-opts
  [opts]
  (if-let [m (:method opts)]
    (case (.toLowerCase ^String (name m))
      "get" org.scribe.model.Verb/GET
      (throw+ (str "Unknown or unsupported method: " m)))
    org.scribe.model.Verb/GET))

(defn scribe-token
  "Convert a token to a Scribe object.
  Tokens are stored as Clojure objects so that they are serializable
  and edn readable/writable."
  [[key secret]]
  (Token. key secret))

(defn unscribe-token
  [^Token scribe-token]
  "Convert a Scribe token to a plain old Clojure object."
  [(.getToken scribe-token) (.getSecret scribe-token)])

(defn build-from-java
  [service]
  "Create a OAuth service from a Scribe OAuth service. Type will be :scribe."
  {:service service
   :type :scribe})

(defmethod oauth/build :scribe
  [{:keys [^String api-key ^String api-secret
           ^java.lang.Class provider type] :as service}]
  (let [builder (-> (org.scribe.builder.ServiceBuilder.)
                  (.provider provider)
                  (.apiKey api-key)
                  (.apiSecret api-secret))]
    (if-let [c (:callback service)] (.callback builder c))
    (assoc (build-from-java (.build builder)) :type type)))

(defmethod oauth/request-session :scribe
  [{service-type :type ^OAuthService service :service}]
  (let [request-token (.getRequestToken service)]
    {:type service-type
     :request-token (unscribe-token request-token)
     :csrf-token (lib/csrf-token) ; TODO needed?
     :url (.getAuthorizationUrl service request-token)}))

(defmethod oauth/verify-session :scribe
  [{^OAuthService service :service}
   {request-token :request-token :as oauth-session} verifier-token]
  (let [access-token (->> verifier-token
                       (org.scribe.model.Verifier.)
                       (.getAccessToken service (scribe-token request-token))
                       unscribe-token)]
    (-> oauth-session
      (dissoc :csrf-token :url :request-token)
      (assoc :access-token access-token))))

(defmethod oauth/request-raw :scribe
  [{^OAuthService service :service service-type :type}
   {access-token :access-token} opts]
  (let [req (OAuthRequest. (scribe-verb-from-opts opts) (:url opts))]
    (if-let [body (:body opts)]
      (.addPayload req ^String (slurp body))
      (if-let [form-params (:form-params opts)]
        (doseq [[k v] form-params] (.addBodyParameter req k v))))
    (doseq [[k v] (:query-parameters opts)]
      (.addQuerystringParameter req k v))
    (doseq [[^String k ^String v] (:headers opts)]
      (.addHeader req k v))
    (.setFollowRedirects req (boolean (get opts :follow-redirects true)))
    (.signRequest service (scribe-token access-token) req)
    (let [resp (.send req)
          status (.getCode resp)
          _ (when-not (and (.isSuccessful resp) (lib/success? status))
              ; Don't use throw+, might expose api keys
              ; TODO custom exception
              (throw (java.lang.RuntimeException.
                       (str "Request failed for service "
                            service-type ", status " status ", request was "
                            (pr-str opts)))))]
      {:status status
       :body (.getStream resp)
       :headers (into {} (.getHeaders resp))})))

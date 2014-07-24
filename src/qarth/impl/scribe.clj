(ns qarth.impl.scribe
  "An implementation of OAuth for Scribe, with type :scribe.
  The build method requires a Scribe OAuth provider class, under the key
  :provider."
  (require (qarth [oauth :as oauth]
                  [util :as util])
           [qarth.oauth.lib :as lib]
           clojure.java.io
           clojure.string
           [clojure.tools.logging :as log])
  (import [java.lang String Boolean]
          [org.scribe.model OAuthRequest Token]
          [org.scribe.oauth OAuthService]))

(qarth.oauth/derive :scribe :oauth)
(qarth.oauth/derive :scribe-v1 :scribe)

(defn scribe-verb-from-opts
  [opts]
  (-> opts (:method "get")
    name clojure.string/upper-case org.scribe.model.Verb/valueOf))

(defn scribe-token
  "Convert a token to a Scribe object.
  Tokens are stored as Clojure objects so that they are serializable
  and edn readable/writable."
  [[token secret]]
  (Token. token secret))

(defn get-state
  "Get the state component of a Clojure Scribe token."
  [[token _]] token)

(defn unscribe-token
  [^Token scribe-token]
  "Convert a Scribe token to a plain old Clojure object."
  [(.getToken scribe-token) (.getSecret scribe-token)])

(defn build-from-java
  [service provider key]
  "Create a OAuth service from a Scribe OAuth service. Type will be :scribe."
  {:service service
   :type :scribe
   :api-key key
   :provider provider})

(defmethod oauth/build :scribe
  [{:keys [^String api-key ^String api-secret ^String callback
           ^String scope ^java.lang.Class provider type] :as service}]
  (let [builder (-> (org.scribe.builder.ServiceBuilder.)
                  (.provider provider)
                  (.apiKey api-key)
                  (.apiSecret api-secret)
                  (.callback (or callback "oob")))
        _ (if scope (.scope builder scope))
        scribe-service (build-from-java (.build builder) (.getName provider) api-key)]
    (merge service scribe-service)))

(defmacro extend-scribe
  "Derives an extension of the Scribe implementation,
  that builds a Scribe service with the provided type and api."
  ([type api] `(extend-scribe ~type :scribe ~api))
  ([type supertype api]
   `(do
      (qarth.oauth/derive ~type ~supertype)
      (defmethod oauth/build ~type
        [spec#]
        (-> spec#
          (assoc :type :scribe :provider ~api)
          oauth/build
          (assoc :type ~type))))))

(defmethod oauth/new-record :scribe
  [{service-type :type ^OAuthService oauth-service :service
    provider :provider api-key :api-key :as service}]
  (let [state (.getRequestToken oauth-service)]
    {:type service-type
     :state (unscribe-token state)
     :url (.getAuthorizationUrl oauth-service state)}))

(defmethod oauth/activate :scribe
  [{^OAuthService service :service}
   {access-token :access-token state :state :as record}
   code]
  (if access-token
    record
    (let [access-token (->> code
                         (org.scribe.model.Verifier.)
                         (.getAccessToken service (scribe-token state))
                         unscribe-token)]
      (lib/activate-record record {:access-token access-token}))))

(defmethod oauth/requestor :scribe
  [{^OAuthService service :service service-type :type} {access-token :access-token}]
  (vary-meta
    (fn [{:keys [url form-params query-params headers body follow-redirects as]
          :as opts}]
      (let [req (OAuthRequest. (scribe-verb-from-opts opts) url)]
        (if-not (contains? #{"string" "stream" nil} (and as (name as)))
          (throw (UnsupportedOperationException.
                   (str "Unsupported :as option: " as))))
        (if body
          ; Override body
          (.addPayload req ^String (slurp body))
          (doseq [[k v] form-params] (.addBodyParameter req k v)))
        (doseq [[k v] query-params] (.addQuerystringParameter req k v))
        (doseq [[^String k ^String v] headers] (.addHeader req k v))
        (.setFollowRedirects req (boolean (or follow-redirects true)))
        (.signRequest service (scribe-token access-token) req)
        (let [resp (.send req)
              status (.getCode resp)
              _ (when-not (and (.isSuccessful resp) (util/success? status))
                  (throw (ex-info
                           (str "Request failed for service "
                                service-type ", status " status ", request was "
                                (pr-str opts))
                           {::oauth/response-status status})))]
          {:status status
           :body (case (and as (name as))
                   "stream" (.getStream resp)
                   (nil "string") (with-open [s (.getStream resp)] (slurp s)))
           :headers (into {} (.getHeaders resp))})))
    assoc :type service-type))

; For scribe impls using oauth-v1
(defmethod oauth/extract-code :scribe-v1
  [service record request]
  (lib/do-extract-code (-> record :state qarth.impl.scribe/get-state)
                       request :oauth_token :oauth_verifier :oauth_problem))

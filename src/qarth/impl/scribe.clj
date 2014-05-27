(ns qarth.impl.scribe
  "An implementation of OAuth for Scribe, with type :scribe.
  The build method requires a Scribe OAuth provider class, under the key
  :provider."
  (require (qarth [oauth :as oauth]
                  [lib :as lib])
           clojure.java.io
           [clojure.tools.logging :as log])
  (import [java.lang String Boolean]
          [org.scribe.model OAuthRequest Token]
          [org.scribe.oauth OAuthService]))

(lib/derive :scribe)

(defn scribe-verb-from-opts
  [opts]
  (if-let [m (:method opts)]
    (case (.toLowerCase ^String (name m))
      "get" org.scribe.model.Verb/GET
      (throw (java.lang.IllegalArgumentException.
               (str "Unknown or unsupported method: " m))))
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
  [service provider key]
  "Create a OAuth service from a Scribe OAuth service. Type will be :scribe."
  {:service service
   :type :scribe
   :api-key key
   :provider provider})

(defmethod oauth/build :scribe
  [{:keys [^String api-key ^String api-secret ^String callback
           ^java.lang.Class provider type] :as service}]
  (let [builder (-> (org.scribe.builder.ServiceBuilder.)
                  (.provider provider)
                  (.apiKey api-key)
                  (.apiSecret api-secret)
                  (.callback (or callback "oob")))
        scribe-service (build-from-java (.build builder) (.getName provider) api-key)]
    (merge service scribe-service)))

(defmethod oauth/extract-verifier :scribe
  [_ {[access-token _] :access-token}
   {{their-token :oauth-token verifier :oauth_verifier} :params}]
  (if (= their-token access-token)
    verifier
    (do
      (log/infof "Given token %s didn't match our token %s"
                 their-token access-token)
      nil)))

(defmethod oauth/new-record :scribe
  [{service-type :type ^OAuthService oauth-service :service
    provider :provider api-key :api-key :as service}]
  (let [request-token (.getRequestToken oauth-service)]
    (-> service
      (dissoc :service)
      (assoc :request-token (unscribe-token request-token)
             :url (.getAuthorizationUrl oauth-service request-token)))))

(defmethod oauth/active? :scribe
  [service {access-token :access-token}]
  (if access-token true false))

(defmethod oauth/verify :scribe
  [{^OAuthService service :service}
   {request-token :request-token :as record} verifier-token]
  (if (oauth/active? service record)
    record
    (let [access-token (->> verifier-token
                         (org.scribe.model.Verifier.)
                         (.getAccessToken service (scribe-token request-token))
                         unscribe-token)]
      (-> record
        (dissoc :url :request-token)
        (assoc :access-token access-token)))))

; TODO yahoo oauth_problem
(defmethod oauth/requestor :scribe
  [{^OAuthService service :service service-type :type} {access-token :access-token}]
  (fn [{:keys [url form-params query-params headers body follow-redirects] :as opts}]
    (let [req (OAuthRequest. (scribe-verb-from-opts opts) url)]
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
            _ (when-not (and (.isSuccessful resp) (lib/success? status))
                (throw (java.lang.RuntimeException.
                         (str "Request failed for service "
                              service-type ", status " status ", request was "
                              (pr-str opts)))))]
        {:status status
         :body (.getStream resp)
         :headers (into {} (.getHeaders resp))}))))

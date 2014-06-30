(ns qarth.oauth.lib
  "Helper fns for OAuth implementations."
  (require [qarth.auth :as auth]
           clj-http.client
           cheshire.core
           ring.util.codec
           clojure.data.codec.base64
           [clojure.tools.logging :as log]
           [clojure.java.io :as io]
           clojure.string))

(auth/derive :oauth)

(defn csrf-token
    "Returns a random base-64 encoded 12-byte CSRF token."
    []
    (String. (.getBytes (clojure.string/replace (crypto.random/base64 12) \/ \_))
                                   "UTF-8"))

(defn remove-nils
  "Remove nil keys from a map."
  [m]
  (reduce (fn [m [k v]] (if v (assoc m k v) m))
          {} m))

(defn do-new-record
  "Helper fn for new record suitable for standard OAuth v2.

  Returns a new record with these fields:
  :type -- same as the given service
  :state -- a random state token
  :url -- a GET-formatted url with standard OAuth v2 params

  URL params:
  :client_id -- our :api-key
  :state -- the state token
  :response_type -- \"code\"
  :redirect_uri (optional) -- the service :callback, if configured
  :scope (optional) -- the service :scope, if configured
  other params given by extra-params"
  [{type :type client_id :api-key request-url :request-url
    callback :callback scope :scope :as service}
   url extra-params]
  (let [state (csrf-token)
        extra-params (if (and scope (not (:scope extra-params)))
                       (assoc extra-params :scope scope)
                       extra-params)
        url-form {:client_id client_id
                  :state state
                  :response_type "code"
                  :redirect_uri (if (= callback "oob") nil callback)
                  :scope scope}
        url (->> url-form
              remove-nils
              (merge extra-params)
              ring.util.codec/form-encode
              (str url "?"))]
    {:type type :url url :state state}))

(defn do-extract-code
  "Helper fn for implementors to extract an auth code from a Ring request.
  Looks for the params in the givne Ring request.
  Returns the found auth code, nil if no state or code was found,
  or throws an exception if an error or token mismatch were found.

  our-state -- the state token in the record
  req -- the request
  state-fn -- finds the state token in the params
  code-fn -- finds the auth code in the params
  error-fn -- finds the error in the params

  In standard OAuth v2, state-fn, code-fn and error-fn are
  :state, :code and :error respectively."
  [our-state {params :params :as req} state-fn code-fn error-fn]
  (let [their-state (state-fn params)
        code (code-fn params)
        error (error-fn params)]
    (log/tracef "From params %s extracted state %s code %s error %s"
                (pr-str params) their-state code error)
    (cond
      error (-> "OAuth access request returned error"
              (str " " error)
              (ex-info {::auth/status 400}) throw)
      (not (or their-state code)) nil
      (= their-state our-state) code
      true (-> "Given state token %s didn't match our state token %s"
             (format their-state our-state)
             (ex-info {::auth/status 400}) throw))))

(defn- make-token-request
  "Helper fn for OAuth v2 services. Creates a Ring HTTP POST
  to the given url supplying :code, :client_id, and :client_secret
  params from the given service and auth code. This can be used with
  e.g. the clj-http library."
  [{key :api-key secret :api-secret callback :callback :as service} record
   code url additional-params]
  (let [form-params {:code code
                     :grant_type "authorization_code"
                     :client_id key
                     :client_secret secret}
        form-params (if callback
                      (assoc form-params :redirect_uri callback)
                      form-params)]
    {:method :post
     :url url
     :form-params (merge form-params additional-params)}))

(defn v2-json-parser
  "Parser for use with do-request-access. Reads a JSON-encoded response stream."
  [response-stream]
  (let [resp (cheshire.core/parse-stream (io/reader response-stream))
        {:keys [access_token token_type expires expires_in]} resp]
    (log/trace "Parsed JSON response" (pr-str resp))
    (remove-nils
      {:access-token access_token
       :token-type token_type
       :expires expires
       :expires-in expires_in})))

(defn v2-form-parser
  "Parser for use with do-request-access. Reads a form-encoded response stream."
  [response-stream]
  (let [resp (ring.util.codec/form-decode (slurp response-stream))]
    (log/trace "Parsed form response" (pr-str resp))
    (remove-nils
      {:access-token (get resp "access_token")
       :expires (get resp "expires")
       :expires-in (get resp "expires_in")
       :token-type (get resp "token_type")})))

(defn- url-base64-adjuster
  ;Adjuster for Base64 URL for decoding JWTs. See
  ;http://tools.ietf.org/html/draft-jones-json-web-signature-04
  [s]
  (let [s (-> s
            (clojure.string/replace \+ \-)
            (clojure.string/replace \/ \_))]
    (case (mod (.length s) 4)
      2 (.concat s "==")
      3 (.concat s "=")
      0 s)))

(defn- jwt-read-field [field]
  (-> field
    url-base64-adjuster
    (.getBytes "UTF-8")
    clojure.data.codec.base64/decode
    io/reader
    cheshire.core/parse-stream))

(defn jwt-read
  "Decodes a URL-encoded JSON Web Token and returns its body.
  Does not validate the JWT--use only if the JWT is trusted."
  [fields]
  (if fields
    (let [f2 (second (clojure.string/split fields #"\."))]
      ; TODO it's possible to verify the body.
      ; See http://openid.net/specs/draft-jones-json-web-token-07.html#ExampleJWT
      ; and https://developers.google.com/accounts/docs/OAuth2Login#validatinganidtoken
      (jwt-read-field f2))))

(defn activate-record
  "Helper fn for OAuth records. Dissocs :url and :state,
  and adds the given keys and values."
  [record param-map]
  (-> record
    (dissoc :url :state)
    (merge param-map)))

(defn do-activate
  "Helper fn for OAuth v2 services. Uses clj-http to execute a token request
  (described in make-token-request) and parses the response using the given parser fn.

  The parser should return a map containing at least the key :access-token
  and the optional key :expires-in. All keys will be added to the record.
  
  service -- the auth service
  record -- the auth record
  code -- the auth code
  url -- the access token url
  parser -- a fn response-stream -> map, should return at least an :access-token
  (be careful not to return access_token instead!)

  The response stream will be closed if it isn't closed already when done."
  [service record code url parser]
  (let [req (make-token-request service record code url {})
        req (merge {:as :stream} req)
        _ (log/debug "Requesting access token")
        _ (log/trace "Access token request: " (pr-str req))
        ^java.io.InputStream res (-> req clj-http.client/request :body)]
    (try
      (when-let [values (parser res)]
        (log/trace "Activating record with parsed values" (pr-str values))
        (activate-record record values))
      (finally
        (try
          (.close res)
          (catch Exception e))))))

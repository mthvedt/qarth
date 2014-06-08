(ns qarth.oauth.lib
  "Helper fns for OAuth implementations."
  (require clj-http.client
           ring.util.codec))
; TODO better error in invalid session than 'missing type for multi-service'
; maybe a verified? key in session

(qarth.lib/derive :oauth)

(defn csrf-token
    "Returns a random base-64 encoded 12-byte CSRF token."
    []
    (String. (.getBytes (clojure.string/replace (crypto.random/base64 12) \/ \_))
                                   "UTF-8"))

(defn do-new-record
  "Helper fn for new record suitable for standard OAuth v2.

  Returns a new record with these fields:
  :type -- same as the given service
  :request-token -- a random token
  :url -- a GET-formatted url with standard OAuth v2 params

  URL params:
  :client_id -- our :api-key
  :state -- the request token
  :response_type -- \"code\"
  :redirect_uri (optional) -- the service :callback, if configured
  :scope (optional) -- the service :scope, if configured
  other params given by extra-params"
  [{type :type client_id :api-key request-url :request-url
    callback :callback scope :scope :as service}
   url extra-params]
  (let [token (csrf-token)
        extra-params (if (and scope (not :scope extra-params))
                       (assoc extra-params :scope scope)
                       extra-params)
        url-form {:client_id client_id
                  :state token
                  :response_type "code"}
        url-form (if (or (not callback) (= callback "oob"))
                   url-form
                   (assoc url-form :redirect_uri callback))
        url-form (if scope (assoc url-form :scope scope) url-form)
        url (->> url-form
              (merge extra-params)
              ring.util.codec/form-encode
              (str url "?"))]
    {:type type :url url :request-token token}))

(defn do-extract-verifier
  "Helper fn for implementors to extract a verifier from a Ring request.
  Looks for the params in the givne Ring request.
  Throws an exception if an error or token mismatch was found; otherwise
  returns the found verifier.

  request-token -- must match the found token
  req -- the request
  token-fn -- finds the request token in the params
  verifier-fn -- finds the verifier in the params
  error-fn -- finds the error in the params

  In standard OAuth v2, token-fn, verifier-fn and error-fn are
  :state, :code and :error respectively."
  [request-token {params :params :as req} token-fn verifier-fn error-fn]
  (let [their-token (token-fn params)
        verifier (verifier-fn params)
        error (error-fn params)]
    (cond
      error (throw (RuntimeException. "OAuth verification returned error: %s" error))
      (not verifier) (throw (RuntimeException. "No verifier found"))
      (= their-token request-token) verifier
      true (->
             (str "Given token %s didn't match our token %s"
                  their-token request-token)
             RuntimeException. throw))))

(defn make-verify-request
  "Helper fn for OAuth v2 services. Creates a Ring HTTP POST
  to the given url supplying :code, :client_id, and :client_secret
  params from the given service and verifier. This can be used with
  e.g. the clj-http library."
  [{key :api-key secret :api-secret callback :callback :as service} record
   verifier url additional-params]
  (let [form-params {:code verifier
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
  ; TODO doc
  [response-stream]
  (prn response-stream)
  (let [resp (cheshire.core/parse-stream response-stream)
        {:keys [access_token token_type expires_in]} resp]
    {:access-token access_token
     :token-type token_type
     :expires-in expires_in}))

(defn activate
  "Helper fn for OAuth records. Dissocs :url and :request-token,
  and adds :access-token and the given additional params."
  [record access-token additional-params]
  (merge additional-params
         (-> record
           (dissoc :url :request-token)
           (assoc :access-token access-token))))

; TODO verify language? API?
(defn do-verify
  "Helper fn for OAuth v2 services. Uses clj-http to do a verify request
  (described in make-verify-request) and parses the response using the given parse-fn.
  The parse-fn should return a map containing at least the key :access-token,
  the optional key :expires-in, and any other keys you might want. Nil values
  will be ignored."
  [service record verifier url additional-params parser]
  (when-let [resp (-> service
                     (make-verify-request record verifier url additional-params)
                     clj-http.client/request
                     parser)]
    (activate record (:access-token resp) (dissoc resp :access-token))))

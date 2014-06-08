(ns qarth.oauth
  "Base fns for OAuth and OAuth-style interactive auth services.
  You can also define your own auth implementations--see the docs."
  ; TODO oauth-v2 namespace?
  ; TODO lib vs oauth.lib
  ; TODO debug, trace levels
  (require [qarth.lib :as l]
           [qarth.oauth.lib :as lib]
           clj-http.client
           qarth.oauth.lib))

(l/derive :multi :oauth)

(defmulti build 
  "Multimethod. Usage:
  (build {:type type :api-key my-key :api-secret my-secret ...})

  Create an auth service from a hash map specification.

  Mandatory fields:
  :type -- the type of the service; multimethods dispatch on this

  Usually required for OAuth:
  :api-key -- your API key. the [type, api-key] pair uniquely identifies a service
  :api-secret -- your API secret
  
  Implementations may accept other fields also, such as the following:
  :callback -- a callback URL for interactive browser logins
  
  Auth services contain secret information, like api secret keys.
  Be careful if about writing or serializing them."
  l/type-first :hierarchy l/h)

(defmethod build :multi
  [{services :services options :options :as spec}]
  {:type :multi
   :services
   (into {}
         (for [[k v] services]
           [k (build (merge options {:type k} v))]))})

(defmethod build :oauth [x] x)

(defmulti new-record
  "Multimethod. Usage:
  (new-record service)
  (new-record multi-service key)

  Returns an inactive OAuth record. An OAuth record keeps tracks of
  request tokens, verifiers, access tokens, CSRF, &c.

  Returns a map with:
  :type -- required, the type of the record
  :url -- optional, a callback URL for interactive auth
  other various implementation-specific keys"
  l/type-first :hierarchy l/h)

(defmethod new-record :oauth
  [{request-url :request-url :as service}]
  (lib/do-new-record service request-url {}))

(defmethod new-record :multi
  ([service] (throw (IllegalArgumentException. "Missing type for multi-service")))
  ([service key]
   (let [record (new-record (get-in service [:services key]))]
   {:type :multi :key key :record record :url (:url record)})))

(defmulti extract-verifier
  "Multimethod. Usage:
  (extract-verifier service record ring-request)

  Handles auth HTTP callbacks. Return a verifier token
  from the given Ring-formatted request. If the request was invalid,
  (perhaps due to a CSRF attack), it should return nil."
  l/type-first :hierarchy l/h)

(defmethod extract-verifier :multi
  [service {key :key record :record} req]
  (extract-verifier (get-in service [:services key]) record req))

(defmethod extract-verifier :oauth
  [_ {request-token :request-token} req]
  (lib/do-extract-verifier request-token req :state :code :error))

(defmulti verify
  "Multimethod. Usage:
  (verify service record verifier)
  Takes a new auth record and a verifier, and creates a verified, active
  auth record.
  If verification fails, can return nil or optionally
  throw an Exception of some kind.
  Idempotent--can be used on an already verified record."
  l/type-first :hierarchy l/h)

(defmethod verify :multi
  [service {key :key subrecord :record :as record} verifier]
  (assoc record :record
         (verify (get-in service [:services key]) subrecord verifier)))

(defmethod verify :oauth
  [{verify-url :verify-url :as service} record verifier]
  (lib/do-verify service record verifier
                 verify-url {} lib/v2-json-parser))

; TODO support exception toggling
(defmulti requestor
  "Multimethod. Usage:
  (let [r (requestor service record)]
    (r opts))

  Returns a fn that can be used to make requests to the auth service.

  Mandatory opt:
  :url -- the request URL

  Supported opts:
  :method -- :GET, :PUT, :POST, :DELETE, default :GET
  :body -- the HTTP body for PUT or POST. Overrides form-params.
  Can be anything usable by clojure.java.io (e.g. a String or byte array).
  :form-params -- the form parameters
  :query-params -- the query parameters
  :headers -- a string->string map
  :follow-redirects -- true or false, default true

  Returns a Ring-Style response map containing at least:
  :status -- the status code
  :headers -- the http headers
  :body -- an InputStream

  If an exceptional status code happens, throws an Exception instead.
  Other implementations might support more opts and return more stuff.
  
  A default implementation is provided for implementors. It adds the param
  :access_token to the form params if it's a POST, and the query if it's a GET."
  l/type-first :hierarchy l/h)

(defmethod requestor :multi
  [service {key :key record :record}]
  (requestor (get-in service [:services key]) record))

(defmethod requestor :oauth
  [_ {access-token :access-token type :type}]
  (if access-token
    (vary-meta
      (fn [req]
        (let [req (merge {:method :get} req)
              param-key (if (= (:method req) :post) :form-params :query-params)]
          (prn "req" (assoc-in req [param-key :access_token] access-token))
          (clj-http.client/request
            (assoc-in req [param-key :access_token] access-token))))
      assoc :type type)
    (throw (IllegalArgumentException. "Record is not verified"))))

(defmulti id
  "Multimethod. Optional. Usage:
  (id requestor)

  Gets a user ID from the requestor. The ID is guaranteed to be unique
  and unchanging per service."
  type :hierarchy l/h)

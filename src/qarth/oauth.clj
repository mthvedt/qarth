(ns qarth.oauth
  ; TODO user principals and fn
  ; TODO consider oauth -> auth
  "Base fns for auth. All auth implementations use these.
  You can also define your own auth implementations--see the docs."
  (require [qarth.oauth.support :as s]))

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
  s/type-first :hierarchy s/h)

(defmulti new-record
  "Multimethod. Usage:
  (new-record service)

  Returns an inactive OAuth record. An OAuth record keeps tracks of
  request tokens, verifiers, access tokens, CSRF, &c.

  All inactive records will have the key :url,
  which can be used to authorize records with 'verify-record.

  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

(defmulti extract-verifier
  "Multimethod. Usage:
  (extract-verifier service record ring-request)

  Handles auth HTTP callbacks. Return a verifier token
  from the given Ring-formatted request. If the request was invalid,
  (perhaps due to a CSRF attack), it should return nil.

  For implementation details, see the docs."
  s/type-first :hierarchy s/h :default :any)

(defmulti active?
  "Multimethod. Usage:
  (active? service record)
  
  Ture if the current auth record is verified and active.
  (Returns false if it is nil.)"
  s/type-first :hierarchy s/h)

(defmethod active? nil [_ _] false)

(defmulti verify
  "Multimethod. Usage:
  (verify service record verifier)
  Takes a new auth record and a verifier, and creates a verified, active
  auth record.
  If verification fails, can return nil or optionally
  throw an Exception of some kind.
  Idempotent--can be used on an already verified record.
  
  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

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
  Other implementations might support more opts and return more stuff."
  s/type-first :hierarchy s/h)

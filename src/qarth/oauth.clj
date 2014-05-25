(ns qarth.oauth
  ; TODO opts for all?
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

(defmulti new-session
  "Multimethod. Usage:
  (new-session service)

  Returns an inactive Qarth session.

  All inactive sessions will have the key :url,
  which can be used to authorize sessions with 'verify.

  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

(defmulti extract-verifier
  "Multimethod. Usage:
  (extract-verifier auth-service auth-session ring-request)

  Handles auth HTTP callbacks. Return a verifier token
  from the given Ring-formatted request. If the request was invalid,
  (perhaps due to a CSRF attack), it should return nil.

  For implementation details, see the docs."
  s/type-first :hierarchy s/h :default :any)

(defmulti active?
  "Multimethod. Usage:
  (active? service oauth-session)
  
  Ture if the current auth session is verified and active.
  (Returns false if it is nil.)"
  s/type-first :hierarchy s/h)

(defmethod active? nil [_ _] false)

(defmulti verify
  "Multimethod. Usage:
  (verify oauth-service oauth-session verifier)
  Creates a verified, active auth session from the given oauth session.
  If verification fails, can return nil or optionally
  throw a {:status 401} exception.
  Idempotent--can be used on an already verified session.
  
  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

(defmulti request-raw
  "Multimethod. Usage:
  (request-raw service session opts)

  Executes a HTTP request with an auth session.
  Does not parse the output.
  
  Mandatory opts:
  :url

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

  Implementations should throw an exception on exceptional status codes. 
  Other implementations might support more opts and return more stuff."
  s/type-first :hierarchy s/h)

(defmulti request
  "Multimethod with default implementation. Usage:
  (request service session opts)

  Executes a request with a verified auth session. Implementations
  might parse the output to a more usable form. The default implementation
  returns the body as a string."
  s/type-first :default :any :hierarchy s/h)

(defmethod request :any
  [service session opts]
  (-> (request-raw service session opts) :body slurp))

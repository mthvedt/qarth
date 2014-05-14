(ns qarth.oauth
  "Base fns for OAuth. All OAuth implementations use these.
  You can also define your own OAuth implementations--see the docs."
  (require [qarth.oauth.support :as s]
           [clojure.tools.logging :as log]))

(defmulti build 
  "Multimethod. Usage:
  (build {:type type :api-key my-key :api-secret my-secret ...})

  Create an OAuth service. Accepts a hash map.
  Mandatory fields:
  :type -- the type of the service; multimethods dispatch on this
  :api-key -- your API key. the [type, api-key] pair uniquely identifies a service
  :api-secret -- your API secret
  
  Implementations may accept other fields also, such as the following:
  :callback -- a callback URL for interactive browser logins
  
  OAuth services contain secret information, like api secret keys,
  and should probably not be read, written or serialized."
  s/type-first :hierarchy s/h)

; TODO API?
; Inactive session
; Activate session
; Deactivate session
(defmulti new-session
  "Multimethod. Usage:
  (new-session service)

  Returns an inactive OAuth session.

  All inactive sessions will have the key :url,
  which can be used to authorize sessions with verify-session.

  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

; TODO API
(defmulti extract-token
  "Multimethod with default implementation. Usage:
  (extract-token oauth-service ring-request)

  Handles OAuth HTTP callbacks. Return a verifier token
  from the given Ring-formatted request. If the request was invalid,
  it can return nil, or optionally throw+ a {:status 401} exception.

  For implementation details, see the docs."
  s/type-first :hierarchy s/h :default :any)

(defmethod extract-token :any
  [_ {{their-token :auth_token verifier :oauth_verifier} :params
      {our-token ::auth_token} :session
      :as ring-request}]
  (if (= our-token their-token)
    verifier
    (do
      (log/infof "Given auth_token %s did not match session auth_token"
                 their-token)
      nil)))

(defmulti is-active?
  "Multimethod. Usage:
  (is-active? oauth-session)
  
  Ture if the current OAuth session is active. (False if it is null.)"
  s/type-first :hierarchy s/h)

(defmethod is-active? nil [_] false)

; TODO handle scribe exceptions, if they exist
(defmulti verify-session
  "Multimethod. Usage:
  (verify-session oauth-service oauth-session verifier)
  Creates an active OAuth session from the given oauth session.
  If verification fails, can return nil or optionally
  throw a {:status 401} exception.
  
  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

(defmulti request-raw
  "Multimethod. Usage:
  (request-raw service session opts)

  Executes an OAuth request in an OAuth session.
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

  Executes an OAuth request in an OAuth session. Implementations
  might parse the output to a more usable form. The default implementation
  returns the body as a string."
  s/type-first :default :any :hierarchy s/h)

(defmethod request :any
  [service session opts]
  (-> (request-raw service session opts) :body slurp))

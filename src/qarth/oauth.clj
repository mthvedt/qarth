(ns qarth.oauth
  "Base fns for OAuth. All OAuth implementations use these.
  You can also define your own OAuth implementations--see the docs."
  (require [qarth.oauth.support :as s]))

(defmulti build 
  "Usage:
  (build {:type type :api-key my-key :api-secret my-secret ...})

  Create an OAuth service. Accepts a hash map.
  Mandatory fields: :type, :api-key, :api-secret. Implementations may
  accept other fields also.
  
  OAuth services contain secret information, like api secret keys,
  and should probably not be read, written or serialized."
  s/type-first :hierarchy s/h)

(defmulti request-session
  "Usage:
  (request-session service)

  Returns an unverified OAuth session.

  All unverified sessions will have the key :url,
  which can be used to authorize sessions with verify-session.

  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

(defmulti verify-session
  "Usage:
  (verify-session oauth-service oauth-session verifier)
  Creates a verified OAuth session from the given oauth session
  
  For implementation details, see the docs."
  s/type-first :hierarchy s/h)

; TODO should be varargs?
(defmulti request-raw
  ;"Usage:
  ;(request-raw session opts)
  ;(request-raw service session opts)
  ;(request-raw service-map session opts)
  "Usage:
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
  ;"Usage:
  ;(request session opts)
  ;(request service session opts)
  ;(request service-map session opts)
  "Usage:
  (request service session opts)

  Executes an OAuth request in an OAuth session. Implementations
  might parse the output to a more usable form. The default implementation
  returns the body as a string."
  s/type-first :default :any :hierarchy s/h)

(defmethod request :any
  [service session opts]
  (-> (request-raw service session opts) :body slurp))

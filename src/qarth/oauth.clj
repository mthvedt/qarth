(ns qarth.oauth
  "Base fns for OAuth. All OAuth implementations use these.
  You can also define your own OAuth implementations--see the docs."
  (require [qarth.oauth.support :as s]))

(defmulti build 
  "build [{:keys [type api-key api-secret ...]}]
  Create an OAuth service. Accepts a hash map.
  Mandatory fields: :type, :api-key, :api-secret. Implementations may
  accept other fields also.
  
  OAuth services contain secret information, like api secret keys,
  and should probably not be read, written or serialized."
  s/type-first :hierarchy s/h)

(defmulti request-session
  "get-access-info [service]
  Returns an unverified OAuth session,
  which is a readable/writable/serializable map.
  All services provide at least a :url, an authorization URL to get a verifier."
  s/type-first :hierarchy s/h)

(defmulti verify-session
  "verify [oauth-service oauth-session verifier]
  Creates a verified OAuth session from the given oauth session
  and verifier."
  s/type-first :hierarchy s/h)

(defmulti request-raw
  "request [oauth-service oauth-session url params opts]
  Executes an OAuth request in an OAuth session.
  Does not parse the output."
  s/type-first :hierarchy s/h)

(defmulti request
  "request [service session url params opts]
  Executes an OAuth request in an OAuth session. Implementations
  might parse the output to a more usable form. The default implementation
  does no parsing."
  s/type-first :default :any :hierarchy s/h)

(defmethod request :any
  [service session url params opts]
  (request-raw service session url params opts))

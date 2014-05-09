(ns qarth.oauth
  "Base fns for OAuth. All OAuth implementations use these.
  You can also define your own OAuth implementations--see the docs."
  (:refer-clojure :exclude [derive]))

; TODO support lib
(def ^{:doc "The OAuth hierarchy ref."}
  h
  (atom (make-hierarchy)))

(defn derive
  "Add to the OAuth hierarchy."
  [child parent]
  (swap! h clojure.core/derive child parent))

(defn type-first
  "Returns the :type of the first arg. Used in OAuth multimethods."
  [x & _]
  (:type x))

(defmulti build 
  "build [{:keys [type api-key api-secret ...]}]
  Create an OAuth service. Accepts a hash map.
  Mandatory fields: :type, :api-key, :api-secret. Implementations may
  accept other fields also.
  
  OAuth services contain secret information, like api secret keys,
  and should probably not be read, written or serialized."
  type-first :hierarchy h)

(defmulti request-session
  "get-access-info [service]
  Returns an unverified OAuth session,
  which is a readable/writable/serializable map.
  All services provide at least a :url, an authorization URL to get a verifier."
  type-first :hierarchy h)

(defmulti verify-session
  "verify [oauth-service oauth-session verifier]
  Creates a verified OAuth session from the given oauth session
  and verifier."
  type-first :hierarchy h)

(defmulti request-raw
  "request [oauth-service oauth-session url params opts]
  Executes an OAuth request in an OAuth session.
  Does not parse the output."
  type-first :hierarchy h)

(defmulti request
  "request [service session url params opts]
  Executes an OAuth request in an OAuth session. Implementations
  might parse the output to a more usable form. The default implementation
  does no parsing."
  type-first :default :any :hierarchy h)

(defmethod request :any
  [service session url params opts]
  (request-raw service session url params opts))

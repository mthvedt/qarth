(ns qarth.oauth
  "Base fns for OAuth and OAuth-style interactive auth services.
  You can also define your own auth implementations--see the docs."
  ; TODO get rid of .coms? maybe use ns?
  ; TODO the 'requirement requirement'?
  (require [qarth.support :as s]
           [qarth.oauth.lib :as lib])
  (:refer-clojure :exclude [derive]))

(defn derive
  "Add a type to the Qarth hierarchy."
  ([type] (derive type :any))
  ([type parent] (swap! s/h clojure.core/derive type parent)))

(derive :oauth)
(derive :multi :oauth)

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

(defmethod build :multi
  [{services :services options :options :as spec}]
  {:type :multi
   :services
   (into {}
         (for [[k v] services]
           [k (build (merge options {:type k} v))]))})

(defmethod build :any [{type :type}]
  (throw (RuntimeException. (str "No implementation for type " type
                                 ". Did you require the appropriate namespace?"))))

(defmulti new-record
  "Multimethod. Usage:
  (new-record service)
  (new-record multi-service key)

  Returns an inactive OAuth record. An OAuth record keeps tracks of
  state tokens, request tokens, auth codes, access tokens, CSRF, &c.

  Returns a map with:
  :type -- required, the type of the record
  :url -- optional, a callback URL for interactive auth
  other various implementation-specific keys"
  s/type-first :hierarchy s/h)

(defmethod new-record :multi
  ([service] (throw (IllegalArgumentException. "Missing OAuth service type")))
  ([service key]
   (let [record (new-record (get-in service [:services key]))]
   {:type :multi :key key :record record :url (:url record)})))

(defmulti extract-code
  "Multimethod. Usage:
  (extract-code service record ring-request)

  Return an auth code from the given Ring-formatted request.
  If the request was invalid (perhaps due to a CSRF attack),
  it should return nil."
  s/type-first :hierarchy s/h)

(defmethod extract-code :multi
  [service {key :key record :record} req]
  (extract-code (get-in service [:services key]) record req))

(defmulti activate
  "Multimethod. Usage:
  (activate service record auth-code)
  Takes a new auth record and a auth-code, and creates an active auth record.
  If activation fails, can return nil or optionally
  throw an Exception of some kind."
  s/type-first :hierarchy s/h)

(defmethod activate :multi
  [service {key :key subrecord :record :as record} auth-code]
  (assoc record :record
         (activate (get-in service [:services key]) subrecord auth-code)))

(defmulti active?
  "Multimethod. Usage:
  (active? service record)
  True if a record is active (authenticated and usable), false otherwise."
  s/type-first :hierarchy s/h)

; TODO support exception toggling
(defmulti requestor
  "Multimethod. Usage:
  (let [r (requestor service record)]
    (r opts))

  Returns a fn that can be used to make requests to the auth service.
  The requestor works similarly to the Clojure library clj-http.
  If the record is inactive (perhaps it expired or was never activated),
  throws a Slingshot exception {::qarth.core/unauthorized true}.

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
  :body -- an InputStream (be sure to read and/or close it when you're done!)

  If an exceptional status code happens, throws an Exception instead.
  Other implementations might support more opts and return more stuff.
  
  A default implementation is provided for implementors. It adds the param
  :access_token to the form params if it's a POST, and the query if it's a GET."
  s/type-first :hierarchy s/h)

(defmethod requestor :multi
  [service {key :key record :record}]
  (requestor (get-in service [:services key]) record))

(defn resp-reader
  "Get a reader from a response map. Make sure to close and/or fully read it."
  [req]
  (-> req :body clojure.java.io/reader))

(defmacro with-resp-reader [[sym requestor m] & body]
  "Executes (requestor m), gets a reader from the body, binds it to the given sym,
  and closes the reader when done."
  `(let [~sym (resp-reader (~requestor ~m))]
     (try
       ~@body
       (finally
         (try
           (.close ~sym)
           (catch Exception ~'_))))))

; TODO fully read response body
; TODO no requires
(defmulti id
  "Multimethod. Optional. Usage:
  (id requestor)

  Gets a user ID from the requestor. The ID is guaranteed to be unique
  and unchanging per service."
  type :hierarchy s/h)

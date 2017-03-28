(ns qarth.ring
  "Low-level Ring fns to use with Qarth.
  This lib is in a state of flux,
  so you probably shouldn't refer to it in your code."
  (require (qarth [oauth :as oauth] [core :as core])
           ring.util.response
           [clojure.tools.logging :as log])
  (:refer-clojure :exclude [get set update]))

(defn new-record-redirect-handler
  "Returns a Ring handler that creates a new auth record
  and returns a response redirecting to its auth :url.

  If a :service query param is present, constructs an auth record from
  a multiservice with the specified key."
  ([service]
   (fn [{session :session params :query-params}]
     (log/trace "Reached new record redirect handler")
     (let [record (if (= (:type service) :multi)
                    (if-let [key (clojure.core/get params "service")]
                      (oauth/new-record service (keyword key))
                      (throw (IllegalArgumentException.
                               (str "Multi-services require an auth record or "
                                    "service parameter"))))
                    (oauth/new-record service))
           session (assoc session ::oauth/record record)]
       (log/debug "Redirecting and installing new oauth record"
                  (pr-str record))
       (assoc (ring.util.response/redirect (:url record))
              :session session))))
  ([service key]
   (let [h (new-record-redirect-handler service)]
     (fn [req]
       (log/trace "New record redirect handler with key" key)
       (h (assoc-in req [:query-params "service"] key))))))

(defn get
  "Gets the auth record from the request.

  Short for (get-in request [:session ::oauth/record])"
  [request]
  (get-in request [:session ::oauth/record]))

(defn set
  "Sets the auth record in the request. If the given auth record is nil,
  removes the auth record instead."
  [request record]
  (if record
    (assoc-in request [:session ::oauth/record] record)
    (update-in request [:session] #(dissoc % ::oauth/record))))

(defn update
  "Updates the auth record in the request.

  Short for (update-in request [:session ::oauth/record] f)."
  [request f]
  (update-in request [:session ::oauth/record] f))

(defn activate-request
  "Given a callback in the form of a Ring request, activates the current auth record.
  Returns the updated Ring request with the new auth record.
  Returns null if it's not an activation request. Throws an exception on failure."
  [service {{record ::oauth/record} :session :as request}]
  (log/trace "Activate request")
  (if-let [v (oauth/extract-code service record request)]
    (do
      (log/debug "Got auth code" v)
      (if-let [record (oauth/activate service record v)]
        (do
          (log/debug "Got active record" (pr-str record))
          (set request record))
        (core/unauthorized (str "Could not activate record with token " v))))))

(defn auth-callback-handler
  "Returns a Ring handler that handles an auth callback request,
  for example from an OAuth callback,
  and attempts to verify the current auth record.
  If success, continues to call (success-handler request).
  If no auth code was found, continues to (fallback-handler request).
  If failure, calls (exception-handler request exception)."
  [service success-handler fallback-handler exception-handler]
  (fn [req]
    (log/trace "Reached auth callback handler")
    (try
      (if-let [resp (activate-request service req)]
        (success-handler resp)
        (if fallback-handler
          (do
            (log/debug "Failed to get auth code, executing fallback handler")
            (fallback-handler req))
          (-> req :params pr-str
            (str "Could not get auth code with params: ")
            core/unauthorized)))
      (catch Exception e
        (log/debug "Auth callback handler caught exception" (.getMessage e))
        (log/debug "Params were" (-> req :params pr-str))
        (log/trace "Request was" (pr-str req))
        (exception-handler req e)))))

(defn omni-handler
  "Returns a Ring handler that handles both new auth records and callbacks.
  Your one-stop shop for auth. Install this at some desired route,
  and redirect to that route to kick off the auth process.

  Required params:
  service -- The auth service.
  success-handler -- Called when an auth record is successfully activated
  (or is already active).
  failure-handler -- Called when an auth record exists, is inactive,
  but could not be activated. An exception is logged and the auth record is removed
  first.

  Optional params:
  new-record-handler -- Called when this handler detects no auth record.
  Default is qarth.ring/new-record-redirect-handler.
  exception-handler -- a function (f request exception). The default
  is to log the exception, remove the auth record, and call failure-handler.
  fallback -- If true, verification attempts that fail will be assumed
  to be initial auth attempts, and will call new-record-handler instead.
  Defaults to true.

  Query params:
  key -- if present, will construct auth records from a multiservice
  using the specified key."
  [{:keys [service success-handler failure-handler
           new-record-handler exception-handler fallback]
    :as config}]
  (let [new-record-handler (or new-record-handler
                               (new-record-redirect-handler service))
        fallback (or fallback (nil? fallback))
        exception-handler (or exception-handler
                              (fn [req e]
                                (log/trace "Reached default exception handler;"
                                           "exception forthcoming")
                                (log/info e "Exception trying to verify record")
                                (failure-handler (set req nil))))
        callback-handler (auth-callback-handler service success-handler
                                                (if fallback new-record-handler)
                                                exception-handler)]
    (fn [req]
      (log/trace "Reached Ring omni-handler")
      (log/trace "with session" (pr-str (:session req)))
      (log/trace "with cookies" (pr-str (:cookies req)))
      (log/trace "with params" (pr-str (:params req)))
      (if-let [record (get req)]
        (if (oauth/active? service record)
          (success-handler req)
          (callback-handler req))
        (try 
          (new-record-handler req)
          (catch Exception e
            (exception-handler req e)))))))

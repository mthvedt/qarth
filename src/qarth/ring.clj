(ns qarth.ring
  "Low-level Ring fns to use with Qarth."
  (require [qarth.oauth :as oauth]
           ring.util.response
           [clojure.tools.logging :as log])
  (:refer-clojure :exclude [get set]))

(defn new-record-redirect-handler
  "Returns a Ring handler that creates a new auth record
  and returns a response redirecting to its auth :url.

  If a :service query param is present, constructs an auth record from
  a multiservice with the specified key."
  ([service]
   (fn [{session :session params :query-params}]
     (let [record (if-let [key (clojure.core/get params "service")]
                    (oauth/new-record service (keyword key))
                    (oauth/new-record service))
           session (assoc session ::oauth/record record)]
       (assoc (ring.util.response/redirect (:url record))
              :session session))))
  ([service key]
   (let [h (new-record-redirect-handler service)]
     (fn [req]
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

; TODO should we distinguish between normal failures and exceptional cases?
(defn verify-request
  "Given a callback in the form of a Ring request, verifies the current auth record.
  Returns the updated Ring request with the new auth record.
  Throws an exception on failure."
  [service {{record ::oauth/record} :session :as request}]
  (if-let [v (oauth/extract-verifier service record request)]
    (if-let [record (oauth/verify service record v)]
      (set request record)
      (throw (RuntimeException. (str "Could not verify record with token " v))))
    (throw (RuntimeException. (str "Could not get verifier with params: "
                                   (pr-str (:params request)))))))

(defn auth-callback-handler
  "Returns a Ring handler that handles an auth callback request,
  for example from an OAuth callback,
  and attempts to verify the current auth record.
  If success, continues to call (success-handler request).
  If failure, calls (exception-handler request exception)."
  [service success-handler exception-handler]
  (fn [req]
    (try
      (log/trace "Verifying auth record...")
      (let [req (verify-request service req)]
        (if (oauth/active? service (get req))
          (do
            (log/trace "Verified auth record")
            (success-handler req))
          (throw (IllegalStateException.
                   "Verification was successful but record not active"))))
      (catch Exception e
        (exception-handler req e)))))

(defn omni-handler
  "Returns a Ring handler that handles both new auth records and callbacks.
  Your one-stop shop for auth. Install this at some desired route,
  and redirect to that route to kick off the auth process.

  Required params:
  service -- The auth service.
  success-handler -- Called when an auth record is successfully verified
  (or is already verified).
  failure-handler -- Called when an auth record exists, is unverified,
  but could not be verified. An exception is logged and the auth record is removed
  first.

  Optional params:
  new-record-handler -- Called when this handler detects no auth record.
  Default is qarth.ring/new-record-redirect-handler.
  exception-handler -- a function (f request exception). The default
  is to log the exception, remove the auth record, and call failure-handler.

  Query params:
  key -- if present, will construct auth records from a multiservice
  using the specified key."
  [{:keys [service success-handler failure-handler
           new-record-handler exception-handler]
    :as config}]
  (let [new-record-handler (or new-record-handler
                               (new-record-redirect-handler service))
        exception-handler (or exception-handler
                              (fn [req e]
                                (log/info e "Exception trying to verify record")
                                (failure-handler (set req nil))))
        callback-handler (auth-callback-handler service success-handler
                                                exception-handler)]
    ; TODO call it subservice? key?
    (fn [req]
      (if-let [record (get req)]
        (callback-handler req)
        (new-record-handler req)))))

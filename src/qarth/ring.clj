(ns qarth.ring
  "Low-level Ring fns to use with Qarth."
  (require [qarth.oauth :as oauth]
           ring.util.response
           [clojure.tools.logging :as log])
  (:refer-clojure :exclude [get set]))

(defn new-session-redirect-handler
  "Returns a Ring handler that creates a new auth session
  and returns a response redirecting to its auth :url."
  [service]
  (fn [{session :session}]
    (let [sesh (oauth/new-session service)
          session (assoc session ::oauth/session sesh)]
      (assoc (ring.util.response/redirect (:url sesh))
             :session session))))

(defn get
  "Gets the auth session from the request.

  Short for (get-in request [:session ::oauth/session])"
  [request]
  (get-in request [:session ::oauth/session]))

(defn set
  "Sets the auth session in the request. If the given auth session is nil,
  removes the auth session instead."
  [request auth-session]
  (if auth-session
    (assoc-in request [:session ::oauth/session] auth-session)
    (update-in request [:session] #(dissoc % ::oauth/session))))

(defn update
  "Updates the auth sesssion in the request.

  Short for (update-in request [:session ::oauth/session] f)."
  [request f]
  (update-in request [:session ::oauth/session] f))

; TODO should we distinguish between normal failures and exceptional cases?
(defn verify-request
  "Given a callback in the form of a Ring request, verifies the current auth session.
  Returns the updated Ring request with the new auth session.
  Throws an exception on failure."
  [service {{sesh ::oauth/session} :session :as request}]
  (if-let [v (oauth/extract-verifier service sesh request)]
    (if-let [sesh (oauth/verify service sesh v)]
      (set request sesh)
      (throw (RuntimeException. (str "Could not verify session with token " v))))
    (throw (RuntimeException. (str "Could not get verifier with params: "
                                   (pr-str (:params request)))))))

(defn auth-callback-handler
  "Returns a Ring handler that handles an auth callback request,
  for example from an OAuth callback,
  and attempts to verify the current auth session.
  If success, continues to call (success-handler request).
  If failure, removes the auth session and
  calls (exception-handler request exception)."
  [service success-handler exception-handler]
  (fn [req]
    (try
      (log/trace "Verifying auth session...")
      (let [req (verify-request service req)]
        (if (oauth/active? service (get req))
          (do
            (log/trace "Verified auth session")
            (success-handler req))
          (throw (IllegalStateException.
                   "Verification was successful but session not active"))))
      (catch Exception e
        (exception-handler (set req nil) e)))))

(defn omni-handler
  "Returns a Ring handler that handles both new auth sessions and callbacks.
  Your one-stop shop for auth. Install this at some desired route,
  and redirect to that route to kick off the auth process.

  Required params:
  service -- The auth service.
  success-handler -- Called when an auth session is successfully verified
  (or is already verified).
  failure-handler -- Called when an auth session exists, is unverified,
  but could not be verified.
  Will log the failure and/or exception and nuke the auth session beforehand.
  Can be overridden by an exception-handler.

  Optional params:
  new-session-handler -- Called when this handler detects no auth session,
  and a new one is created. Default is to redirect to the auth :url.
  exception-handler -- a function (f request exception). Overrides failure-handler."
  [{:keys [service success-handler failure-handler
           new-session-handler exception-handler]
    :as config}]
  (let [new-session-handler (or new-session-handler
                                (new-session-redirect-handler service))
        exception-handler (or exception-handler
                              (fn [req e]
                                (log/info e "Exception trying to verify session")
                                (failure-handler req)))
        callback-handler (auth-callback-handler service success-handler
                                                exception-handler)]
    (fn [req]
      (if-let [sesh (get req)]
        (callback-handler req)
        (new-session-handler req)))))

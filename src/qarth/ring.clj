(ns qarth.ring
  "Low-level Ring fns to use with Qarth."
  (require [qarth.oauth :as oauth]
           ring.util.response
           [clojure.tools.logging :as log]))

(defn auth-redirect
  "Creates a new auth session and returns a response redirecting to its
  auth :url."
  [service {session :session}]
  (let [sesh (oauth/new-session service)
        session (assoc session ::oauth/session sesh)]
    (assoc (ring.util.response/redirect (:url sesh))
           :session session)))

(defn verify-request
  "Given a callback in the form of a Ring request, verifies the current auth session.
  Returns the updated auth session, which you should re-add to the Ring session."
  [service {{sesh ::oauth/session} :session :as request}]
  (oauth/verify service sesh (oauth/extract-verifier service sesh request)))

(defn oauth-callback-handler
  "A Ring handler that handles an auth callback request,
  for example from an OAuth callback,
  and attempts to verify the current auth session.
  Based on success/failure,
  continues on to one of the given handlers.
  Will also log failures or exceptions.

  Optional -- exception-handler, a function (f request exception).
  The default is to log exceptions and treat them as failures."
  ([req service success-handler failure-handler]
   (oauth-callback-handler service req success-handler failure-handler
                           (fn [req ex]
                             (log/info ex "Exception verifying auth session")
                             (failure-handler req))))
  ([service req success-handler failure-handler exception-handler]
   (try
     (log/trace "Verifying auth session...")
     (let [sesh (verify-request service req)
           req (assoc-in req [:session ::oauth/session] sesh)]
       (if (oauth/active? service sesh)
         (do
           (log/trace "Verified auth session")
           (success-handler req))
         (do
           (log/info "Failure to verify auth session")
           (failure-handler req))))
     (catch Exception e
       (exception-handler req e)))))

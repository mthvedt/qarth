(ns qarth.ring
  "Low-level Ring fns to use with Qarth."
  (require [qarth.oauth :as oauth]
           ring.util.response
           [clojure.tools.logging :as log])
  (:refer-clojure :exclude [get set]))

(defn auth-redirect-handler
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
  "Sets the auth session in the request.
  
  Short for (assoc-in request [:session ::auth/session] auth-session)"
  [request auth-session]
  (assoc-in request [:session ::oauth/session] auth-session))

(defn update
  "Updates the auth sesssion in the request.
  
  Short for (update-in request [:session ::oauth/session] f)."
  [request f]
  (update-in request [:session ::oauth/session] f))

; TODO should verify return success or exception?
(defn verify-request
  "Given a callback in the form of a Ring request, verifies the current auth session.
  Returns the updated Ring request with the new auth session."
  [service {{sesh ::oauth/session} :session :as request}]
  (->> request
    (oauth/extract-verifier service sesh)
    (oauth/verify service sesh)
    (set request)))

(defn auth-callback-handler
  "Returns a Ring handler that handles an auth callback request,
  for example from an OAuth callback,
  and attempts to verify the current auth session.
  Based on success/failure,
  continues on to one of the given handlers.
  Will also log failures or exceptions.

  Optional -- exception-handler, a function (f request exception).
  The default is to log exceptions and treat them as failures."
  ([service success-handler failure-handler]
   (auth-callback-handler service success-handler failure-handler
                           (fn [req ex]
                             (log/info ex "Exception verifying auth session")
                             (failure-handler req))))
  ([service success-handler failure-handler exception-handler]
   (fn [req]
     (try
       (log/trace "Verifying auth session...")
       (let [req (verify-request service req)]
         (if (oauth/active? service (get req))
           (do
             (log/trace "Verified auth session")
             (success-handler req))
           (do
             (log/info "Failure to verify auth session")
             (failure-handler req))))
       (catch Exception e
         (exception-handler req e))))))

(defn- auth-handler-helper
  [service callback-handler]
  (let [redir-handler (auth-redirect-handler service)]
    (fn [req]
      (if (get-in req [:session ::new?])
        (do
          (log/trace "Auth handler: Auth callback detected")
          (callback-handler (update-in req [:session] #(dissoc % ::new?))))
        (do
          (log/trace "Auth handler: Auth callback not detected, "
                     "opening new auth session and redirecting")
          (redir-handler (assoc-in req [:session ::new?] true)))))))

(defn auth-handler
  "Returns a Ring handler that handles both opening new auth sessions
  and auth callbacks. Your one-stop-shop for handling login activity.

  Optional -- exception-handler, a function (f request exception).
  The default is to log exceptions and treat them as failures."
  ([service success-handler failure-handler]
   (auth-handler-helper service
                        (auth-callback-handler service success-handler
                                               failure-handler)))
  ([service success-handler failure-handler exception-handler]
   (auth-handler-helper service
                        (auth-callback-handler service success-handler
                                               failure-handler exception-handler))))

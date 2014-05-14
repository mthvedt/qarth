(ns qarth.ring
  "Utils to tie Qarth sessions to Ring sessions."
  (require [qarth.oauth :as oauth]
           [qarth.oauth.lib :as lib]
           [clojure.tools.logging :as log]
           ring.util.response)
  (use [slingshot.slingshot :only [try+ throw+]]))

(defn ring-new-session
  "Returns a Ring redirect with a new, unverified OAuth session stored in the
  ring session. Redirects to the :url provided by the OAuth service."
  ; TODO add :resource key
  [{ring-session :session :as ring-request} service]
  (when-let [oauth-session (oauth/new-session service)]
    (assoc (ring.util.response/redirect (:url oauth-session))
           :session (assoc (or ring-session {}) ::oauth-session oauth-session))))

(defn get-oauth-session
  "Gets the current oauth session from the current ring session in a request."
  [{{oauth-session ::oauth-session :as ring-session} :session :as ring-request}]
  oauth-session)

(defn set-oauth-session
  "Sets the current oauth session from the current ring session.
  If the given oauth session is nil, clears it instead. Returns the request."
  [request oauth-session]
  (if oauth-session
    (assoc-in request [:session ::oauth-session] oauth-session)
    (update-in request [:session] #(dissoc % ::oauth-session))))

(defn transfer-ring-session
  "Merges the Ring session for the first request into the second one."
  [{session1 :session} {session2 :session :as resp}]
  (assoc resp :session (merge (or session2 {}) (or session1 {}))))

; TODO put this in a different thing?
; TODO remove throw-401 references
; TODO fix this
#_(defn verify-middleware
  "Verifies an OAuth session, using the information currently in the Ring session,
  and returned from the request. Intended to be used as an OAuth callback.
  Stores the OAuth session in the current ring session.

  If verification fails with a {:status 401} exception, logs the exception
  and reroutes to failure-handler."
  ; TODO optional failure handler?
  ; TODO 401
  [oauth-service success-handler failure-handler]
  ; TODO: Errors here are not terribly graceful.
  ; TODO cleanup
  (fn [{{oauth-session ::oauth-session :as ring-session} :session :as ring-request}]
    ; TODO
    (try+
      (if-let [oauth-session (oauth/verify-session oauth-service oauth-session
                                           (oauth/extract-token
                                             oauth-service ring-request))]
        (success-handler (set-oauth-session ring-request oauth-session))
        (do
          (log/infof "OAuth callback could not be verified")
          (failure-handler (set-oauth-session ring-request nil))))
      (catch Exception e
        (log/info e "Exception trying to verify OAuth callback")
        (failure-handler (set-oauth-session ring-request nil))))))

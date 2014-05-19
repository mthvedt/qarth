(ns qarth.friend
  "Friend workflows for Qarth."
  (require (qarth [oauth :as oauth]
                  [ring :as qarth-ring])
           [cemerick.friend :as friend]
           (ring.util request response)
           [clojure.tools.logging :as log]))

(defn- credential-map [credential-fn sesh redirect-on-auth?]
  (vary-meta (credential-fn sesh) assoc
             ::friend/workflow ::qarth
             ::friend/redirect-on-auth? redirect-on-auth?
             :type ::friend/auth))

; TODO multi-workflow, remove intermediate step
; TODO make requests from friend
; TODO user principal cred fn
; TODO better flow--success-handler, failure-handler, error-handler
(defn workflow
  "Creates a Friend workflow using a Qarth service.

  Required arguments:
  service -- the OAuth service

  Optional arguments:
  oauth-url -- a URL used by the workflow that starts the oauth authentication flow,
  default is /auth/doauth. Must be relative
  callback -- the OAuth callback in the authentication flow, default is /auth/callback.
  Must be relative
  credential-fn -- override Friend credential fn (default is identity)
  redirect-on-auth? -- the Friend redirect on auth setting, default true
  login-failure-handler -- the login failure handler. Overrides the friend settings
  (the Friend login-failure-handler, login-url, or /login in that order)

  The workflow's returned credentials are the Qarth session,
  with Friend metadata attached.
  Exceptions are logged and countered as auth failures."
  [{:keys [service oauth-url callback credential-fn redirect-on-auth?
           login-url login-failure-handler]}]
  (let [redirect-on-auth? (or redirect-on-auth? true)
        callback (or callback "/auth/callback")
        oauth-url (or oauth-url "/auth/doauth")]
    (fn [{ring-sesh :session :as req}]
      (let [path (ring.util.request/path-info req)
            auth-config (::friend/auth-config req)
            credential-fn (or credential-fn
                              (get auth-config :credential-fn)
                              identity)
            success-handler (fn [{{sesh ::oauth/session} :session}]
                              (credential-map credential-fn sesh redirect-on-auth?))
            login-failure-handler (or login-failure-handler
                                      (get auth-config :login-failure-handler)
                                      #(ring.util.response/redirect
                                         (or (get auth-config :login-url)
                                             login-url "/login")))]
        (cond
          (= path callback)
          (qarth-ring/oauth-callback-handler req service
                                             success-handler
                                             login-failure-handler)

          (= path oauth-url)
          (do
            (log/trace "Redirecting to OAuth service")
            (qarth-ring/auth-redirect service req)))))))

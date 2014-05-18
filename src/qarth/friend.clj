(ns qarth.friend
  "Friend workflows for Qarth."
  (require [qarth.oauth :as oauth]
           [cemerick.friend :as friend]
           (ring.util request response)
           [clojure.tools.logging :as log]))

(defn- credential-map [credential-fn sesh redirect-on-auth?]
  (vary-meta (credential-fn sesh) assoc
             ::friend/workflow ::qarth
             ::friend/redirect-on-auth? redirect-on-auth?
             :type ::friend/auth))

(defn workflow
  "Creates a Friend workflow using a Qarth service.

  Required arguments:
  service -- the OAuth service

  Optional arguments:
  oauth-url -- a URL used by the workflow that starts the oauth authentication flow,
  default is /auth/doauth. Must be relative
  callback -- the OAuth callback in the authentication flow, default is /callback.
  Must be relative
  credential-fn -- override Friend credential fn
  redirect-on-auth? -- the Friend redirect on auth setting, default true
  login-failure-handler -- the login failure handler,
  default is to use the Friend login-failure-handler,
  redirect to the Friend :login-url or /login

  The workflow's returned credentials are the Qarth session,
  with Friend metadata attached."
  [{:keys [service oauth-url callback credential-fn redirect-on-auth?
           login-url login-failure-handler]}]
  (let [redirect-on-auth? (or redirect-on-auth? true)
        callback (or callback "/callback")
        oauth-url (or oauth-url "/auth/doauth")]
    ; TODO what is :login-uri
    (fn [{{sesh ::oauth/session :as ring-sesh} :session :as req}]
      (let [path (ring.util.request/path-info req)
            credential-fn (or credential-fn
                              (get-in req [::friend/auth-config :credential-fn])
                              (throw (IllegalArgumentException.
                                       "No credential-fn provided")))
            login-failure-handler (or login-failure-handler
                                      (get-in req [::friend/auth-config
                                                   :login-failure-handler])
                                      #(ring.util.response/redirect
                                         (or 
                                           (get-in req [::friend/auth-config
                                                        :login-url])
                                           login-url "/login")))]
        (cond
          (= path callback)
          (if (oauth/is-active? sesh)
            (do
              ; TODO logging
              (prn "OAuth session already verified")
              (credential-map credential-fn sesh redirect-on-auth?))
            ; TODO make verify idempotent?
            (let [sesh (oauth/verify service sesh (oauth/extract-token service req))]
              (prn "Verifying OAuth session")
              (if (oauth/is-active? sesh)
                (do
                  (prn "Verification successful")
                  (credential-map credential-fn sesh redirect-on-auth?))
                (do
                  (prn "Failure to verify session")
                  (login-failure-handler req)))))

          (= path oauth-url)
          (let [sesh (oauth/new-session service)
                ring-sesh (assoc ring-sesh ::oauth/session sesh)]
            (prn "Redirecting to OAuth service")
            (assoc (ring.util.response/redirect (:url sesh))
                   :session ring-sesh)))))))

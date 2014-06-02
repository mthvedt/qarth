(ns qarth.friend
  "Friend workflows for Qarth."
  (require (qarth [oauth :as oauth]
                  [ring :as qarth-ring])
           [cemerick.friend :as friend]
           (ring.util request response)
           [clojure.tools.logging :as log]))

(defn- credential-map
  ;Adds Friend meta-information to the auth record, and uses that
  ;as the credential map.
  [credential-fn record redirect-on-auth?]
  (if-let [r (credential-fn record)]
    (vary-meta r assoc
               ::friend/workflow ::qarth
               ::friend/redirect-on-auth? redirect-on-auth?
               :type ::friend/auth)))

(defn auth-record
  "Looks for a qarth auth record in the Friend authentications.
  Returns it, or nil if not found."
  [req]
  (->> req
    :session ::friend/identity :authentications
    vals
    (filter #(-> % meta ::friend/workflow (= ::qarth)))
    first
    ::oauth/record))

(defn requestor
  "Get an auth requestor from a Friend-authenticated request and a service."
  [req service]
  (if-let [r (auth-record req)]
    (oauth/requestor service r)))

(defn workflow
  "Creates a Friend workflow using a Qarth service.

  Required arguments:
  service -- the auth service
  auth-url -- A dual purpose URL. This starts both the OAuth workflow
  (so a login button, for example, should redirect here)
  and serves as the auth callback.
  It should be the same as the callback in your auth service.

  Optional arguments:
  login-url or login-uri -- a URL to redirect to if a user is not logged in.
  (The default Friend :login-uri is /login.)
  key -- for multi-services. Can also be passed as a query param, \"service\".
  credential-fn -- override the Friend credential fn.
  The default Friend credential-fn, for some reason, returns nil.
  The credential map is of the form
  {::qarth.oauth/record auth-record, :identity ::qarth.oauth/anonymous}.
  redirect-on-auth? -- the Friend redirect on auth setting, default true
  login-failure-handler -- the login failure handler.
  Default is to use the Friend login-failure-handler, redirect to a
  configured login-url or redirect to the Friend :login-uri while
  preserving the current record.

  Exceptions are logged and treated as auth failures."
  [{:keys [service key auth-url credential-fn redirect-on-auth?
           login-url login-uri login-failure-handler] :as params}]
  (fn [{ring-sesh :session :as req}]
    (let [auth-config (merge (::friend/auth-config req) params)
          auth-url (or auth-url (:auth-url auth-config))]
      (if (= (ring.util.request/path-info req) auth-url)
        (let [redirect-on-auth? (or redirect-on-auth?
                                    (:redirect-on-auth? auth-config) true)
              credential-fn (or credential-fn (:credential-fn auth-config))
              success-handler (fn [{{record ::oauth/record} :session}]
                                (credential-map credential-fn
                                                {::oauth/record record
                                                 :identity ::qarth.oauth/anonymous}
                                                redirect-on-auth?))
              key (or key (:key auth-config))
              req (if key
                    (assoc-in req [:query-params "service"] key)
                    req)
              login-failure-handler (or login-failure-handler
                                        (get auth-config :login-failure-handler)
                                        (fn [req]
                                          (assoc
                                            (ring.util.response/redirect
                                              (or login-url
                                                  login-uri
                                                  ; Honor our promise to look up
                                                  ; 'the configured login-url'
                                                  (:login-url auth-config)
                                                  (:login-uri auth-config)))
                                            :session (:session req))))]
          ((qarth-ring/omni-handler {:service service
                                     :success-handler success-handler
                                     :failure-handler login-failure-handler})
             req))))))

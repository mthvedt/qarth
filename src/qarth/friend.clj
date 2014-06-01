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

; TODO add and doc credential map, credential fn, but in a separate .md
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

; TODO multi-workflow
; TODO make requests from friend
; TODO user principal cred fn
; TODO how to do requestor
; TODO type metadata for requestors
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
  credential-fn -- override the Friend credential fn
  redirect-on-auth? -- the Friend redirect on auth setting, default true
  login-failure-handler -- the login failure handler.
  Default is to use the Friend login-failure-handler, redirect to a
  configured login-url or redirect to the Friend :login-uri while
  preserving the current record.
  (The default Friend :login-uri is /login. Note that Friend
  calls it a 'URI' even though it's always a URL.)

  Because of the architecture of Friend, this workflow requires
  that the qarth.oauth/principal method be supported.

  Exceptions are logged and treated as auth failures.

  For experts--
  The workflow's returned credentials are of the form
  {::qarth.oauth/record auth-record ::qarth.oauth/type record-type
  ::qarth.oauth/principal principal :identity [record-type principal]}."
  ; TODO consider docs
  [{:keys [service auth-url credential-fn redirect-on-auth?
           login-url login-uri login-failure-handler] :as params}]
  (fn [{ring-sesh :session :as req}]
    (let [auth-config (merge (::friend/auth-config req) params)
          auth-url (or auth-url (:auth-url auth-config))]
      (if (= (ring.util.request/path-info req) auth-url)
        (let [redirect-on-auth? (or redirect-on-auth?
                                    (:redirect-on-auth? auth-config) true)
              credential-fn (or credential-fn (:credential-fn auth-config))
              success-handler (fn [{{record ::oauth/record} :session}]
                                (log/debug "Fetching user principal...")
                                (let [p (->> record
                                          (oauth/requestor service)
                                          oauth/id)
                                      type (:type record)]
                                  ; TODO principal map
                                  ; TODO make principals optional
                                  ; otherwise use static id (::qarth.oauth/anonymous?)
                                  (credential-map credential-fn
                                                  {::oauth/record record
                                                   ::oauth/id p
                                                   :identity [type p]
                                                   ::oauth/type (:type record)}
                                                  redirect-on-auth?)))
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
          ; TODO add credential stuff to omnihandler?
          ((qarth-ring/omni-handler {:service service
                                     :success-handler success-handler
                                     :failure-handler login-failure-handler})
             req))))))

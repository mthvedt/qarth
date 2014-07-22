(ns qarth.impl.oauth-v2
  "A generic OAuth v2 implementation. Services should have the keys
  :request-url and :access-url. Assumes form-encoded responses."
  (require (qarth [oauth :as oauth] core)
           [qarth.oauth.lib :as lib]
           clj-http.client))

(defmethod oauth/build :oauth [x] x)

(defmethod oauth/new-record :oauth
  [{request-url :request-url :as service}]
  (lib/do-new-record service request-url {}))

(defmethod oauth/extract-code :oauth
  [_ {state :state} req]
  (lib/do-extract-code state req :state :code :error))

(defmethod oauth/activate :oauth
  [{access-url :access-url :as service} record auth-code]
  (lib/do-activate service record auth-code access-url lib/v2-form-parser))

(defmethod oauth/active? :oauth
  [_ {access-token :access-token}]
  (if access-token true false))

(defmethod oauth/requestor :oauth
  [_ {access-token :access-token type :type}]
  (if access-token
    (vary-meta
      (fn [req]
        (let [req (merge {:method :get} req)
              param-key (if (= (:method req) :post) :form-params :query-params)]
          (clj-http.client/request
            (assoc-in req [param-key :access_token] access-token))))
      assoc :type type)
    (qarth.core/unauthorized "Record is not active")))

(ns qarth.impl.facebook
  "A Facebook oauth impl."
  (require [qarth.oauth :as oauth]
           cheshire.core
           [qarth.oauth.lib :as lib]
           clj-http.client))

(qarth.lib/derive :facebook.com :oauth)

; TODO make example
; TODO verifier nomenclature?
(defmethod oauth/build :facebook.com
  [service]
  (assoc service
         :request-url "https://www.facebook.com/dialog/oauth"
         :verify-url "https://graph.facebook.com/oauth/access_token"))

; TODO maybe better as multimethod?
(defmethod oauth/verify :facebook.com
  [service record verifier]
  (lib/do-verify service record verifier (:verify-url service) {}
                 (fn [req]
                   (let [resp (-> req :body (ring.util.codec/form-decode))]
                     {:access-token (get resp "access_token")
                      :expires (get resp "expires")}))))

(defmethod oauth/id :facebook.com
  [requestor]
  (let [resp (requestor {:url "https://graph.facebook.com/me"})]
    (-> resp
      :body
      cheshire.core/parse-string
      (get "id"))))

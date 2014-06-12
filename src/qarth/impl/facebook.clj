(ns qarth.impl.facebook
  "A Facebook oauth impl. Type is facebook.com."
  (require [qarth.oauth :as oauth]
           cheshire.core
           [qarth.oauth.lib :as lib]
           clj-http.client))

(qarth.lib/derive :facebook.com :oauth)

; TODO make example
; TODO verifier nomenclature? Actually all nomenclature
(defmethod oauth/build :facebook.com
  [service]
  (assoc service
         :request-url "https://www.facebook.com/dialog/oauth"
         :verify-url "https://graph.facebook.com/oauth/access_token"))

; TODO maybe better as multimethod?
(defmethod oauth/verify :facebook.com
  [service record verifier]
  (lib/do-verify service record verifier (:verify-url service) lib/v2-form-parser))

(defmethod oauth/id :facebook.com
  [requestor]
  ; TODO requestor body should be a stream
  (-> (requestor {:url "https://graph.facebook.com/me"})
    :body cheshire.core/parse-string (get "id")))

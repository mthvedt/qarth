(ns qarth.impl.facebook
  "A Github oauth impl. Type is github.com."
  (require [qarth.oauth :as oauth]
           cheshire.core
           [qarth.oauth.lib :as lib]
           clj-http.client))

(qarth.lib/derive :github.com :oauth)

; TODO make example
; TODO verifier nomenclature? Actually all nomenclature
(defmethod oauth/build :github.com
  [service]
  (assoc service
         :request-url "https://github.com/login/oauth/authorize"
         :verify-url "https://github.com/login/oauth/access_token"))

; TODO double check responses and stuff
(defmethod oauth/verify :github.com
  [service record verifier]
  (lib/do-verify service record verifier (:verify-url service) lib/v2-form-parser))

(defmethod oauth/id :github.com
  [requestor]
  (-> (requestor {:url "https://api.github.com/user"})
    :body cheshire.core/parse-string (get "id")))


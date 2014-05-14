(ns qarth.test-ring
  (require [qarth.oauth :as oauth]
           [qarth.ring :as ring]
           qarth.util
           ; TODO maybe auto load impl?
           qarth.impl.scribe
           ring.util.response
           compojure.handler)
  (use compojure.core))

; TODO: use ring-jetty-adapter instead when we move to multiple examples.
; any example should be run with line with-profile run such and such
(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:yahoo conf)
                                 :type :scribe
                                 :provider org.scribe.builder.api.YahooApi
                                 :callback "http://localhost:3000/oauth-callback")))

; TODO this is unweildly. easier with middleware?
; TODO verify-session -> verify
(defroutes app
  (GET "/" req
       (let [sesh (ring/get-oauth-session req)]
         (if (oauth/is-active? sesh)
           "<html><body>Hello world!</body></html>"
           (ring/ring-new-session req service))))
  (GET "/oauth-callback" req
       (let [sesh (ring/get-oauth-session req)]
         (ring/transfer-ring-session
           (ring/set-oauth-session req
                                   (oauth/verify-session
                                     service sesh (oauth/extract-token service req)))
           (ring.util.response/redirect "/")))))

(def app (compojure.handler/site app))

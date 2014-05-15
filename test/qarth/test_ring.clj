(ns qarth.test-ring
  (require [qarth.oauth :as oauth]
           qarth.util
           ; TODO maybe auto load impl?
           qarth.impl.scribe
           ring.util.response
           compojure.handler)
  (use compojure.core))
; A verbose example, not using any helpers or custom middleware,
; to show you how you might tie Qarth OAuth into Ring in your own way.

; TODO: use ring-jetty-adapter instead when we move to multiple examples.
; any example should be run with line with-profile run such and such
(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:yahoo conf)
                                 :type :scribe
                                 :provider org.scribe.builder.api.YahooApi
                                 :callback "http://localhost:3000/oauth-callback")))

(defroutes app
  (GET "/" req
       (let [sesh (get-in req [:session ::oauth/session])]
         (if (oauth/is-active? sesh)
           "<html><body>Hello world!</body></html>"
           (let [sesh (oauth/new-session service)
                 req (assoc-in req [:session ::oauth/session] sesh)]
             (assoc (ring.util.response/redirect (:url sesh))
                    :session (:session req))))))
  (GET "/oauth-callback" req
       (let [sesh (oauth/verify service (get-in req [:session ::oauth/session])
                                (oauth/extract-token service req))
             req (assoc-in req [:session ::oauth/session] sesh)]
         ; If success
         (if (oauth/is-active? sesh)
           (assoc (ring.util.response/redirect "/") :session (:session req))
           (let [sesh (oauth/new-session service)
                 req (assoc-in req [:session ::oauth/session] sesh)]
             (assoc (ring.util.response/redirect (:url sesh))
                    :session (:session req)))))))

(def app (compojure.handler/site app))

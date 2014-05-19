(ns qarth.examples.ring
  (require (qarth [oauth :as oauth] util)
           ; Require to make scribe work
           qarth.impl.scribe
           ring.util.response
           compojure.handler
           ring.adapter.jetty)
  (:gen-class)
  (use compojure.core))
; A verbose example, not using any helpers or custom middleware,
; to show you how you might tie Qarth OAuth into Ring in your own way.

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:yahoo conf)
                                 :type :scribe
                                 :provider org.scribe.builder.api.YahooApi
                                 :callback "http://localhost:3000/oauth-callback")))

(defroutes app
  (GET "/" req
       (let [sesh (get-in req [:session ::oauth/session])]
         (if (oauth/is-active? sesh)
           (let [user-guid (->
                             (oauth/request-raw
                               service sesh
                               {:url "https://social.yahooapis.com/v1/me/guid"})
                             :body
                             clojure.xml/parse
                             :content first :content first)]
             (str "<html><body>Your Yahoo! id is: " user-guid "</body></html>"))
           (let [sesh (oauth/new-session service)
                 req (assoc-in req [:session ::oauth/session] sesh)]
             (assoc (ring.util.response/redirect (:url sesh))
                    :session (:session req))))))
  (GET "/oauth-callback" req
       (let [sesh (get-in req [:session ::oauth/session])
             sesh (oauth/verify service sesh
                                (oauth/extract-verifier service sesh req))
             req (assoc-in req [:session ::oauth/session] sesh)]
         ; If success
         (if (oauth/is-active? sesh)
           (assoc (ring.util.response/redirect "/") :session (:session req))
           (let [sesh (oauth/new-session service)
                 req (assoc-in req [:session ::oauth/session] sesh)]
             (assoc (ring.util.response/redirect (:url sesh))
                    :session (:session req)))))))

(def app (compojure.handler/site app))

(defn -main [& args] (ring.adapter.jetty/run-jetty app {:port 3000}))

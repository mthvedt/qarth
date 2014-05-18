(ns qarth.examples.friend
  (require (qarth [oauth :as oauth]
                  util friend)
           qarth.impl.scribe
           cemerick.friend
           compojure.handler
           ring.util.response
           ring.adapter.jetty)
  (use compojure.core))

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:yahoo conf)
                                 :type :scribe
                                 :provider org.scribe.builder.api.YahooApi
                                 :callback "http://localhost:3000/callback")))

; TODO: qarth.oauth -> qarth.auth?
; TODO multi callback sitaution? use service callback?
(def workflow
  (qarth.friend/workflow {:service service}))

; TODO user principal cred fn
(defn cred-fn [auth-map]
  (prn "Getting user principal")
  (when-let [user-guid (-> (oauth/request-raw
                           service auth-map
                           {:url "https://social.yahooapis.com/v1/me/guid"})
                       :body
                       clojure.xml/parse
                       :content first :content first)]
    (prn "Got user principal " user-guid)
    {:identity user-guid :roles [::user]}))

(defroutes app
  (GET "/" _
       (cemerick.friend/authorize #{::user}
                                  (str "<html><body>Hello friend!</body></html>")))
  (GET "/login" _
       (ring.util.response/redirect "/auth/doauth")))

(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :credential-fn cred-fn})
    compojure.handler/site))

(defn -main [& args] (ring.adapter.jetty/run-jetty app {:port 3000}))

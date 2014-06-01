(ns qarth.examples.friend
  (require (qarth [oauth :as oauth]
                  util friend)
           qarth.impl.yahoo
           cemerick.friend
           compojure.handler
           ring.util.response
           ring.adapter.jetty)
  (use compojure.core))

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:yahoo conf)
                                 :type :yahoo.com
                                 :callback "http://localhost:3000/login")))

(def workflow
  (qarth.friend/workflow {:service service}))

(defroutes app
  (GET "/" req
       (cemerick.friend/authorize
         #{::user}
         (do
           (prn (qarth.friend/auth-record req))
           (str "<html><body>Hello friend!</body></html>")))))

(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :auth-url "/login"
                                   :credential-fn #(assoc % :roles [::user])})
    compojure.handler/site))

(defn -main [& args] (ring.adapter.jetty/run-jetty app {:port 3000}))

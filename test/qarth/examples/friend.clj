(ns qarth.examples.friend
  (require (qarth [oauth :as oauth]
                  util friend)
           qarth.impl.github
           cemerick.friend
           compojure.handler
           ring.adapter.jetty)
  (use compojure.core))

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:github.com conf)
                                 :type :github.com
                                 :callback "http://localhost:3000/login")))

(def workflow
  (qarth.friend/oauth-workflow {:service service
                                :login-failure-handler
                                (fn [_] (ring.util.response/redirect
                                          "/login?exception=true"))}))

(defroutes app
  (GET "/" req
       (cemerick.friend/authorize
         #{::user}
         (let [id (-> req (qarth.friend/requestor service) oauth/id)]
           (str "<html><body>Hello friend! Your unique user ID is "
                id
                "</body></html>")))))

(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :auth-url "/login"
                                   :credential-fn #(assoc % :roles [::user])})
    compojure.handler/site))

(defn -main [& args] (ring.adapter.jetty/run-jetty app {:port 3000}))

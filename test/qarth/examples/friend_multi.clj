(ns qarth.examples.friend-multi
  (require (qarth [oauth :as oauth]
                  util friend)
           (qarth.impl yahoo facebook github google)
           cemerick.friend
           compojure.handler
           ring.util.response
           ring.adapter.jetty)
  (use compojure.core)
  (:gen-class))

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build {:type :multi
                           :services conf
                           :options {:callback "http://localhost:3000/auth"}}))

(def workflow
  (qarth.friend/oauth-workflow {:service service
                                :login-failure-handler
                                (fn [_] (ring.util.response/redirect
                                          "/login?exception=true"))}))

(defroutes app
  (cemerick.friend/logout
    (GET "/login" [exception]
         (str "<html><head/><body>"
              (if exception "Oops... there was a problem logging you in" "")
              "<p><a href=\"/auth?service=yahoo.com\">Login with Yahoo!</p>"
              "<p><a href=\"/auth?service=facebook.com\">Login with Facebook</p>"
              "<p><a href=\"/auth?service=github.com\">Login with Github</p>"
              "<p><a href=\"/auth?service=google.com\">Login with Google</p>"
              "</body></html>")))
  (GET "/" req
       (cemerick.friend/authorize
         #{::user}
         (let [id (-> req (qarth.friend/requestor service) oauth/id)]
           (str "<html><body>Hello friend! Your unique user ID is "
                id
                "</body></html>")))))

(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :auth-url "/auth"
                                   :credential-fn #(assoc % :roles [::user])})
    compojure.handler/site))

(defn -main [& args] (ring.adapter.jetty/run-jetty app {:port 3000}))

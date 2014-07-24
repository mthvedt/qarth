(ns qarth.examples.friend-multi
  (require (qarth [oauth :as oauth]
                  util friend)
           qarth.impls
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
              (if exception "<p>Oops... there was a problem logging you in</p>" "")
              "<p>If you are using this locally, please note that some of these
              services can only use localhost, and some can only use 127.0.0.1.<br/>
              Mixing up the two might cause mysterious cookie bugs.</p>"
              "<p><a href=\"/auth?service=yahoo\">Login with Yahoo!</p>"
              "<p><a href=\"/auth?service=facebook\">Login with Facebook</p>"
              "<p><a href=\"/auth?service=github\">Login with Github</p>"
              "<p><a href=\"/auth?service=google\">Login with Google</p>"
              "<p><a href=\"/auth?service=twitter\">Login with Twitter</p>"
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

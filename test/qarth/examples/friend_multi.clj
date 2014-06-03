(ns qarth.examples.friend-multi
  (require (qarth [oauth :as oauth]
                  util friend)
           qarth.impl.yahoo
           cemerick.friend
           compojure.handler
           ring.util.response
           ring.adapter.jetty)
  (use compojure.core)
  (:gen-class))

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build {:type :multi
                           ; TODO
                           :services (dissoc conf
                                             :facebook.com :github.com :google.com)
                           :options {:callback "http://localhost:3000/auth"}}))

(def workflow
  (qarth.friend/workflow {:service service}))

(defroutes app
  (GET "/login" _
       (str "<html><head/><body>"
            "<p><a href=\"/auth?service=yahoo.com\">Login with Yahoo!</p>"
            "<p><a href=\"/auth?service=facebook.com\">Login with Facebook</p>"
            "<p><a href=\"/auth?service=github.com\">Login with Github</p>"
            "<p><a href=\"/auth?service=google.com\">Login with Google</p>"
            "</body></html>"))
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

(ns qarth.examples.oob
  (:gen-class)
  (require [qarth.oauth :as oauth]
           qarth.util
           qarth.impl.scribe
           clojure.string
           clojure.data.xml))

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:yahoo.com conf)
                                 :type :scribe
                                 :provider org.scribe.builder.api.YahooApi)))

(defn -main [& args]
  (let [rec (oauth/new-record service)
        _ (println "Auth url:" (:url rec))
        _ (print "Enter token: ")
        _ (flush)
        token (clojure.string/trim (read-line))
        rec (oauth/activate service rec token)
        user-guid (-> ((oauth/requestor service rec)
                         {:url "https://social.yahooapis.com/v1/me/guid"})
                    :body clojure.data.xml/parse :content first :content first)
        _ (println "user-guid:" user-guid)
        user-info (-> ((oauth/requestor service rec)
                         {:url (str "https://social.yahooapis.com/v1/user/"
                                    user-guid "/profile")})
                    :body clojure.data.xml/parse :content)
        _ (println "user info:" (pr-str user-info))]))

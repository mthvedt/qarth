(ns qarth.examples.oob
  (:gen-class)
  (require [qarth.oauth :as oauth]
           qarth.util
           qarth.impl.scribe
           clojure.string
           clojure.data.xml))

(def conf (qarth.util/read-resource "keys.edn"))

(def service (oauth/build (assoc (:yahoo conf)
                                 :type :scribe-v1
                                 :provider org.scribe.builder.api.YahooApi)))

(defn -main [& args]
  (let [rec (oauth/new-record service)
        _ (println "Auth url:" (:url rec))
        _ (print "Enter token: ")
        _ (flush)
        token (clojure.string/trim (read-line))
        rec (oauth/activate service rec token)
        resp ((oauth/requestor service rec)
                {:url "https://social.yahooapis.com/v1/me/guid"})
        user-guid (-> resp :body clojure.data.xml/parse-str
                    :content first :content first)
        _ (println "response status:" (:status resp))
        _ (println "response headers:" (pr-str (:headers resp)))
        _ (println "user-guid:" user-guid)
        resp ((oauth/requestor service rec)
                {:url (str "https://social.yahooapis.com/v1/user/"
                           user-guid "/profile")
                          :as :stream})
        user-info (-> resp :body clojure.data.xml/parse :content)
        _ (println "response status:" (:status resp))
        _ (println "response headers:" (pr-str (:headers resp)))
        _ (println "user info:" (pr-str user-info))]))

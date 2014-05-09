(ns qarth.test
  (:gen-class)
  (require qarth.oauth.common qarth.oauth.scribe)
  (use qarth.oauth))

(def keys (qarth.oauth.common/read-resource "keys.edn"))

; TODO way to build multi-services
(def service (build (assoc (:yahoo keys) :type :scribe
                           :provider org.scribe.builder.api.YahooApi)))

(defn -main [& args]
  (prn keys)
  (prn service))

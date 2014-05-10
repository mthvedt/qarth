(ns qarth.test
  (:gen-class)
  (require qarth.util qarth.impl.scribe)
  (use qarth.oauth))

(def keys (qarth.util/read-resource "keys.edn"))

; TODO way to build multi-services
(def service (build (assoc (:yahoo keys) :type :scribe
                           :provider org.scribe.builder.api.YahooApi)))

(defn -main [& args]
  (prn keys)
  (prn service))

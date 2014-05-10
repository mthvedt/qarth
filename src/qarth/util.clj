(ns qarth.util
  "Some utility fns that are not part of Qarth, but you may find useful."
  (require [clojure.java.io :as io])
  (use [slingshot.slingshot :only [try+ throw+]]))

(defn read-resource [name]
  (binding [*read-eval* false] 
    (if-let [ff (io/resource name)]
      (with-open [rdr (java.io.PushbackReader. (io/reader ff))] 
        (read rdr))
      (throw+ (str "Could not read " name)))))

(ns qarth.util
  "Some utility fns that are not important parts of Qarth, but you may find useful."
  (require [clojure.java.io :as io]))

(defn read-resource
  "Reads the resource identified by the given name, with read-eval set to false.
  Throws an exception upon failure."
  [name]
  (binding [*read-eval* false] 
    (if-let [ff (io/resource name)]
      (with-open [rdr (java.io.PushbackReader. (io/reader ff))] 
        (read rdr))
      (throw (RuntimeException. (str "Could not open resource: " name))))))

(ns qarth.oauth.support
  "Support fns for qarth.oauth. You probably shouldn't need to use this namespace."
  (:refer-clojure :exclude [derive]))

(def ^{:doc "The OAuth hierarchy ref."}
  h
  (atom (make-hierarchy)))

(defn type-first
  "Returns the :type of the first arg. Used in OAuth multimethods."
  [x & _]
  (:type x))

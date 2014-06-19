(ns qarth.support
  "Support fns for qarth, for implementing or extending behavior."
  (require crypto.random))

(def ^{:doc "The OAuth hierarchy ref."}
  h
  (atom (make-hierarchy)))

(defn type-first
  "Returns the :type of the first arg. Used in some multimethods."
  [x & _]
  (:type x))

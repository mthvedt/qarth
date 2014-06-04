(ns qarth.lib
  "Support fns for qarth, for implementing or extending behavior."
  (:refer-clojure :exclude [derive]))

(def ^{:doc "The OAuth hierarchy ref."}
  h
  (atom (make-hierarchy)))

(defn type-first
  "Returns the :type of the first arg. Used in OAuth multimethods."
  [x & _]
  (:type x))

(defn derive
  "Add a type to the Qarth hierarchy."
  ([type] (derive type :any))
  ([type parent] (swap! h clojure.core/derive type parent)))

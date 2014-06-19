(ns qarth.auth
  ; TODO requestors? should they be fns?
  "Core NS for Qarth auth. Right now this is just some support
  for qarth.oauth and implementations."
  (require qarth.support)
  (:refer-clojure :exclude [derive]))

(defn derive
  "Add a type to the Qarth hierarchy."
  ([type] (derive type :any))
  ([type parent] (swap! qarth.support/h clojure.core/derive type parent)))

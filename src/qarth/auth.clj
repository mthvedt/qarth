(ns qarth.auth
  "Core NS for Qarth auth. Right now this is just some support
  for qarth.oauth and implementations."
  (require qarth.support)
  (use slingshot.slingshot)
  (:refer-clojure :exclude [derive]))

(defn derive
  "Add a type to the Qarth hierarchy."
  ([type] (derive type :any))
  ([type parent] (swap! qarth.support/h clojure.core/derive type parent)))

(defn unauthorized
  "Throw an unauthorized Slingshot exception.
  Use this in preference to throw+, since that leaks environment information
  like API keys."
  [message]
  (throw+ {::unauthorized true} message))

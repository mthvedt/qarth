(ns qarth.lib
  "Common fns for auth implementations.
  Designed to have a stable API, so you can use them also."
  (require [qarth.oauth :as oauth]
           qarth.oauth.support
           crypto.random
           [clojure.tools.logging :as log]
           clojure.xml)
  (:refer-clojure :exclude [derive]))

(defn derive
  "Add a type to the Qarth auth hierarchy."
  ([type] (derive type :any))
  ([type parent] (swap! qarth.oauth.support/h clojure.core/derive type parent)))

(defn csrf-token
  "Returns a random 6-byte CSRF token suitable for use in HTTP params."
  []
  (String. (.getBytes (clojure.string/replace (crypto.random/base64 6) \/ \_))
           "UTF-8"))

(defn success?
  "True if the given object is a number that 
  represents a successful HTTP status code."
  [code]
  #{200 201 202 203 204 205 206 207 300 301 302 303 307} code)

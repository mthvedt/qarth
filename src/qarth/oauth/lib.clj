(ns qarth.oauth.lib
  "Common fns for oauth implementations.
  Designed to have a stable API, so you can use them also."
  (require [qarth.oauth :as oauth]
           qarth.oauth.support
           crypto.random
           clojure.xml)
  (:refer-clojure :exclude [derive])
  (use [slingshot.slingshot :only [try+ throw+]]))

(defn derive
  "Add to the Qarth OAuth hierarchy."
  [child parent]
  (swap! qarth.oauth.support/h clojure.core/derive child parent))

(defn csrf-token
  "A UTF-8 CSRF token."
  []
  (String. (.getBytes (clojure.string/replace (crypto.random/base64 60) \/ \_))
           "UTF-8"))

(defmacro parse-body
  "A macro intended to be used to implement oauth/request.
  Binds the given binding symbol to the given string body, executes the given code,
  and throws, in a Slingshot wrapper, {:body body} if an Exception occurs."
  [[binding-sym str-body] & code]
  `(let [body# ~str-body ~binding-sym body#]
     (try+
       ~@code
       (catch Exception e#
         (throw+ {:body body#} "Error parsing response body")))))

(defmacro def-request-xml
  "Defines an implementation of qarth.oauth/request that parses the body into XML."
  [dispatch-type]
  `(defmethod oauth/request ~dispatch-type [service# session# url# params# opts#]
     (parse-body [body# (oauth/request-raw service# session# url# params# opts#)]
                 (-> body# (.getBytes "UTF-8") java.io.ByteArrayInputStream.
                   clojure.xml/parse))))

(defn success?
  "True if the given object represents a successful HTTP status code."
  [code]
  #{200 201 202 203 204 205 206 207 300 301 302 303 307} code)

;TODO
#_(defmacro def-request-json
    "Def an implementation of qarth.oauth/request
    that parses the body into JSON."
    [dispatch-val]
    `(defmethod oauth/request ~dispatch-val [session url params opts]
       (parse-body [body# (oauth/request-raw session url params opts)]
                   (json/read-json body#))))

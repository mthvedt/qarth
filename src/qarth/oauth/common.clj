(ns qarth.oauth.common
  "Common fns for oauth implementations.
  Designed to have a stable API, so you can use them also."
  (require [qarth.oauth :as oauth]
           [clojure.java.io :as io]
           crypto.random
           clojure.xml)
  (use [slingshot.slingshot :only [try+ throw+]]))

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
         (throw+ {:body body#} "Error parsing returned body")))))

; TODO logging?
(defmacro make-xml-requestor
  "Defines an implementation of zim.oauth.common/request that parses the body into XML."
  [dispatch-type]
  `(defmethod oauth/request ~dispatch-type [service# session# url# params# opts#]
     (parse-body [body# (oauth/request-raw service# session# url# params# opts#)]
                 (-> body# (.getBytes "UTF-8") java.io.ByteArrayInputStream.
                   clojure.xml/parse))))

;TODO
#_(defmacro def-json-requestor
    "Def an implementation of zim.oauth.common/request
    that parses the body into JSON."
    [dispatch-val]
    `(defmethod oauth/request ~dispatch-val [session url params opts]
       (parse-body [body# (oauth/request-raw session url params opts)]
                   (json/read-json body#))))

; TODO move to support or util
(defn read-resource [name]
  (binding [*read-eval* false] 
    (if-let [ff (io/resource name)]
      (with-open [rdr (java.io.PushbackReader. (io/reader ff))] 
        (read rdr))
      (throw+ (str "Could not read " name)))))

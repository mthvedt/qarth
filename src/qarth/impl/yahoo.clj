(ns qarth.impl.yahoo
  "Implementation for Yahoo! The type is :yahoo.com."
  (require (qarth [oauth :as oauth]
                  [lib :as lib])
           qarth.impl.scribe
           clojure.xml))

(qarth.impl.scribe/extend-scribe :yahoo.com org.scribe.builder.api.YahooApi)

(defmethod oauth/extract-verifier :yahoo.com
  [service record {{problem :oauth_problem} :params :as req}]
  (if problem
    (throw (RuntimeException. "Yahoo! returned error: %s" problem))
    (oauth/extract-verifier (assoc service :type :scribe) record req)))

(defmethod oauth/id :yahoo.com
  [requestor]
  (-> (requestor {:url "https://social.yahooapis.com/v1/me/guid"})
    :body clojure.xml/parse
    :content first :content first))

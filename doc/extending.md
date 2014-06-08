# How to make your own Qarth implementations

To create an oauth implementation you need to define the following four methods:
* build
* new-record
* extract-verifier
* verify

The following methods have default implementations that you might also need to change:
* requestor

For more information, see qarth/oauth.clj and qarth/oauth/lib.clj. An example
implementation can be found in qarth/impl/github.clj.

TODO links

TODO documentation

## Extending Scribe

Qarth comes with a default implementation for the Java library, Scribe.
However, Scribe doesn't handle OAuth callbacks.

To provide a specific implementation you need only extend :scribe 
and provide the method to handle
OAuth callbacks, which is qarth.oauth/extract verifier.

For example, here's how it works for Yahoo!:

```clojure
(qarth.impl.scribe/extend-scribe :yahoo.com org.scribe.builder.api.YahooApi)

(defmethod oauth/extract-verifier :yahoo.com
  [service record request]
  (lib/do-extract-verifier (-> record :request-token first)
                           request :oauth-token :oauth-verifier :oauth_problem))

(qarth.impl.scribe/extend-scribe :yahoo.com org.scribe.builder.api.YahooApi)

(defmethod oauth/extract-verifier :yahoo.com
  [service record request]
  (qarth.oauth.lib/do-extract-verifier
    (-> record :request-token first)
    request :oauth-token :oauth-verifier :oauth_problem))
```

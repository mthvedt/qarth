# Qarth

Qarth is yet another OAuth library. It has the following features:

* Designed for the most common flow--logging in a user to an app.
* As simple as is reasonable, but no simpler. No clever tricks or hacks.
* Implementations using multimethods, because OAuth providers differ in details.
You can extend them yourself also.
* Simple Ring integration. You can use Friend, or vanilla Ring.
* Implementation for the popular Java OAuth library, Scribe. Any of the 40+
Scribe OAuth implementations are usable through Qarth.

## Usage

See below for usage with Friend, Ring (no Friend), and
'plain old vanilla' usage.

### A Friend app

TODO: Not yet implemented

```clojure
; Create a Qarth service.
; Add it to Friend, and... you're done.
```

### A Ring app (no Friend)

TODO: Not yet implemented

```clojure
; Create an OAuth callback.
; Create a Qarth service.
; If a user is not logged in, invite him to login.
; Otherwise, display some user information.
```

TODO switch yahoo/scribe order?

### A plain old vanilla app
```clojure
; Create a Qarth service
; in this case, using Java Scribe to talk to Yahoo.
(def conf {:type :scribe
           :provider org.scribe.builder.api.YahooApi
           :api-key "my key"
           :api-secret "my-secret"})

(def service (build conf))

(def sesh (request-session service))
(println ("Auth url: " (:url sesh)))
; The user can get the verification token from this url (no callback)
(print "Enter token: ") (flush)
(def sesh (verify-session service sesh (clojure.string/trim (read-line))))

; Make some requests!
(def user-guid (->
				(request-raw service sesh
				 {:url "https://social.yahooapis.com/v1/me/guid"})
				:body
				clojure.xml/parse
				:content first :content first)

; We could also have used the ns 'qarth.impl.yahoo,
; which has a special implementation of 'request to save us some typing.
(def conf {:type :yahoo
           :api-key "my key"
           :api-secret "my-secret"})

(def service (build conf))

(def sesh (request-session service))
(println ("Auth url: " (:url sesh)))
(print "Enter token: ") (flush)
(def sesh (verify-session service sesh (clojure.string/trim (read-line))))

(def user-guid (->
				(request service sesh
				 {:url "https://social.yahooapis.com/v1/me/guid"})
                :content first :content first))
```

TODO put examples in separate folder

All examples live in (link).

## Implementations included

Qarth has a generic implementation for Scribe.
the most popular JVM Oauth library. Any Scribe implementation
can be used with Qarth.
You can add your own behavior also, using the built-in multimethods.

Also, more specific implementations for Facebook, Github, Yahoo and Google
are provided.

## Details

Qarth has three ideas:

* Services. Services are the master configuration objects.
They contain sensitive information like API keys and should
only live in application state.
* States. States represent delegated access. They contain
stuff like OAuth keys, must be serializable and edn read/writable,
and are intended for things like HTTP sessions.
* Requestors. State + service = requestor. You use these
to make requests, like OAuth requests.
Currently vanilla requestors are lacking
some features, but some libs implement clj-http-requestor
which provides a clj-http compatible API.

TODO create codox.

For more, see the (link) codox.

## Implement your own

TODO doc/extending.md is not yet written.

Most of Qarth is implemented using multimethods,
which allow the use of inheritance.
See doc/extending.md for more.

## License

Copyright © 2014 Zimilate, Inc.; Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

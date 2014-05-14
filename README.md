# Qarth

Qarth is yet another OAuth library. It has the following features:

* Simple facade oriented towards the 99% use case of OAuth:
getting and remembering OAuth access tokens.
* As simple as is reasonable, but no simpler. No clever tricks or hacks.
* Multimethod-based, because OAuth providers differ in details.
Adding, changing, or providing new implementations for behavior is nearly trivial.
* Simple Ring integration. You can use Friend, or vanilla Ring.
* Implementation for the popular Java OAuth library, Scribe.
Any of the 40+ OAuth services supported by Scribe are now available through Qarth.

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

(def sesh (new-session service))
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

(def sesh (new-session service))
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

TODO create codox

## Roll your own multimethods

Say you want to get a unique userid associated with a logged-in OAuth user.
The punch line of Qarth is that all OAuth sessions carry a :type field.
With this it becomes easy to dispatch on type. If you have users that
can use Facebook and Yahoo! you might write:

```clojure
TODO
```

In fact this is basically what is done in qarth/principals.clj (TODO).

## Implementations included

Qarth has a generic implementation for Scribe.
the most popular JVM Oauth library. Any Scribe implementation
can be used with Qarth.
You can add your own behavior also, using the built-in multimethods.

Also, more specific implementations for Facebook, Github, Yahoo and Google
are provided.

For more, see the (link) codox.

## Logging

Qarth uses clojure.tools.logging for logging. TODO LINK

## Implement your own

TODO doc/extending.md is not yet written.

Most of Qarth is implemented using multimethods,
which allow the use of inheritance.
See doc/extending.md for more.

## License

Copyright © 2014 Zimilate, Inc.; Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

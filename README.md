# Qarth

A simple interface to authentication.

Qarth began life as a way to satisfy the 99% use case of OAuth:
to fetch, track, and use permissions from multiple providers.
Through its Scribe (LINK) implementation, Qarth supports 40+ OAuth services
out of the box. Qarth also integrates nicely with the security library Friend (LINK).

## Features so far

* Simple facade for the 99% use case of OAuth.
* Straightforward functional design. No "easy" tricks or hacks. No stupid defaults.
* Multimethod layer, because there is no one-size-fits-all way for auth.
* Single object (actually a map) to contain auth credentials. The map
carries type information, so users can modify, extend, and implement new behavior.
* Implementation for the widely used Java OAuth library, Scribe.
Any of the 40+ OAuth services supported by Scribe are usable through Qarth.
* Friend integration.

Coming soon:

* A 'strategy' based Ring implementation, similar to Ring OmniAuth.
* Support for all kinds of auth, not just OAuth, through the above.

## Rationale

There are many OAuth libraries for Clojure that more or less provide a low-level
prettification of OAuth. Generally these low-level functions are the building
blocks for a higher level library. The problem with this is that
a lot of OAuth is just bookkeeping and knowing APIs, and the APIs
can differ between providers and even go off-spec.
Often, using a low-level OAuth library
is only a small improvement over just doing the HTTP calls yourself.

Qarth's goal is to fill this gap and provide a simple abstraction
for authentication in Clojure.

### A basic app

A command-line app that uses Qarth to fetch an OAuth token.
This shows how the core Qarth abstraction works.

```clojure
; Create an auth service
; in this case, using Java Scribe to talk to Yahoo.
(def conf {:type :scribe
           :provider org.scribe.builder.api.YahooApi
           :api-key "my-key"
           :api-secret "my-secret"})
(def service (build conf))

; Qarth comes with some specific implementations out of the box, also.
(def conf {:type :yahoo.com
           :api-key "my-key"
           :api-secret "my-secret"})
(def service (build conf))

; Creates an unverified auth session.
(def sesh (new-session service))
(println ("Auth url: " (:url sesh)))
; The user can get the verification token from this url (no callback)
(print "Enter token: ") (flush)
(def sesh (verify-session service sesh (clojure.string/trim (read-line))))
; Now we have a verified auth session, and can make requests.
```

### Make requests

```clojure
; For a Qarth sesison called 'sesh', this is how you make a request.
(def user-guid (->
				(request service sesh
				 {:url "https://social.yahooapis.com/v1/me/guid"})
				:body
				clojure.xml/parse
				:content first :content first)
(println "Your Yahoo! user GUID is " user-guid)

; Qarth comes with some built-in multimethods
; for Facebook, Yahoo!, Github, and Google.
(def user-guid (oauth/id sesh))
(println "Your Yahoo! user GUID is " user-guid)
```

### A Friend app

A Ring app that uses Friend to force users to log in with OAuth.

(def workflow (qarth.friend/workflow {:service service}))

TODO a more elaborate example

### Use multiple services

TODO

### Roll your own multimethods

TODO

### More examples

See https://github.com/mthvedt/qarth/tree/master/test/qarth/examples.

### Details

TODO generate API docs.

[API docs](http://mthvedt.github.io/qarth/codox)

See [doc/extending.md](https://github.com/mthvedt/qarth/blob/master/doc/extending.md)
for information on extending Qarth.
Or see the implementations in
https://github.com/mthvedt/qarth/tree/master/src/qarth/impl.

## Implementations included

Qarth has a generic implementation for Scribe.
the most popular JVM Oauth library. Any Scribe implementation
can be used with Qarth.
You can add your own behavior also, using the built-in multimethods.

Also, more specific implementations for Facebook, Github, Yahoo and Google
are provided.

For more, see the codox. TODO CODOX

## Logging

Qarth uses [clojure.tools.logging](https://github.com/clojure/tools.logging)
for logging.

## License

Copyright © 2014 Zimilate, Inc.; Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

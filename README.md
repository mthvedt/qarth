# Qarth

A simple interface to authentication.

Qarth began life as a way to satisfy the 99% use case of OAuth;
to fetch, track, and use permissions from multiple providers.
Through its Scribe (LINK) implementation, Qarth supports 40+ OAuth services
out of the box. Qarth also integrates nicely with the security library Friend (LINK).

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

## Features

* Simple facade for the 99% use case of OAuth.
* Straightforward functional design. No "easy" tricks or hacks. No stupid defaults.
* Multimethod layer, because there is no one-size-fits-all way for auth.
* Single object (actually a map) to contain auth credentials. The map
carries type information, so users can modify, extend, and implement new behavior.
* Implementation for the widely used Java OAuth library, Scribe.
Any of the 40+ OAuth services supported by Scribe are usable through Qarth.
* Friend integration.

### A basic app

TODO put examples in the best order

A command-line app that uses Qarth to fetch an OAuth token.
This shows how the core Qarth abstraction works.

```clojure
; Create an auth service
; in this case, using Java Scribe to talk to Yahoo.
(def conf {:type :scribe
           :provider org.scribe.builder.api.YahooApi
           :api-key "my key"
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
				(request-raw service sesh
				 {:url "https://social.yahooapis.com/v1/me/guid"})
				:body
				clojure.xml/parse
				:content first :content first)
(println "Your Yahoo! user GUID is " user-guid)

; A Ring handler that does the same thing.
TODO
```

### A Friend app

A Ring app that uses Friend to force users to log in with OAuth.
TODO


### Use multiple services

TODO

### Roll your own multimethods

Say you want to get a unique userid associated with a logged-in OAuth user.
The punch line of Qarth is that all OAuth sessions carry a :type field.
With this it becomes easy to dispatch on type. If you have users that
can use Facebook and Yahoo! you might write:

```clojure
TODO
```

In fact this is basically what is done in qarth/principals.clj (TODO NOT YET WRITTEN).

### Codox and other details

See doc/extending.md (TODO NOT YET WRITTEN) for how to extend.

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

## License

Copyright © 2014 Zimilate, Inc.; Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

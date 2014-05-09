# Qarth

Qarth is yet another OAuth library with the following features:

* Simple facade.
* Simple Ring integraiton. You can use Friend, or vanilla Ring.
* Extensible multimethods, because OAuth providers differ in details.

## Usage

See below for usage with Friend, Ring (no Friend), and
'plain old vanilla' usage.

### A Friend app

N.B.: Friend is not yet implemented!

```clojure
; Create a Qarth service.
; Add it to Friend, and... you're done.
```

### A Ring app (no Friend)

```clojure
; Create an OAuth callback.
; Create a Qarth service.
; If a user is not logged in, invite him to login.
; Otherwise, display some user information.
```

### A plain old vanilla app
```clojure
; Create a Qarth service
; in this case, using Java Scribe to talk to Yahoo.

; Create a Qarth state using a verifier token.
; Qarth states can be read/written using Serializable or edn
; and can be stored in Ring sessions.

; Create a Qarth requestor.
; Service + state = requestor. Requestors are ephemeral.

; Make some requests!
```

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

For more, see the (link) codox.

## Implement your own

TODO LINKS

Most of Qarth is implemented using multimethods,
which allow the use of inheritance.
See doc/extending.md for more.

## License

Copyright © 2014 Zimilate, Inc.; Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

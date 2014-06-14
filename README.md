# Qarth

TODO lein/maven coords

A library for using OAuth without setting your hair on fire.

## Overview

* Authenticate users in four lines of code[1].
* Zero-configuration Friend workflows. Authenticate users in one line of code[1].
* Polymorphic OAuth requests--support multiple providers at once
with zero additional code.
* Single credential object with multimethod type information.
Modify, extend, and implement new behavior.

[1] Additional lines of code may apply.

## Rationale

There are several good OAuth libraries for Clojure,
but the OAuth spec is both simple and incomplete, such that
that low-level libraries are barely more useful than doing HTTP calls yourself.
Many OAuth providers have quirks, odd extra tricks, and may even go off-specification.

Qarth is a polymorphic facade using Clojure multilmethods
that satisfies the 99% use case:
to fetch, track, and use permissions from multiple providers. You can
use the same set of methods to talk to any provider or among multiple providers.

Qarth comes with a zero-effort workflow for the
security library [Friend](https://github.com/cemerick/friend).
The workflow supports one or multiple providers.
Qarth also includes multimethods for grabbing user IDs, email, and generic info.

Qarth comes with implementations for Github, Facebook, Google, and Yahoo!, and
makes it easy for you to write your own.

TODO a bit about scribe.

## Features so far

* Simple facade for the 99% use case of OAuth.
* Straightforward functional design. No "easy" tricks or hacks. No stupid defaults.
* Multimethod layer, because there is no one-size-fits-all way for auth.
* Single object (actually a map) to contain auth credentials. The map
carries type information, so users can modify, extend, and implement new multimethods.
* Friend integration.

Coming soon:

* A 'strategy' based Ring implementation, similar to Ruby OmniAuth.
* Support for all kinds of auth, not just OAuth, through the above.

### A basic configuration

Qarth puts all your super-secret information, like API keys and passwords,
in auth services. Auth services are built from ordinary maps.

```clojure
(def conf {:type :yahoo.com
           :callback "http://localhost:3000"
           :api-key "my-key"
           :api-secret "my-secret"})
(def service (qarth.oauth/build conf))
```

### A Friend app

```clojure
(def workflow (qarth.friend/workflow {:service service}))

(defroutes app
  (GET "/" req
       (cemerick.friend/authorize
         #{::user}
         ; A requestor uses verified OAuth credentials to make http requests.
         ; qarth.friend/requestor can get a requestor from the Friend credentials.
         ; oauth/id is a method on requestors.
         (let [id (-> req (qarth.friend/requestor service) oauth/id)]
           (str "<html><body>Hello friend! Your unique user ID is "
                id
                "</body></html>")))))

; To kick off OAuth, redirect users to the :auth-url and Qarth handles the rest.
; Here the :auth-url is "/login", the default Friend redirect.
; But it could also be a landing page.
(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :auth-url "/login"
                                   :credential-fn #(assoc % :roles [::user])})
    compojure.handler/site))
```

### A command-line app

Qarth's basic facade authenticates users in only two multimethod calls.

```clojure
; Assume service is an OAuth service with no callback
(def sesh (new-session (assoc service))
(println ("Auth url: " (:url sesh)))
; Compliant OAuth implementations will show the user a verification token.
(print "Enter token: ") (flush)
(def sesh (verify-session service sesh (clojure.string/trim (read-line))))
(println "Your unique user ID is " (->> sesh (oauth/requestor service) oauth/id))
```

### A basic Ring app

TODO

### Make arbitrary requests

A 'requestor', in addition to being an object with multimethods,
is a vanilla fn that can be used to make arbitrary HTTP requests.

```clojure
; TODO don't use yahoo!, too complicated.
(def requestor (requestor service sesh))
(def user-guid (-> (requestor {:url "https://social.yahooapis.com/v1/me/guid"})
				:body
				clojure.xml/parse
				:content first :content first))
(println "Your user GUID is " user-guid)
; TODO write a more elaborate example.
```

Requestors support many (or all! depending on implementation)
of the options that :clj-http supports.

### Using multiple services

```clojure
; You can define a service containing several sub-services.
(def conf {:type :multi
           :services {:yahoo.com {:api-key "my-key"
                                  :api-secret "my-secret"}
                      :github.com {:api-key "my-key"
                                   :api-secret "my-secret"}}
           ; Options applied to all services
           :options {:callback "http://localhost:3000/auth"}})
(def service (oauth/build conf))

; Works the same as an ordinary service, except for one thing...
; to open a new session takes an extra argument.
(def sesh (new-session service :yahoo.com))

; You can use Friend by adding an extra ?service= query param.
; A basic login page might look like this:
(GET "/login" _
     (str "<html><head/><body>"
          "<p><a href=\"/auth?service=yahoo.com\">Login with Yahoo!</p>"
          "<p><a href=\"/auth?service=github.com\">Login with Github</p>"
          "</body></html>"))
```

### Using Scribe

```clojure
; Any Scribe implementation can be used here.
(def conf {:type :scribe
           :provider org.scribe.builder.api.YahooApi
           :api-key "my-key"
           :api-secret "my-secret"})
(def service (build conf))
```

Scribe covers all features of OAuth EXCEPT extracting verifiers from callbacks.
To implement your own Scribe-based service, just add a multimethod type
and implement the method extract-verifier.

For example, see
https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/yahoo.clj.

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
the most popular JVM Oauth library. You can extend Scribe using one defmethod.
TODO link example.
You can add your own behavior also, using the built-in multimethods.

Also, more specific implementations for Facebook, Github, Yahoo and Google
are provided.

For more, see the codox. TODO CODOX

## Logging

Qarth uses [clojure.tools.logging](https://github.com/clojure/tools.logging)
for logging.

## License

Copyright © 2014 Zimilate, Inc., Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

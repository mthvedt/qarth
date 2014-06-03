# Qarth

TODO lein/maven coords

A simple interface to authentication.

Qarth began life as a way to satisfy the 99% use case of OAuth:
to fetch, track, and use permissions from multiple providers.
Through its [Scribe](https://github.com/fernandezpablo85/scribe-java) implementation, Qarth supports 40+ OAuth services
out of the box. Qarth also integrates nicely with the security library
[Friend](https://github.com/cemerick/friend).

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

* A 'strategy' based Ring implementation, similar to Ruby OmniAuth.
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

### A basic Ring app

TODO

### A Friend app

```clojure
; Create an auth service
(def conf {:type :yahoo.com
           :callback "http://localhost:3000"
           :api-key "my-key"
           :api-secret "my-secret"})
; An auth service contains all your super-secret information, like API passwords.
; Qarth separates auth services, which contain your darkest secrets,
; from auth records, which are kept in Ring sessions and contain user information.
(def service (qarth.oauth/build conf))

(def workflow (qarth.friend/workflow {:service service}))

; A requestor uses verified OAuth credentials to make http requests.
; oauth/id is a method on requestors.
; Here we grab the requestor from our verified OAuth credentials
; and request a user ID.
(defroutes app
  (GET "/" req
       (cemerick.friend/authorize
         #{::user}
         (let [id (-> req (qarth.friend/requestor service) oauth/id)]
           (str "<html><body>Hello friend! Your unique user ID is "
                id
                "</body></html>")))))

; Using the workflow is simple. To kick off OAuth,
; just redirect users to the :auth-url and Qarth handles the rest.
; Here the :auth-url is "/login", the default Friend redirect.
(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :auth-url "/login"
                                   :credential-fn #(assoc % :roles [::user])})
    compojure.handler/site))
```

### A command-line app

Qarth's basic facade is very simple. Here's how to use it:

```clojure
; Assume service is an OAuth service with no callback
(def sesh (new-session (assoc service))
(println ("Auth url: " (:url sesh)))
; Compliant OAuth implementations will show the user a verification token.
(print "Enter token: ") (flush)
(def sesh (verify-session service sesh (clojure.string/trim (read-line))))
(println "Your unique user ID is " (->> sesh (oauth/requestor service) oauth/id))
```

With only a few steps, you can make your own auth systems
if you don't like the provided Friend implementation.
See the oauth.clj and ring.clj namespaces for more information.
TODO generate codox for these namespaces.

### Make arbitrary requests

```clojure
; A 'requestor', in addition to being an object with multimethods,
; is a vanilla fn that can be used to make arbitrary HTTP requests.
; TODO don't use yahoo!, too complicated.
(def requestor (requestor service sesh))
(def user-guid (-> (requestor {:url "https://social.yahooapis.com/v1/me/guid"})
				:body
				clojure.xml/parse
				:content first :content first))
(println "Your user GUID is " user-guid)
; Requestors support many (or all! depending on implementation)
; of the options that :clj-http supports.
; TODO write a more elaborate example.
```

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

You can also implement your own using Scribe. For example, see
https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/yahoo.clj.

### Roll your own multimethods

TODO

### Implement your own Qarth service

TODO

### More examples

See https://github.com/mthvedt/qarth/tree/master/test/qarth/examples.

### Details

TODO generate API docs.

[API docs](http://mthvedt.github.io/qarth/codox)

See [doc/extending.md](https://github.com/mthvedt/qarth/blob/master/doc/extending.md)
for information on extending Qarth. TODO write this.
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

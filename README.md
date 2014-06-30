# Qarth

```
[qarth "0.1.0-SNAPSHOT"]
```

For managing OAuth without setting your hair on fire. Features Friend integration.

## Overview

There are several good OAuth libraries for Clojure,
but the OAuth spec is both simple and incomplete, such that
that low-level libraries are barely more useful than doing HTTP calls yourself.
Many OAuth providers have extra quirks and off-spec behavior.
(So far, I haven't found an OAuth provider
that is even 100% compatible with the spec.)

Qarth is a polymorphic facade using Clojure multilmethods
that satisfies the 99% use case:
to fetch, track, and use permissions from multiple providers.
Authenticate users in three lines of code plus configuration[1].

[1] Additional lines of code may apply.

Qarth also offers the following features:

* Zero-effort [Friend](https://github.com/cemerick/friend) workflows.
* Straightforward functional design. No "easy" tricks or hacks. No stupid defaults.
* Polymorphic OAuth requests--support any providers,
or multiple providers at once, with no
additional effort. Hides the quirks and off-spec behavior of each OAuth provider.
* Comes with implementations for Github, Yahoo!, Facebook, and Google.
* Standard interface with generic implementations for OAuth2
[Scribe](https://github.com/fernandezpablo85/scribe-java).
Add your own OAuth provider by implementing as few as one method.
* Multimethods for grabbing user IDs from different providers.

## TODO list

* General 'strategies' to allow login without Friend...
* Multimethods for email, userinfo, &c
* More documentation!

### A basic configuration

Qarth puts all your super-secret information, like API keys and passwords,
in auth services. They are configured with ordinary maps such as might come
from a configuration file.

```clojure
(require '[qarth.oauth :as oauth])
(require 'qarth.impl.facebook)
(def conf {:type :facebook.com
           :callback "http://localhost:3000/login"
           :api-key "my-key"
           :api-secret "my-secret"})
(def service (oauth/build conf))
```

### A Friend app

Temporary per-user credentials are stored in auth records.
Auth records are readable and writable maps of data
and can be stored in cookies, sessions, databases, Friend credentials, etc.

```clojure
(def workflow (qarth.friend/oauth-workflow {:service service}))

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
; Here the :auth-url is "/login", the default Friend landing page.
(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :auth-url "/login"
                                   :credential-fn #(assoc % :roles [::user])})
    compojure.handler/site))
```

### A command-line app

Qarth's basic facade authenticates users in two multimethod calls.

```clojure
; Assume 'service is an OAuth service with no callback
(def record (oauth/new-record service))
(println ("Auth url: " (:url record)))
; Compliant OAuth implementations will show the user an authroization code.
(print "Enter token: ") (flush)
(def record (oauth/activate service record (clojure.string/trim (read-line))))
(println "Your unique user ID is " (->> record (oauth/requestor service) oauth/id))
```

### Make arbitrary requests

A 'requestor', in addition to being an object with multimethods,
is a vanilla fn that can be used to make arbitrary HTTP requests.

```clojure
(def requestor (oauth/requestor service record))
(def user-guid (-> (requestor {:url "https://graph.facebook.com/me"})
				:body slurp clojure.data.json/read-str (get :id)))
(println "Your user GUID is " user-guid)
```

Requestors support many (or all! depending on implementation)
of the options that :clj-http supports. They return Ring-style response maps.
(As is usual in web APIs, make sure to fully read and/or close the response body.)

### Using multiple services

```clojure
; You can define a service containing several sub-services.
(require '(qarth.impl yahoo github))
(def conf {:type :multi
           :services {:yahoo.com {:api-key "my-key"
                                  :api-secret "my-secret"}
                      :github.com {:api-key "my-key"
                                   :api-secret "my-secret"}}
           ; Options applied to all services
           :options {:callback "http://localhost:3000/auth"}})
(def service (oauth/build conf))

; Works the same as an ordinary service, except for one thing...
; to open a new record takes an extra argument.
(def record (new-record service :yahoo.com))

; You can use Friend by adding an extra ?service= query param.
; A basic login page might look like this:
(GET "/login" _
     (str "<html><head/><body>"
          "<p><a href=\"/auth?service=yahoo.com\">Login with Yahoo!</p>"
          "<p><a href=\"/auth?service=github.com\">Login with Github</p>"
          "</body></html>"))
```

### A basic Ring app

TODO

### Using OAuth v2

Qarth has a set of default multimethods for OAuth v2.
For any compliant implementation that returns form-encoded responses[2],
all you need to do is something like this:

```clojure
(def conf {:type :oauth
           :request-url "https://www.facebook.com/dialog/oauth"
           :access-url "https://graph.facebook.com/oauth/access_token"))}
(def service (oauth/build conf))
```

In fact, this is exactly how
[Facebook](https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/facebook.clj)
and [Github](https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/github.clj)
are implemented.

You can also override individual multimethods, as seen in the
[Google](https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/google.clj)
implementation (which uses JSON and JWTs instead of form encoding).

Many useful fns can be found in qarth.oauth.lib [TODO LINK].

[2] The OAuth2 spec specifies JSON-encoded responses. However,
it seems to be routine not to follow that part of the spec.

### Using Scribe

Qarth has a generic implementation for
[Scribe](https://github.com/fernandezpablo85/scribe-java),
the most popular OAuth library for the JVM.

```clojure
; Any Scribe implementation can be used here.
(def conf {:type :scribe
           :provider org.scribe.builder.api.YahooApi
           :api-key "my-key"
           :api-secret "my-secret"})
(def service (oauth/build conf))
```

Scribe covers all features of OAuth EXCEPT extracting auth codes from callbacks.
To implement your own Scribe-based service, just add a multimethod type
and implement the method extract-code. If the Scribe-based service
returns standard OAuth v2 form-encoded responses, you don't have to do anything.

For example, see
https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/yahoo.clj.

### Roll your own multimethods

TODO

### More examples

See https://github.com/mthvedt/qarth/tree/master/test/qarth/examples.

### Details

TODO generate API docs.

[API docs](http://mthvedt.github.io/qarth/codox)

TODO write more about extending Qarth.

Implementations you can study are in
https://github.com/mthvedt/qarth/tree/master/src/qarth/impl.

## Implementations included

Qarth includes specific implementations for Facebook, Github, Yahoo, and Google.
Qarth includes generic, extensible implementations for OAuth V2 and Scribe.

For more, see the codox or examples. TODO CODOX

## Logging

Qarth uses [clojure.tools.logging](https://github.com/clojure/tools.logging)
for logging. You can turn on DEBUG for qarth to log important login activity,
and TRACE to see very detailed info on everything that's happening.

TRACE logging logs auth records, if that is a security concern. It does not
log auth services or any private information contained therein.

## License

Copyright © 2014 Zimilate, Inc., Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

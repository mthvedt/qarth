# Qarth

[![Clojars Project](http://clojars.org/qarth/latest-version.svg)](http://clojars.org/qarth)

Qarth is a simple interface to OAuth.
Qarth features zero-effort Friend integration.
The interactive auth flow in "friendless" qarth is two or three lines of code plus configuration.

Qarth comes with out-of-the-box support for Facebook, Github, Twitter, Google, and Yahoo!,
and generic support for OAuth v2 and [Scribe](https://github.com/fernandezpablo85/scribe-java).

## Using Qarth

### A basic configuration

Qarth puts all your super-secret information, like API keys and passwords,
in auth services. They are configured with ordinary maps such as might come
from a configuration file. They are generally not readable or writable.

```clojure
(require '[qarth.oauth :as oauth])
(require 'qarth.impl.facebook) ; Loads the methods for :facebook
(def conf {:type :facebook
           :callback "http://localhost:3000/login"
           :scope "public_profile,email" ; Scopes are optional
           :api-key "my-key"
           :api-secret "my-secret"})
(def service (oauth/build conf))
```

### A Friend app

Temporary per-user credentials are stored in auth records.
Auth records are ordinary Clojure maps
and can be stored in cookies, sessions, databases, Friend credentials, &c.

```clojure
; All you need to create a Qarth workflow is a configured auth service.
(def workflow (qarth.friend/oauth-workflow {:service service}))

(defroutes app
  (GET "/" req
    (cemerick.friend/authorize #{::user}
      "<html><body>Hello world!</body></html>")))

; To kick off OAuth, redirect users to the :auth-url and Qarth handles the rest.
; Here the :auth-url is "/login", the default Friend landing page.
(def app
  (-> app
    (cemerick.friend/authenticate {:workflows [workflow] :auth-url "/login"
                                   :credential-fn #(assoc % :roles [::user])})
    compojure.handler/site))
```

You can make requests with the OAuth information stored in the Friend credentials.

```clojure
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
```

### Make arbitrary requests

A requestor, in addition to being an object with multimethods,
is a vanilla fn that can be used to make arbitrary HTTP requests.

```clojure
(def my-requestor (oauth/requestor service record))
(def user-guid (-> (my-requestor {:url "https://graph.facebook.com/me"})
                                  :body slurp clojure.data.json/read-str (get :id)))
(println "Your user GUID is " user-guid)
```

Obviously these are provider-specific, so the use of multimethods is encouraged
if you want to use multiple providers (see below).

Requestors support many (or all! depending on implementation)
of the options that :clj-http supports. They return Ring-style response maps.
(As is usual in Java, remember to always close your streams at the end.)

### Using multiple services

Qarth has multiservices, which have type `:multi`.
To use multiservices requires a one-line change
to `oauth/new-record`—all other methods work the same.

```clojure
(require 'qarth.impls) ; Loads all methods bundled with Qarth
(def conf {:type :multi
           :services {:yahoo {:api-key "my-key"
                              :api-secret "my-secret"}
                      :github {:api-key "my-key"
                               :api-secret "my-secret"}}
           ; Options applied to all services
           :options {:callback "http://localhost:3000/auth"}})
(def service (oauth/build conf))

; Works the same as an ordinary service, except for one thing...
; to open a new record takes an extra argument.
(def record (oauth/new-record service :yahoo))

; You can use Friend by adding an extra ?service= query param.
; A basic login page might look like this:
(GET "/login" _
     (str "<html><head/><body>"
          "<p><a href=\"/auth?service=yahoo\">Login with Yahoo!</p>"
          "<p><a href=\"/auth?service=github\">Login with Github</p>"
          "</body></html>"))
```

### A command-line app

Qarth's basic facade authenticates users in two multimethod calls.

```clojure
; Assume 'service is an OAuth service with no callback
(def record (oauth/new-record service))
(println ("Auth url: " (:url record)))
; Compliant OAuth implementations will show the user an authorization code.
; Not every OAuth provider supports this.
(print "Enter token: ") (flush)
(def record (oauth/activate service record (clojure.string/trim (read-line))))
(println "Your unique user ID is " (->> record (oauth/requestor service) oauth/id))
```

### Examples

Examples live in [test/qarth/examples](https://github.com/mthvedt/qarth/tree/master/test/qarth/examples). You can run any example file with `lein example <example-class>`. To run them you need to put a `keys.edn` file in [test-resources](https://github.com/mthvedt/qarth/tree/master/test-resources) (see [https://github.com/mthvedt/qarth/blob/master/test-resources/keys-example.edn](this example file)).

## Extending Qarth

### Roll your own OAuth v2 implementation

Qarth has a set of default multimethods for OAuth v2 with form-encoded responses†.
They require only a :request-url and :access-url.

```clojure
(def conf {:type :oauth
           :request-url "https://www.facebook.com/dialog/oauth"
           :access-url "https://graph.facebook.com/oauth/access_token"))}
(def service (oauth/build conf))
```

This is how
[Facebook](https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/facebook.clj)
and [Github](https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/github.clj)
are implemented.

You can also override individual multimethods, as seen in the
[Google](https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/google.clj)
implementation, which uses JSON and JWTs instead of form encoding in the responses.

Useful fns for implementations can be found in [qarth.oauth.lib](https://mthvedt.github.io/qarth/doc/codox/qarth.oauth.lib.html).

† The OAuth v2 spec specifies JSON-encoded responses. However,
it seems to be routine not to follow that part of the spec.

### Roll your own Scribe implementation

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

Scribe for Java covers all features of OAuth EXCEPT extracting auth codes from callbacks (the method `oauth/extract-code`).
Qarth comes with `:scribe` and `:scribe-v1` types that implement `oauth/extract-code` for form-encoded v2 and standard v1, respectively.

For a working example, see the [Yahoo! implementation](https://github.com/mthvedt/qarth/blob/master/src/qarth/impl/yahoo.clj).

### Roll your own multimethods

Qarth multimethods dispatch on `qarth.support/h`. Requestors
already have `:type` metadata on them.
So you can roll your own multimethods as follows:

```clojure
(defmulti my-method
  "my-documentation"
  type :hierarchy qarth.support/h)
```

For instance, you could write some methods to grab user info
(friends lists, name, &c) from various
providers in a polymorphic way.

## Reference

[Examples](https://github.com/mthvedt/qarth/tree/master/test/qarth/examples)

[API docs](http://mthvedt.github.io/qarth/doc/codox).

[Implementations](https://github.com/mthvedt/qarth/tree/master/src/qarth/impl)

## Logging

Qarth uses [clojure.tools.logging](https://github.com/clojure/tools.logging)
for logging. You can turn on DEBUG for qarth to log important login activity,
and TRACE to see very detailed info on everything that's happening.

TRACE logging logs auth records, if that is a security concern. It does not
log auth services or any private information contained therein.

## TODO

* Record refresh and expiration. Currently you must do this manually.
* General 'strategies' to allow login without Friend...
* Multimethods for email, userinfo, &c.
* More documentation!

## Finally…

Qarth is a new library, so please let me know about any bugs, difficulties, or rough edges you encounter. My Freenode IRC name is mthvedt and my email is mike.thvedt@gmail.com.

## License

Thanks to John Schroeder and Anders Hovmöller for valuable help and feedback.

Copyright © 2014 [Zimilate, Inc.](http://zimilate.com), Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

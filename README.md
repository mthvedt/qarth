# Qarth

[![Clojars Project](http://clojars.org/qarth/latest-version.svg)](http://clojars.org/qarth)

Qarth is a simple interface to OAuth.

Qarth can be used with Friend or stand-alone. Friend integration is zero-effort.
Standalone mode only takes a few lines of code and configuration.

Qarth comes with out-of-the-box support for Facebook, Github, Twitter, Google, and Yahoo!,
and generic support for OAuth v2 and [Scribe](https://github.com/fernandezpablo85/scribe-java).

## Using Qarth

This tutorial presupposes a basic familiarity with OAuth.

Qarth encapsulates its functionality in three kinds of objects: services, requestors, and maps.
You can manipulate these yourselves, or plug them into Friend.

### A basic configuration

Qarth is configured using Clojure maps and runs with multimethods.
Qarth auth services are encapsulated in opaque objects called 'services',
which are built from configurations as follows:

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
; Create a Friend workflow from a Qarth service.
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

Friend then stores the OAuth information in its Friend credentials, which can be
used to make OAuth-authenticated requests. This is done with Qarth objects called
requestors.

Requestors are multimethod objects used to make various kinds of requests.
Qarth provides several multimethods you can use. For example:

```clojure
(defroutes app
  (GET "/" req
       (cemerick.friend/authorize
         #{::user}
         ; A requestor uses verified OAuth credentials to make http requests.
         ; qarth.friend/requestor obtains get a requestor from the Friend credentials.
         ; We then call oauth/id on that requestor, which gets the user's unique ID.
         (let [id (-> req (qarth.friend/requestor service) oauth/id)]
           (str "<html><body>Hello friend! Your unique user ID is "
                id
                "</body></html>")))))
```

### A command-line app

Qarth's basic facade authenticates users in two multimethod calls.
We call `oauth/new-record` to crate a Qarth record--actually just a map--
for that service. The `:url` for that service gives an authorization URL.

Here's how it works on the command line:

```clojure
; Assume 'service is an OAuth service with no callback
(def record (oauth/new-record service))
(println ("Auth url: " (:url record)))
; Compliant OAuth implementations will show the user an authorization token
; after they authorize with the given URL.
(print "Enter token: ") (flush)
(def record (oauth/activate service record (clojure.string/trim (read-line))))
(println "Your unique user ID is " (->> record (oauth/requestor service) oauth/id))
```

In practice, of course, you would want to plug this workflow into your webapp somehow.
If you're using Friend, that makes things much easier.

### Interop

For OAuth v2 services, you can grab the `:access-token` from the OAuth record
and use that with third-party libraries such as [clj-facebook-graph](https://github.com/maxweber/clj-facebook-graph).

### Make arbitrary requests

Requestors, when used as fns, can make arbitrary requests.

```clojure
(def my-requestor (oauth/requestor service record))
(def user-guid (-> (my-requestor {:url "https://graph.facebook.com/me"})
                                  :body slurp clojure.data.json/read-str (get :id)))
(println "Your user GUID is " user-guid)
```

Requestors support many (or all! depending on implementation)
of the options that :clj-http supports. They return Ring-style response maps.
The user is responsbile for closing any streams in the returned Ring-style map.

Because OAuth methods differ from provider to provider, we encourage using multimethods
instead of arbitrary requests. That way, you can easily change behavior between providers.

### Using multiple services

Qarth has multiservices, which have type `:multi`.
When using multiservices, when creating a new Qarth record,
you must provide the service name. If using friend, you must provide a GET parameter
named 'service'. For example:

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

Qarth multimethods dispatch on the hierarchy `qarth.support/h`. Requestors
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

[API docs](http://mthvedt.github.io/qarth/doc/codox)

[Implementations](https://github.com/mthvedt/qarth/tree/master/src/qarth/impl)

### Working examples

Examples live in [test/qarth/examples](https://github.com/mthvedt/qarth/tree/master/test/qarth/examples). The examples look for a `keys.edn` file in [test-resources](https://github.com/mthvedt/qarth/tree/master/test-resources) (see [https://github.com/mthvedt/qarth/blob/master/test-resources/keys-example.edn](this example file)).

You can run any example file by cloning this repo and then running `lein example qarth.examples.<example-file.clj>` or `lein exdebug qarth.examples.<example-file.clj>`, provided you put the proper OAuth keys in `test-resources/keys.edn`. Only the lein `example` profile knows about this directory. Qarth itself makes no assumptions about how you supply OAuth keys.

## Logging

Qarth uses [clojure.tools.logging](https://github.com/clojure/tools.logging)
for logging. You can turn on DEBUG for qarth to log important login activity,
and TRACE to see very detailed info on everything that's happening.

TRACE logging logs auth records, if that is a security concern. It does not
log auth services or any private information contained therein.

## TODO

* Auth record refresh and expiration. Currently you must handle these cases yourself.
* Multimethods for email, userinfo, &c.
* OAuth 'strategies' in the style of Ruby OmniAuth, so that we can implement easy workflows other than with Friend. The infrastructure is already there in `ring.clj`.

## Finally…

Qarth is a new library, so please let me know about any bugs, difficulties, or rough edges you encounter. My Freenode IRC name is mthvedt and my email is mike.thvedt@gmail.com.

Special thanks go to John Schroeder and Anders Hovmöller.

## License

Copyright © 2014-2017 [Zimilate, Inc.](http://zimilate.com), Mike Thvedt

Distributed under the Eclipse Public License, the same as Clojure.

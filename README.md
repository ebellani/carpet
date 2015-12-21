# carpet

Web and mobile interface using a single source style.

## Development

Open a terminal and type `lein repl` to start a Clojure REPL
(interactive prompt) already parked at the `carpet.server`
namespace. In there, in order to start the server, run:

```clojure
(run)
```

This does two things: it starts the webserver at port 10555, and also
the Figwheel server which takes care of live reloading ClojureScript
code and CSS. Give them some time to start.

If you want to have a repl into the CLJS dimension, then when the repl
is in the `carpet.server` namespace, run:

```clojure
(dev/browser-repl)
```

This will start a Weasel REPL server, and replace the current REPL for
a CLJS REPL. Evaluating expressions here will only work once you've
loaded the page, so the browser can connect to Weasel. If the page is
already loaded, a refresh is necessary so the client can connect to
the Weasel server.

In order to return to the CLJ dimension, you need to evaluate the
:cljs/quit keyword in the CLJS REPL. Again, if you try to return to
the CLJS REPL by the above instructions you will need to refresh the
page.

## Using the app with sample data

The hardcoded user has credentials: user name: mock, password: mock.
In the `carpet.server` you can run the `start-btc-broadcaster!`
command in order to see the `server>user` push mechanism in
action. This will update the dashboard with some fake data.

## Chestnut

Adapted from [Chestnut](http://plexus.github.io/chestnut/) 0.7.0.

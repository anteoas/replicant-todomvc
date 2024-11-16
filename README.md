# The Replicant TodoMVC Example

An implementation of the [TodoMVC](http://todomvc.com) app using the [Replicant](https://github.com/cjohansen/replicant) library.

***Replicant***: _A native [ClojureScript](https://clojurescript.org) virtual DOM renderer - render hiccup directly_

The example shows a way (of many) to wire up a Replicant app, with a focus on keeping the views as pure data. We can do this because replicant supports pure data dom event handlers and vdom lifecycle hooks.

To understand a bit of where Replicant (and this app example) comes from, please watch Christian Johansen's talk at JavaZone 2023, [Stateless, Data-driven UIs](https://2023.javazone.no/program/85f23370-440f-42b5-bf50-4cb811fef44d).

To understand a bit more about the code in this example, read the more comprehensive README of the [Replicant mini-app](https://github.com/anteoas/replicant-mini-app) example.

## Running the app in development mode

Prerequisites:

- [Node.js](https://nodejs.org) (or something node-ish enough)
- [Java](https://adoptopenjdk.net)
- [Clojure CLI](https://clojure.org/guides/getting_started)

We're using [shadow-cljs](https://github.com/thheller/shadow-cljs) to build the app. Clojure editors like [Calva](https://calva.io) and [CIDER](https://cider.mx/) will let you quickly start the app and connect you to its REPL. You can also just run it without installing anything, by using npx:

```sh
npx shadow-cljs watch :app
```

Once built, you can access the app at http://localhost:8585


To start the app with a Clojure editor friendly nREPL server, run:

```sh
npx shadow-cljs -d cider/cider-nrepl:0.50.2 watch :app
```

## Licence

Public Domain, **Unlicense**. See [LICENSE.md](LICENSE.md).

## Happy coding! ♥️

Please file issues if you have any questions or suggestions. Pull requests are also welcome (but please file an issue first if your PR would be beyond things like fixing typos). 🙏

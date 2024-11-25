# The Replicant TodoMVC Example

An implementation of the [TodoMVC](http://todomvc.com) app using the [Replicant](https://github.com/cjohansen/replicant) library. You can try the app here:

* https://anteoas.github.io/replicant-todomvc/

***Replicant***: _A native [ClojureScript](https://clojurescript.org) virtual DOM renderer - render hiccup directly_

Replicant is a [Clojure](https://clojure.org) and ClojureScript library. Working with it has many similarities with React, but it is not using React and thus not depending on React and everything that React depends on. In fact Replicant has zero/zilch/no dependencies.

The example shows a way (of infinitely many) to wire up a Replicant app, with a focus on keeping the views as pure data. We can do this because replicant supports pure data DOM event handlers and VDOM lifecycle hooks.

To understand a bit of where Replicant comes from, please watch Christian Johansen's talk at JavaZone 2023, [Stateless, Data-driven UIs](https://2023.javazone.no/program/85f23370-440f-42b5-bf50-4cb811fef44d).

[![Stateless, Data-driven UIs](stateless-data-driven-uis.png)](https://2023.javazone.no/program/85f23370-440f-42b5-bf50-4cb811fef44d)

## General implementation notes

As stated above, Replicant is a library. It is _not a framework_. It only concerns itself with rendering to the DOM (from its own variant of [Hiccup](https://github.com/weavejester/hiccup)), implementing a very efficient virtual DOM. While rendering, Replicant helps with dispatching DOM events and lifecycle hooks to the app. The app needs to implement an event handler. From there, Replicant has no further requirements. You can say that Replicant is satisfied with requiring you to work in functional and data-oriented way.

When dispatching events, Replicant allows to dispatch data instead of functions. It allows functions too, but we are using the data oriented approach in this app. This keeps our views not only pure functions, but also fully serializable, and [very testable](#testing). We arrange the data from the events in vectors of `actions`, where each `action` also is a vector, with the first element being the action name, and the rest is action arguments.

### The app state

The app state is a Clojure atom where the current state is stored as a map.

### App start

When the applications starts we initialize the app state, Replicant, and the router. The state is populated from local storage, or, lacking that, a default map. Both the router and Replicant are initialized with the event handler, meaning that both can dispatch events.

Next we register a watcher on the app state. This watcher will be called whenever the state changes. We use this to trigger a re-render of the app and also for persisting the state to local storage.

Last thing in the app start is to render the app with the initial state.

### The event handler

Replicant will provide our event handler with our event data (`actions`), plus some information about the event itself. We use this information to “enrich” the event data, so that our action handlers have information about things like the event source and the event target.

For each action, the event handler calls the action handler which returns a new state and/or effects to be performed. The event handler will replace the app state with any new state, and/or call the effect handler if there with any effects.

### Action handler

The action handler dispatches on the action name and returns a new state and/or `effects`. `effects` is a vector, similar to the `actions` vector. An `effect` is also a vector, similar to an `action` vector. This makes the action handler a pure function, that can be easily tested.

#### The actions

Actions can be big and monolithic or small and composable. It can be any mix in an application. In this example, we try to stay with the composable approach. Implementing some primitive actions, we can compose more complex event handlers.

### Effect handlers

The effect handler dispatches on the effect name and actually performs the effect. Since the effect handler is not pure, it is not suitable for unit testing. Therefore we keep them stupid. Any logic should be placed in the action handlers. To encourage this stupidness the effect handler is not passed the application state.

### Only actions in event handlers

The event handlers are attached to elements in [the views](#views). Since we only allow actions in an event handler, the view cannot reference effects directly. Therefore there sometimes is a bit of inderection with an event handler using an action only to “access” an effect. It would be entirely possiblee to allow `effects` directly in the event handler, but this would complicate the code of our tiny framework. We want this code to be as simple as possible.

### Rendering

When all actions of an event are handled, we ask Replicant to render the app, providing the current state. If the state has not changed, Replicant will do no re-rendering.

### Views

The views are pure functions that return Hiccup (Clojure vectors representing the DOM). The functions have no local state (and don't use any globel state either), operating only on the data provided to them. This is deliberately imposed by the Replicant library, which doesn't suport local state.

Replicant calls the top view with the app state, and that view then calls all the other views in a cascade. In development you can evaluate any subview in the REPL, while iterating on it, and examine what data (Hiccup) it returns. Since there is no local or global state, all you need to do is provide enough data to the view. You can also write tests for the views. Hiccup is super easy to inspect.

### Routing

We use [Reitit](https://github.com/metosin/reitit) for routing. The route table is pure data, and the router dipatches `actions` (pure data, remember?) on our event handler.

### Side effects

Counting rendering as a side effect, the app is side-effecting as part of the application start, in the event handler, and from the app state atom watcher.

From a business rules perspective, the only side effects happen in the event handler.

### Testing

Since the side effects are “pushed” out to the edges of the application, we can unit test most anything in the app, including:

* Some of the event handling framework
* Business rules functions
* The action handlers
  * We can't easily unit test the effect handlers, but we can test that they are being called as they should
* The views
  * That they show and hide things as they should depending on the app state
  * That they enact the corret behaviour when interacted with

The last point there is important, because with most frameworks, you need to use a browser to test the behaviour of the UI in interaction with the user. With Replicant, if you let your use of it be inspired by its functional and data oriented approach, testing view interaction can be done like so:

1. Call the view function with some state
2. Collect the actions bound to the dom and element life cycle events of the view
3. Run the action handlers for each event in some order, simulating user interaction
  - Check that the state has been updated as expected
  - Check that it will trigger the correct effects

Combining this with keeping the views stupid, we can run the tests in node-js, without a browser. The tests [cover*](#work-in-progress) a lot of what the browser based TodoMVC end-to-end test suite covers. The tests are watched and run whenever a file changes. They run in a few milliseconds (compared to minutes for the browser based tests). They can conveniently be run in the REPL, Interactive Programming, wether you run whole or parts or the test suite, individual tests, or just pieces of code in some test.

Also:

> Check that the state has been updated as expected

Since the state is immutable data, each test is completely isolated, there is no need for any setup or teardown.

**NB**: The app passes the TodoMVC end-to-end in-browser test suite.

#### Work in progress

The test suite is not yet complete. It's about 30% done. It's being worked on and we should soon be done (you can help, if you like). When the tests are done we hopefully will cover more than 90% of the TodoMVC test suite.

### Read more about the general approach in the mini-app example

To understand a bit more about the code in this example, check the README of the [Replicant mini-app](https://github.com/anteoas/replicant-mini-app) example, which discusses some more aspects of the general approach that is used.

## Building the app

Prerequisites:

- [Node.js](https://nodejs.org) (or something node-ish enough)
- [Java](https://adoptopenjdk.net)
- [Clojure CLI](https://clojure.org/guides/getting_started)

### Release build

```sh
npm run build
```

#### Running the tests

```sh
node out/tests.js
```

This tests business rules and some parts of our tiny framework. Since the software under test is free of side effects, we can test it without a browser.

### Running the app in development mode

We're using [shadow-cljs](https://github.com/thheller/shadow-cljs) to build the app. Clojure editors like [Calva](https://calva.io) and [CIDER](https://cider.mx/) will let you quickly start the app and connect you to its REPL. You can also just run it without installing anything, by using npx:

```sh
npx shadow-cljs watch :app :test
```

Once built, you can access the app at http://localhost:8585

Once built, the tests are run and they will also run on any changes to the tests or the software under test.

To start the app with a Clojure editor friendly nREPL server, run:

```sh
npx shadow-cljs -d cider/cider-nrepl:0.50.2 watch :app :test
```

## License

Public Domain, **Unlicense**. See [LICENSE.md](LICENSE.md).

## Implementation repository

This example is maintained at the [replicant-todomvc](https://github.com/anteoas/replicant-todomvc) repository.

## Happy coding! ♥️

Please file issues if you have any questions or suggestions. Pull requests are also welcome (but please file an issue first if your PR would be beyond things like fixing typos). 🙏 (Towards the [Implementation repository](#implementation-repository))

(ns todomvc.effects
  (:require [clojure.core.match :refer [match]]))

(defn perform-effect! [{:keys [^js replicant/js-event]} effect]
  (match effect
    [:console/fx.debug & args]
    (apply (comp js/console.debug prn) args)

    [:dom/fx.focus-element element]
    (.focus element)

    [:dom/fx.prevent-default]
    (.preventDefault js-event)

    [:dom/fx.set-input-text element text]
    (set! (.-value element) text)))
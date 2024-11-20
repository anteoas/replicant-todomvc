(ns pez.hiccup-attributes
  (:require [lookup.core :as l]))

(defn select-attribute
  [selector path data]
  (let [elements (l/select selector data)]
    (keep (fn [element]
            (when (map? (second element))
              (get-in (second element) path)))
          elements)))

(defn collect-attributes
  [events data]
  (reduce (fn [acc [selector path]]
            (let [attributes (select-attribute selector path data)]
              (into acc attributes)))
          []
          events))

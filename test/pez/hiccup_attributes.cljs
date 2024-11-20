(ns pez.hiccup-attributes
  (:require [lookup.core :as l]))

(defn select-attribute
  [selector path data]
  (let [elements (l/select selector data)]
    (->> (keep (fn [element]
                 (when (map? (second element))
                   (get-in (second element) path)))
               elements))))




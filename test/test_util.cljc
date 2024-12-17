(ns test-util
  (:require [lookup.core :as l]
            pez.baldr))

(defn select-attribute
  [selector path data]
  (let [elements (l/select selector data)]
    (->> (keep (fn [element]
                 (when (map? (second element))
                   (get-in (second element) path)))
               elements))))

(defn- flatten-actionss [actionss]
  (reduce into [] actionss))

(defn select-actions [selector path data]
  (->> (select-attribute selector path data)
       flatten-actionss))
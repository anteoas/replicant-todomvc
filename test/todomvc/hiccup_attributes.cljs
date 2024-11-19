(ns todomvc.hiccup-attributes
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

(comment
  (def haystack [:a
                 [:b.x {:c {:d :e}}
                  [:f]]
                 [:g.x {:id "h"
                        :i :j}
                  [:k {:l [[1 2 3]
                           [4 5 6]]}]]])

  (l/select '[a b] haystack)
  ;;=> ([:b {:class #{"x"}, :c {:d :e}} [:f]])
  (select-attribute '[a b] [:c :d] haystack)
  ;;=> (:e)
  (collect-attributes [['[a b] [:c :d]]] haystack)
  ;;=> [:e]

  (l/select '[a g#h] haystack)
  ;;=> ([:g {:class #{"x"}, :id "h", :i :j} [:k {:l [[1 2 3] [4 5 6]]}]])
  (select-attribute '[a g#h] [] haystack)
  ;;=> ({:class #{"x"}, :id "h", :i :j})
  (collect-attributes [['[a g#h] []]] haystack)
  ;;=> [{:class #{"x"}, :id "h", :i :j}]

  (l/select '[g#h k] haystack)
  ;;=> ([:k {:l [[1 2 3] [4 5 6]]}])
  (select-attribute '[g#h k] [:l] haystack)
  ;;=> ([[1 2 3] [4 5 6]])
  (collect-attributes [['[g#h k] [:l]]] haystack)
  ;;=> [[[1 2 3] [4 5 6]]]
  :rcf)
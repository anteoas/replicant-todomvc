(ns todomvc.clojure-util)

(defn remove-index [i v]
  (vec (concat (subvec v 0 i) (subvec v (inc i)))))
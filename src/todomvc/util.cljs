(ns todomvc.util)

(defn remove-nth
  "Returns a new vector with the element at index `n` removed."
  [n v]
  (vec (concat (subvec v 0 n) (subvec v (inc n)))))

(defn js-get-in
  "Returns the value from the JavaScript `object` following the sequence of strings as a `path`.

   ```clojure
   (js-get-in #js {:a #js {:b 1}} [\"a\" \"b\"]) ;=> 1

   (def o (js/Object. (clj->js {:target {:value \"foo\"}})))
   (js-get-in o [\"target\" \"value\"]) ; => \"foo\"
   ```

   Does not throw an exception if the path does not exist, returns `nil` instead.
   ```clojure
   (js-get-in o [\"target\" \"value\" \"bar\"]) ; => nil
   (js-get-in o [\"TARGET\" \"value\"]) ; => nil
   ```"
  [object path]
  (reduce (fn [acc k]
            (some-> acc (unchecked-get k)))
          object
          path))

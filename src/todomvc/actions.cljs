(ns todomvc.actions
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as string]
            [todomvc.util :as util]))

(defn- get-mark-all-as-state [items]
  (let [as-state (if (every? :item/completed items)
                   false
                   (every? :item/completed (filter :item/completed items)))]
    (prn "Mark all as state" as-state)
    as-state))

(defn- mark-items-as [items completed?]
  (println "Marking items as" completed?)
  (mapv (fn [item]
          (assoc item :item/completed completed?))
        items))

(defn- end-editing [state keyup-code draft index]
  (js/console.debug 'end-editing keyup-code draft index)
  (let [save-edit? (and (not (string/blank? draft))
                        (not= "Escape" keyup-code))
        delete-item? (string/blank? draft)]
    (cond-> state
      save-edit? (assoc-in [:app/todo-items index :item/title] draft)
      delete-item? (update :app/todo-items (partial util/remove-nth index))
      delete-item? (assoc :app/mark-all-state (not (get-mark-all-as-state (:app/todo-items state))))
      :always (dissoc :edit/editing-item-index :edit/keyup-code))))

(defn handle-action! [!state {:keys [^js replicant/js-event]} action]
  (match action
    [:app/mark-all-items-as items completed?]
    (swap! !state assoc :app/todo-items (mark-items-as items completed?))

    [:app/set-mark-all-state]
    (swap! !state assoc :app/mark-all-state (not (get-mark-all-as-state (:app/todo-items @!state))))

    [:console/debug & args]
    (apply (comp js/console.debug prn) args)

    [:db/assoc & args]
    (apply swap! !state assoc args)

    [:db/assoc-in path v]
    (apply swap! !state assoc-in path v)

    [:db/dissoc & args]
    (apply swap! !state dissoc args)

    [:db/update k f & args]
    (apply swap! !state update k f args)

    [:db/update-in path & args]
    (apply swap! !state update-in path args)

    [:dom/focus-element element]
    (.focus element)

    [:dom/prevent-default]
    (.preventDefault js-event)

    [:dom/set-input-text element text]
    (set! (.-value element) text)

    [:edit/end-editing draft index]
    (swap! !state end-editing (:edit/keyup-code @!state) draft index)))
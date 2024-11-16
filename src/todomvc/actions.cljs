(ns todomvc.actions
  (:require [clojure.string :as string]
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
  (let [save-edit? (and (not (string/blank? draft))
                        (not= "Escape" keyup-code))
        delete-item? (string/blank? draft)]
    (cond-> state
      save-edit? (assoc-in [:app/todo-items index :item/title] draft)
      delete-item? (update :app/todo-items (partial util/remove-nth index))
      delete-item? (assoc :app/mark-all-state (not (get-mark-all-as-state (:app/todo-items state))))
      :always (dissoc :edit/editing-item-index :edit/keyup-code))))

(defn handle-action! [!state {:keys [^js replicant/js-event]} action]
  (let [[action-name & args] action]
    (case action-name
      :app/mark-all-items-as (swap! !state assoc :app/todo-items (mark-items-as (first args) (second args)))
      :app/set-mark-all-state (swap! !state assoc :app/mark-all-state (not (get-mark-all-as-state (:app/todo-items @!state))))
      :console/debug (apply (comp js/console.debug prn) args)
      :db/assoc (apply swap! !state assoc args)
      :db/assoc-in (apply swap! !state assoc-in args)
      :db/dissoc (apply swap! !state dissoc args)
      :db/update (apply swap! !state update args)
      :db/update-in (apply swap! !state update-in args)
      :dom/focus-element (.focus (first args))
      :dom/prevent-default (.preventDefault js-event)
      :dom/set-input-text (set! (.-value (first args)) (second args))
      :edit/end-editing (apply swap! !state end-editing (:edit/keyup-code @!state) args))))
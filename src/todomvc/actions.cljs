(ns todomvc.actions
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as string]
            [todomvc.util :as util]))

(defn- get-mark-all-as-state [items]
  (let [as-state (if (and (seq items)
                          (every? :item/completed items))
                   false
                   (every? :item/completed (filter :item/completed items)))]
    as-state))

(defn- mark-items-as [items completed?]
  (mapv (fn [item]
          (assoc item :item/completed completed?))
        items))

(defn- end-editing [state keyup-code draft index]
  (let [trimmed (string/trim draft)
        save-edit? (and (not (string/blank? trimmed))
                        (not= "Escape" keyup-code))
        delete-item? (string/blank? trimmed)]
    (cond-> state
      save-edit? (assoc-in [:app/todo-items index :item/title] trimmed)
      delete-item? (update :app/todo-items (partial util/remove-nth index))
      delete-item? (assoc :app/mark-all-state (not (get-mark-all-as-state (:app/todo-items state))))
      :always (dissoc :edit/editing-item-index :edit/keyup-code))))

(defn handle-action [state _replicant-data action]
  (match action
    [:app/ax.mark-all-items-as completed?]
    {:new-state (update state :app/todo-items mark-items-as completed?)}

    [:app/ax.set-mark-all-state]
    {:new-state (let [items (:app/todo-items state)]
                  (assoc state :app/mark-all-state (not (get-mark-all-as-state items))))}

    [:console/ax.debug & args]
    {:effects [(into [:console/fx.debug] args)]}

    [:db/ax.assoc & args]
    {:new-state (apply assoc state args)}

    [:db/ax.assoc-in path v]
    {:new-state (assoc-in state path v)}

    [:db/ax.dissoc & args]
    {:new-state (apply dissoc state args)}

    [:db/ax.update k f & args]
    {:new-state (apply update state k f args)}

    [:db/ax.update-in path & args]
    {:new-state (apply update-in state path args)}

    [:dom/ax.focus-element element]
    {:effects [[:dom/fx.focus-element element]]}

    [:dom/ax.prevent-default]
    {:effects [[:dom/fx.prevent-default]]}

    [:dom/ax.set-input-text element text]
    {:effects [[:dom/fx.set-input-text element text]]}

    [:edit/ax.end-editing draft index]
    {:new-state (end-editing state (:edit/keyup-code state) draft index)}))

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
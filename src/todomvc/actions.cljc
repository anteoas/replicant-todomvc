(ns todomvc.actions
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [todomvc.util :as util]))

(defn- enrich-action-from-replicant-data [{:replicant/keys [js-event node]} actions]
  (walk/postwalk
   (fn [x]
     (if (keyword? x)
       (cond (= "event" (namespace x)) (let [path (string/split (name x) #"\.")]
                                         (util/js-get-in js-event path))
             (= :dom/node x) node
             :else x)
       x))
   actions))

(defn- enrich-action-from-state [state action]
  (walk/postwalk
   (fn [x]
     (cond
       (and (vector? x)
            (= :db/get (first x))) (get state (second x))
       :else x))
   action))

(defn- get-mark-all-as-state [items]
  (let [as-state (if (and (seq items)
                          (every? :item/completed? items))
                   false
                   (every? :item/completed? (filter :item/completed? items)))]
    as-state))

(defn- mark-items-as [items completed?]
  (mapv (fn [item]
          (assoc item :item/completed? completed?))
        items))

(defn- end-editing [{:keys [app/todo-items] :as state} keyup-code draft index]
  (let [trimmed (string/trim draft)
        save-edit? (and (not (string/blank? trimmed))
                        (not= "Escape" keyup-code))
        delete-item? (string/blank? trimmed)
        new-items (when delete-item?
                    (util/remove-nth index todo-items))]
    (cond-> state
      save-edit? (assoc-in [:app/todo-items index :item/title] trimmed)
      delete-item? (assoc :app/todo-items new-items)
      delete-item? (assoc :app/mark-all-checkbox-checked? (not (get-mark-all-as-state new-items)))
      :always (dissoc :edit/editing-item-index :edit/keyup-code))))

(defn handle-action [state replicant-data action]
  (let [enriched-action (->> action
                             (enrich-action-from-replicant-data replicant-data)
                             (enrich-action-from-state state))]
    (match enriched-action
      [:app/ax.mark-all-items-as completed?]
      {:new-state (update state :app/todo-items mark-items-as completed?)}

      [:app/ax.set-mark-all-state]
      {:new-state (let [items (:app/todo-items state)]
                    (assoc state :app/mark-all-checkbox-checked? (not (get-mark-all-as-state items))))}

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
      {:new-state (end-editing state (:edit/keyup-code state) draft index)})))

(defn handle-actions [state replicant-data actions]
  (reduce (fn [{state :new-state :as acc} action]
            (let [{:keys [new-state effects]} (handle-action state replicant-data action)]
              (when #?(:cljs (and js/goog.DEBUG
                                  (exists? js/window))
                       :clj true)
                #?(:cljs (js/console.debug "Triggered action" action)
                   :clj (println "Triggered action" action)))
              (cond-> acc
                new-state (assoc :new-state new-state)
                effects (update :effects into effects))))
          {:new-state state
           :effects []}
          actions))

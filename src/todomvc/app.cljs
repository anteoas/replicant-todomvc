(ns todomvc.app
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [cognitect.transit :as t]
            [gadget.inspector :as inspector]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [replicant.dom :as r-dom]
            [todomvc.util :as cu]
            [todomvc.views :as views]))

(def default-db {:app/todo-items []
                 :app/el nil})

(defonce ^:private !state (atom nil))

(def ^:private routes [["/" {:name :route/home}]
                       ["/active" {:name :route/active}]
                       ["/completed" {:name :route/completed}]])

(defn get-route-actions [{:keys [data]}]
  (case (:name data)
    :route/home [[:db/assoc :app/item-filter :filter/all]]
    :route/active [[:db/assoc :app/item-filter :filter/active]]
    :route/completed [[:db/assoc :app/item-filter :filter/completed]]))

(defn- start-router! [dispatch!]
  (rfe/start! (rf/router routes)
              (fn [m]
                (dispatch! nil (get-route-actions m)))
              {:use-fragment true}))

(def storage-key "replicant-todomvc")

(def persist-keys [:app/todo-items
                   :add/draft
                   :app/mark-all-state])

(defn- load-persisted! []
  (or (->> (.getItem js/localStorage storage-key)
           (t/read (t/reader :json)))
      default-db))

(defn- persist! [state]
  (.setItem js/localStorage storage-key (t/write (t/writer :json) (select-keys state persist-keys))))

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
      delete-item? (update :app/todo-items (partial cu/remove-nth index))
      delete-item? (assoc :app/mark-all-state (not (get-mark-all-as-state (:app/todo-items state))))
      :always (dissoc :edit/editing-item-index :edit/keyup-code))))

(defn- enrich-action-from-event [{:replicant/keys [js-event node]} actions]
  (walk/postwalk
   (fn [x]
     (if (keyword? x)
       (cond (= "event" (namespace x)) (let [path (string/split (name x) #"\.")]
                                         (cu/js-get-in js-event path))
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

(defonce el (js/document.getElementById "app"))

(defn- render! [state]
  (r-dom/render
   el
   (views/app-view state)))

(defn- event-handler [{:replicant/keys [^js js-event] :as replicant-data} actions]
  (doseq [action actions]
    (prn "Triggered action" action)
    (let [enriched-action (->> action
                               (enrich-action-from-event replicant-data)
                               (enrich-action-from-state @!state))
          [action-name & args] enriched-action]
      (prn "Enriched action" enriched-action)
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
        :edit/end-editing (apply swap! !state end-editing (:edit/keyup-code @!state) args))
      (persist! @!state)))
  (render! @!state))

(defn ^{:dev/after-load true :export true} start! []
  (render! @!state))

(defn ^:export init! []
  (reset! !state (load-persisted!))
  (inspector/inspect "App state" !state)
  (r-dom/set-dispatch! event-handler)
  (start-router! event-handler)
  (start!))

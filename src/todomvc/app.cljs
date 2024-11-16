(ns todomvc.app
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [cognitect.transit :as t]
            [gadget.inspector :as inspector]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [replicant.dom :as r-dom]
            [todomvc.actions :as actions]
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

(defn- event-handler [replicant-data actions]
  (doseq [action actions]
    (prn "Triggered action" action)
    (let [enriched-action (->> action
                               (enrich-action-from-event replicant-data)
                               (enrich-action-from-state @!state))]
      (prn "Enriched action" enriched-action)
      (actions/handle-action! !state replicant-data enriched-action))))

(defn ^{:dev/after-load true :export true} start! []
  (render! @!state))

(defn ^:export init! []
  (reset! !state (load-persisted!))
  (inspector/inspect "App state" !state)
  (r-dom/set-dispatch! event-handler)
  (start-router! event-handler)
  (add-watch !state :persist (fn [_ _ old-state new-state]
                               (when (not= old-state new-state)
                                 (render! new-state)
                                 (when (not= (select-keys old-state persist-keys)
                                             (select-keys new-state persist-keys))
                                   (persist! new-state)))))
  (start!))

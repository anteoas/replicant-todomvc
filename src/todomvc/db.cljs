(ns todomvc.db
  (:require [cognitect.transit :as t]))

(defonce !state (atom nil))

(def ^:private default-db {:app/todo-items []
                           :app/el nil})

(def ^:private storage-key "todos-replicant")

(def ^:private persist-keys [:app/todo-items
                             :add/draft
                             :app/mark-all-state])

(defn- load-persisted! []
  (or (->> (.getItem js/localStorage storage-key)
           (t/read (t/reader :json)))
      default-db))

(defn- persist! [state]
  (.setItem js/localStorage storage-key (t/write (t/writer :json) (select-keys state persist-keys))))

(defn init! []
  (reset! !state (load-persisted!)))

(defn watch! [render!]
  (add-watch !state :persist (fn [_ _ old-state new-state]
                               (when (not= old-state new-state)
                                 (render! new-state)
                                 (when (not= (select-keys old-state persist-keys)
                                             (select-keys new-state persist-keys))
                                   (persist! new-state))))))
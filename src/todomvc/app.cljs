(ns todomvc.app
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [gadget.inspector :as inspector]
            [replicant.dom :as r-dom]
            [todomvc.actions :as actions]
            [todomvc.db :as db]
            [todomvc.router :as router]
            [todomvc.util :as util]
            [todomvc.views :as views]))

(defonce el (js/document.getElementById "app"))

(defn- render! [state]
  (r-dom/render
   el
   (views/app-view state)))

(defn- enrich-action-from-event [{:replicant/keys [js-event node]} actions]
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

(defn- event-handler [replicant-data actions]
  (doseq [action actions]
    (when js/goog.DEBUG (prn "Triggered action" action))
    (let [enriched-action (->> action
                               (enrich-action-from-event replicant-data)
                               (enrich-action-from-state @db/!state))]
      (when js/goog.DEBUG (prn "Enriched action" enriched-action))
      (let [{:keys [new-state effects]} (actions/handle-action @db/!state replicant-data enriched-action)]
        (when new-state
          (reset! db/!state new-state))
        (when effects
          (doseq [effect effects]
            (actions/perform-effect! replicant-data effect)))))))

(defn ^{:dev/after-load true :export true} start! []
  (render! @db/!state))

(defn ^:export init! []
  (db/init!)
  (inspector/inspect "App state" db/!state)
  (r-dom/set-dispatch! event-handler)
  (router/start! router/routes event-handler)
  (db/watch! render!)
  (start!))

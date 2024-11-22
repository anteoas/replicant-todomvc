(ns todomvc.app
  (:require [gadget.inspector :as inspector]
            [replicant.dom :as r-dom]
            [todomvc.actions :as actions]
            [todomvc.db :as db]
            [todomvc.router :as router]
            [todomvc.views :as views]))

(defn- render! [state]
  (r-dom/render
   (js/document.getElementById "app")
   (views/app-view state)))

(defn- event-handler [replicant-data actions]
  (doseq [action actions]
    (when js/goog.DEBUG (prn "Triggered action" action))
    (let [{:keys [new-state effects]} (actions/handle-action @db/!state replicant-data action)]
      (when new-state
        (reset! db/!state new-state))
      (when effects
        (doseq [effect effects]
          (actions/perform-effect! replicant-data effect))))))

(defn ^{:dev/after-load true :export true} start! []
  (render! @db/!state))

(defn ^:export init! []
  (db/init!)
  (inspector/inspect "App state" db/!state)
  (r-dom/set-dispatch! event-handler)
  (router/start! router/routes event-handler)
  (db/watch! render!)
  (start!))

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
  (let [{:keys [new-state effects]} (actions/handle-actions @db/!state replicant-data actions)]
    (when new-state
      (reset! db/!state new-state))
    (when effects
      (doseq [effect effects]
        (when js/goog.DEBUG (js/console.debug "Triggered effect" effect))
        (actions/perform-effect! replicant-data effect)))))

(defn ^{:dev/after-load true :export true} start! []
  (render! @db/!state))

(defn ^:export init! []
  (db/init!)
  (inspector/inspect "App state" db/!state)
  (r-dom/set-dispatch! event-handler)
  (router/start! router/routes event-handler)
  (db/watch! render!)
  (start!))

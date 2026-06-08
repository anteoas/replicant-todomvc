(ns todomvc.app
  (:require [dataspex.core :as dataspex]
            [replicant.dom :as r-dom]
            [todomvc.actions :as actions]
            [todomvc.effects :as effects]
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
        (effects/perform-effect! replicant-data effect)))))

(defn ^{:dev/after-load true :export true} start! []
  (render! @db/!state))

(defn ^:export init! []
  (db/init!)
  (dataspex/inspect "App state" db/!state {:track-changes? true
                                            :history-limit 2500})
  (r-dom/set-dispatch! event-handler)
  (router/start! router/routes event-handler)
  (db/watch! render!)
  (start!))

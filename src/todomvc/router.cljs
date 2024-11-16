(ns todomvc.router
  (:require [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]))

(def routes [["/" {:name :route/home}]
             ["/active" {:name :route/active}]
             ["/completed" {:name :route/completed}]])

(defn- get-route-actions [{:keys [data]}]
  (case (:name data)
    :route/home [[:db/assoc :app/item-filter :filter/all]]
    :route/active [[:db/assoc :app/item-filter :filter/active]]
    :route/completed [[:db/assoc :app/item-filter :filter/completed]]))

(defn start! [routes dispatch!]
  (rfe/start! (rf/router routes)
              (fn [m]
                (dispatch! nil (get-route-actions m)))
              {:use-fragment true}))
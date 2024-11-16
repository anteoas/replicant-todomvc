(ns todomvc.views
  (:require [clojure.string :as string]
            [todomvc.clojure-util :as cu]))

(defn- maybe-add [coll s]
  (let [trimmed (string/trim s)]
    (if (string/blank? trimmed)
      coll
      (conj coll {:item/title trimmed
                  :item/completed false
                  :item/id (random-uuid)}))))

(defn- add-view [{:keys [add/draft]}]
  [:form {:on {:submit [[:dom/prevent-default]
                        [:db/update :app/todo-items maybe-add draft]
                        [:db/assoc :add/draft ""]
                        [:dom/set-input-text [:db/get :add/draft-input-element] ""]]}}
   [:input.new-todo {:replicant/on-mount [[:db/assoc :add/draft-input-element :dom/node]]
                     :type :text
                     :autofocus true
                     :placeholder "What needs to be done?"
                     :on {:input [[:db/assoc :add/draft :event/target.value]]}}]])

(defn- edit-view [{:keys [index item edit/editing-item-index edit/draft edit/keyup-code]}]
  (when (and (= index editing-item-index)
             (not= "Escape" keyup-code))
    [:form {:replicant/key (:item/id item)
            :replicant/on-unmount [[:edit/end-editing (string/trim draft) index]]
            :on {:submit (into [[:dom/prevent-default]
                                [:db/dissoc :edit/editing-item-index]])}}
     [:input.edit {:replicant/on-mount [[:dom/focus-element :dom/node]]
                   :value (:item/title item)
                   :on {:blur [[:db/dissoc :edit/editing-item-index]]
                        :keyup [[:db/assoc :edit/keyup-code :event/code]]
                        :input [[:db/assoc :edit/draft :event/target.value]]}}]]))

(defn- todo-list-view [{:keys [app/todo-items edit/editing-item-index app/item-filter]
                        :as state}]
  [:ul.todo-list
   (map-indexed (fn [index item]
                  [:li (cond-> {:replicant/key (:item/id item)
                                :class (when (= index editing-item-index) ["editing"])
                                :on {:dblclick [[:db/assoc :edit/editing-item-index index]
                                                [:db/assoc :edit/draft (:item/title item)]]}}
                         (and (= :filter/active item-filter)
                              (:item/completed item)) (assoc :style {:display "none"})
                         (and (= :filter/completed item-filter)
                              (not (:item/completed item))) (assoc :style {:display "none"}))
                   [:div.view
                    [:input.toggle {:type :checkbox
                                    :checked (:item/completed item)
                                    :on {:change [[:db/update-in [:app/todo-items index :item/completed] not]
                                                  [:app/set-mark-all-state]]}}]
                    [:label (:item/title item)]
                    [:button.destroy {:on {:click [[:db/update :app/todo-items (partial cu/remove-index index)]
                                                   [:app/set-mark-all-state]]}}]]
                   (edit-view (merge state {:index index
                                            :item item}))])
                todo-items)])

(defn- main-view [{:keys [app/todo-items] :as state}]
  [:div.main
   [:input#toggle-all.toggle-all {:type :checkbox
                                  :checked (:app/mark-all-state state)
                                  :on {:change [[:db/assoc :app/mark-all-state :event/target.checked]
                                                [:app/mark-all-items-as todo-items :event/target.checked]]}}]
   [:label {:for "toggle-all"}
    "Mark all as complete"]
   (todo-list-view state)])

(defn- items-footer-view [{:keys [app/todo-items app/item-filter]}]
  (let [active-count (count (remove :item/completed todo-items))]
    [:footer.footer
     [:span.todo-count
      [:strong active-count]
      (str " "
           (condp = active-count 1 "item" "items")
           " left")]
     [:ul.filters
      [:li [:a {:class (when (= :filter/all item-filter) "selected")
                :href "#/"} "All"]]
      [:li [:a {:class (when (= :filter/active item-filter) "selected")
                :href "#/active"} "Active"]]
      [:li [:a {:class (when (= :filter/completed item-filter) "selected")
                :href "#/completed"} "Completed"]]]
     (when (seq (filter :item/completed todo-items))
       [:button.clear-completed {:on {:click [[:db/update :app/todo-items (partial filterv (complement :item/completed))]]}}
        "Clear completed"])]))

(defn- app-footer-view []
  [:footer.info
   [:p "Double-click to edit a todo"]
   [:p "Created by "
    [:a {:href "https://github.com/anteoas"} "Anteo AS developers"]]
   [:p "Part of "
    [:a {:href "https://todomvc.com"} "TodoMVC"]]])

(defn app-view [{:keys [app/todo-items] :as state}]
  [:div
   [:section.todoapp
    [:header.header
     [:h1 "todos"]
     (add-view state)]
    (when (seq todo-items)
      (list
       (main-view state)
       (items-footer-view state)))]
   (app-footer-view)])
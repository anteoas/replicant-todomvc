(ns todomvc.item-footer-view-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [todomvc.actions :as a]
   [test-util :as tu]
   [todomvc.views :as sut]))

(deftest clear-completed-items
  (testing "Clearing completed items"
    (let [state {:app/todo-items [{:item/title "Completed Todo" :item/completed? true}
                                  {:item/title "Active Todo" :item/completed? false}]}
          view (#'sut/items-footer-view state)
          on-click-actions (tu/select-actions :button.clear-completed [:on :click] view)
          {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
      (is (= 1 (count (:app/todo-items new-state)))
          "it clears the completed todo items")
      (is (= "Active Todo" (:item/title (first (:app/todo-items new-state))))
          "it retains the active todo items")
      (is (empty? effects)
          "it has no side effects apart from the state change"))))

(deftest items-footer-view
  (testing "Items footer"
    (is (seq (l/select :.footer (#'sut/items-footer-view {})))
        "it renders a .footer element")

    (testing "Clear completed"
      (is (empty? (l/select :button.clear-completed
                            (#'sut/items-footer-view
                             {:app/todo-items
                              []})))
          "it does not render a button.clear-completed element when there are no items")
      (is (empty? (l/select :button.clear-completed
                            (#'sut/items-footer-view
                             {:app/todo-items
                              [{:item/title "Test Item" :item/completed? false}]})))
          "it does not render a button.clear-completed element when all items are active")
      (is (seq (l/select :button.clear-completed
                         (#'sut/items-footer-view
                          {:app/todo-items
                           [{:item/title "Test Item" :item/completed? true}]})))
          "it renders a button.clear-completed element when there are completed items")))

  (testing "Active count"
    (is (seq (l/select :.todo-count (#'sut/items-footer-view {})))
        "it renders a .todo-count element")
    (is (= "0 items left"
           (->> (l/select [:.todo-count]
                          (#'sut/items-footer-view
                           {:app/todo-items
                            [{:item/title "Test Item" :item/completed? true}]}))
                (l/get-text)))
        "it displays the number of active items pluralized when == 0")
    (is (= "1 item left"
           (->> (l/select [:.todo-count]
                          (#'sut/items-footer-view
                           {:app/todo-items
                            [{:item/title "Test Item" :item/completed? false}]}))
                (l/get-text)))
        "it displays the number of active items non-pluralized when == 1")
    (is (= "2 items left"
           (->> (l/select [:.todo-count]
                          (#'sut/items-footer-view
                           {:app/todo-items
                            [{:item/title "Test Item" :item/completed? false}
                             {:item/title "Test Item" :item/completed? true}
                             {:item/title "Test Item" :item/completed? false}]}))
                (l/get-text)))
        "it displays the number of active items pluralized when > 1"))

  (testing "Filter links"
    (let [filter->href (fn [item-filter]
                         (->> (l/select :a (#'sut/items-footer-view {:app/item-filter item-filter}))
                              (filter (fn [element]
                                        (some #{"selected"} (first (tu/select-attribute :a [:class] element)))))
                              first
                              (tu/select-attribute :a [:href])
                              first))]
      (is (= "#/"
             (filter->href :filter/all))
          "it marks the 'All' filter as selected when the filter is 'all'")
      (is (= "#/active"
             (filter->href :filter/active))
          "it marks the 'Active' filter as selected when the filter is 'active'")
      (is (= "#/completed"
             (filter->href :filter/completed))
          "it marks the 'Completed' filter as selected when the filter is 'completed'"))))

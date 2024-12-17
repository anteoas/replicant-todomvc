(ns todomvc.item-view-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [todomvc.actions :as a]
   [test-util :as tu]
   [todomvc.views :as sut]))

(deftest item-view
  (testing "item rendering"
    (let [item {:item/title "Test Item"}
          view (#'sut/item-view {:app/item-filter :filter/all} 0 item)]
      (is (seq (l/select [:li :.view] view))
          "it renders the item in a list item containing a .view element")
      (is (= [[:label (:item/title item)]]
             (l/select :label view))
          "it displays the item's title"))

    (testing "filtering"
      (is (not= nil?
                (#'sut/item-view {:app/item-filter :filter/all} 0 {:item/title "Test Item"
                                                                   :item/completed? true}))
          "it renders a completed item when filtering on all")
      (is (not= nil?
                (#'sut/item-view {:app/item-filter :filter/all} 0 {:item/title "Test Item"
                                                                   :item/completed? false}))
          "it renders an uncompleted item when filtering on all")
      (is (nil?
           (#'sut/item-view {:app/item-filter :filter/completed} 0 {:item/title "Test Item"
                                                                    :item/completed? false}))
          "it does not render an uncompleted item when filtering on completed")
      (is (not= nil?
                (#'sut/item-view {:app/item-filter :filter/completed} 0 {:item/title "Test Item"
                                                                         :item/completed? true}))
          "it renders a completed items when filtering on completed")
      (is (nil?
           (#'sut/item-view {:app/item-filter :filter/active} 0 {:item/title "Test Item"
                                                                 :item/completed? true}))
          "it does not render a completed item when filtering on active")
      (is (not= nil?
                (#'sut/item-view {:app/item-filter :filter/active} 0 {:item/title "Test Item"
                                                                      :item/completed? false}))
          "it renders an uncompleted item when filtering on active"))

    (testing "editing"
      (is (some #{"editing"}
                (->> (#'sut/item-view {:edit/editing-item-index 0
                                       :app/item-filter :filter/all}
                                      0
                                      {})
                     (tu/select-attribute :li [:class])
                     first))
          "The item should have the 'editing' class when it is being edited")
      (is (not-any? #{"editing"}
                    (->> (#'sut/item-view {:edit/editing-item-index 0
                                           :app/item-filter :filter/all}
                                          1
                                          {})
                         (tu/select-attribute :li [:class])
                         first))
          "The item should not have the 'editing' class when another item is being edited")
      (is (not-any? #{"editing"}
                    (->> (#'sut/item-view {:app/item-filter :filter/all}
                                          1
                                          {})
                         (tu/select-attribute :li [:class])
                         first))
          "The item should not have the 'editing' class when no item is being edited"))

    (testing "completed"
      (is (some #{"completed"}
                (->> (#'sut/item-view {:app/item-filter :filter/all}
                                      0 {:item/title "Test Item"
                                         :item/completed? true})
                     (tu/select-attribute :li [:class])
                     first))
          "The item should have the 'completed' class when it is completed")
      (is (not-any? #{"completed"}
                    (->> (#'sut/item-view {:app/item-filter :filter/all}
                                          0 {:item/title "Test Item"
                                             :item/completed? false})
                         (tu/select-attribute :li [:class])
                         first))
          "The item should not have the 'completed' class when it is uncompleted")))

  (testing "enabling editing mode"
    (let [index 0
          state {:app/item-filter :filter/all}
          item {:item/title "Test Item"
                :item/completed? false}
          view (#'sut/item-view state index item)
          on-dblclick-actions (tu/select-actions :li [:on :dblclick] view)
          {:keys [new-state]} (a/handle-actions state {} on-dblclick-actions)]
      (is (= index
             (:edit/editing-item-index new-state))
          "it enables editing mode for the item")
      (is (= (:item/title item)
             (:edit/draft new-state))
          "it sets the draft to the item title")))

  (testing "the toggle checkbox"
    (testing "uncompleted item"
      (let [state {:edit/editing-item-index nil
                   :app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item" :item/completed? false}]
                   :app/mark-all-checkbox-checked? false}
            item (first (:app/todo-items state))
            view (#'sut/item-view state 0 item)]
        (is (false? (-> (tu/select-attribute :input.toggle [:checked] view) first))
            "it is not checked initially")
        (let [on-change-actions (tu/select-actions :input.toggle [:on :change] view)
              {:keys [new-state]} (a/handle-actions state {} on-change-actions)]
          (is (true? (-> new-state :app/todo-items first :item/completed?))
              "it marks the item as completed")
          (is (true? (:app/mark-all-checkbox-checked? new-state))
              "it updates the mark-all state to true"))))

    (testing "completed item"
      (let [state {:edit/editing-item-index nil
                   :app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item" :item/completed? true}]
                   :app/mark-all-checkbox-checked? true}
            item (first (:app/todo-items state))
            view (#'sut/item-view state 0 item)]
        (is (true? (-> (tu/select-attribute :input.toggle [:checked] view) first))
            "it is checked initially")
        (let [on-change-actions (tu/select-actions :input.toggle [:on :change] view)
              {:keys [new-state]} (a/handle-actions state {} on-change-actions)]
          (is (false? (-> new-state :app/todo-items first :item/completed?))
              "it marks the item as not completed")
          (is (false? (:app/mark-all-checkbox-checked? new-state))
              "it updates the mark-all state to false")))))

  (testing "delete button"
    (is (seq (l/select :button.destroy (#'sut/item-view {:app/item-filter :filter/all} 0 {})))
        "it renders a delete button")

    (testing "Last item"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item"
                                     :item/completed? true}]
                   :app/mark-all-checkbox-checked? true}
            item (first (:app/todo-items state))
            view (#'sut/item-view state 0 item)
            on-click-actions (tu/select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (empty? (:app/todo-items new-state))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (false? (:app/mark-all-checkbox-checked? new-state))
            "it updates the mark-all state to false")))

    (testing "Last completed item"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item 1" :item/completed? true}
                                    {:item/title "Test Item 2" :item/completed? false}
                                    {:item/title "Test Item 3" :item/completed? true}]
                   :app/mark-all-checkbox-checked? false}
            item (second (:app/todo-items state))
            view (#'sut/item-view state 1 item)
            on-click-actions (tu/select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (= 2 (count (:app/todo-items new-state)))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (true? (:app/mark-all-checkbox-checked? new-state))
            "it updates mark-all state to true when all remaining items are completed")))

    (testing "Last uncompleted item"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item 1" :item/completed? false}
                                    {:item:title "Test Item 2" :item/completed? true}
                                    {:item:title "Test Item 3" :item/completed? false}]
                   :app/mark-all-checkbox-checked? false}
            item (second (:app/todo-items state))
            view (#'sut/item-view state 1 item)
            on-click-actions (tu/select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (= 2 (count (:app/todo-items new-state)))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (false? (:app/mark-all-checkbox-checked? new-state))
            "it keeps the mark-all state false when all remaining items are uncompleted")))

    (testing "Uncompleted items remaining"
      (let [state {:app/item-filter :filter/all
                   :app/todo-items [{:item/title "Test Item 1" :item/completed? true}
                                    {:item:title "Test Item 2" :item/completed? false}
                                    {:item:title "Test Item 3" :item/completed? true}]
                   :app/mark-all-checkbox-checked? false}
            item (second (:app/todo-items state))
            view (#'sut/item-view state 1 item)
            on-click-actions (tu/select-actions :button.destroy [:on :click] view)
            {:keys [new-state effects]} (a/handle-actions state {} on-click-actions)]
        (is (= 2 (count (:app/todo-items new-state)))
            "it removes the item from the todo list")
        (is (empty? effects)
            "it does so without other side-effects")
        (is (true? (:app/mark-all-checkbox-checked? new-state))
            "it updates the mark-all state false when some remaining items are completed and some are uncompleted")))))
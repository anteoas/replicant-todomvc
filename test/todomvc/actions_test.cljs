(ns todomvc.actions-test
  (:require [todomvc.actions :as sut]
            [clojure.test :refer [deftest is testing]]))

#_{:clj-kondo/ignore [:private-call]}
(deftest get-mark-all-as-state
  (testing "Mark-all state"
    (is (false? (sut/get-mark-all-as-state [{:item/completed? true}
                                            {:item/completed? true}
                                            {:item/completed? true}]))
        "it is false when all items are completed")

    (is (true? (sut/get-mark-all-as-state [{:item/completed? false}
                                           {:item/completed? false}
                                           {:item/completed? false}]))
        "it is true when no items are completed")

    (is (true? (sut/get-mark-all-as-state [{:item/completed? true}
                                           {:item/completed? false}
                                           {:item/completed? true}]))
        "it is true when some items are completed")

    (is (true? (sut/get-mark-all-as-state []))
        "it is true when items list is empty")))

(deftest handle-action-set-mark-all-state
  (testing "Mark-all checkbox"
    (is (true?
         (-> (sut/handle-action {:app/todo-items [{:item/completed? true}
                                                  {:item/completed? true}]}
                                {}
                                [:app/ax.set-mark-all-state])
             :new-state
             :app/mark-all-checkbox-checked?))
        "it sets the mark-all-checkbox to checked when all items are completed")

    (is (false?
         (-> (sut/handle-action {:app/todo-items [{:item/completed? true}
                                                  {:item/completed? false}]}
                                {}
                                [:app/ax.set-mark-all-state])
             :new-state
             :app/mark-all-checkbox-checked?))
        "it sets the mark-all-checkbox to unchecked when some items are not completed")

    (is (false?
         (-> (sut/handle-action {:app/todo-items [{:item/completed? false}
                                                  {:item/completed? false}]}
                                {}
                                [:app/ax.set-mark-all-state])
             :new-state
             :app/mark-all-checkbox-checked?))
        "it sets the mark-all-checkbox to unchecked when no items are completed")

    (is (false?
         (-> (sut/handle-action {:app/todo-items []}
                                {}
                                [:app/ax.set-mark-all-state])
             :new-state
             :app/mark-all-checkbox-checked?))
        "it sets the mark-all-checkbox to unchecked when there are no items")))

(deftest handle-action-mark-all-items-as
  (testing "Mark-all items"
    (is (= [{:item/completed? true}
            {:item/completed? true}]
           (-> (sut/handle-action {:app/todo-items [{:item/completed? false}
                                                    {:item/completed? false}]}
                                  {}
                                  [:app/ax.mark-all-items-as true])
               :new-state
               :app/todo-items))
        "it should mark all items as completed")

    (is (= [{:item/completed? false}
            {:item/completed? false}]
           (-> (sut/handle-action {:app/todo-items [{:item/completed? true}
                                                    {:item/completed? true}]}
                                  {}
                                  [:app/ax.mark-all-items-as false])
               :new-state
               :app/todo-items))
        "it should mark all items as uncompleted")))

#_{:clj-kondo/ignore [:private-call]}
(deftest handle-action-assoc
  (testing "Assoc key-value pairs"
    (is (= {:new-state {:foo :bar
                        :baz :gaz
                        :something :something}}
           (sut/handle-action {:something :something} {} [:db/ax.assoc :foo :bar :baz :gaz]))
        "it should add the given key-value pair(s) to the state")
    (is (= {:new-state {:foo :bar
                        :something :something}}
           (sut/handle-action {:foo :existing-value
                               :something :something} {} [:db/ax.assoc :foo :bar]))
        "it should replace any value at an existing key")))

(deftest handle-action-prevent-default
  (testing "Prevent default effect"
    (is (= {:effects [[:dom/fx.prevent-default]]}
           (sut/handle-action {} {} [:dom/ax.prevent-default]))
        "it should include a prevent-default effect")))

(deftest handle-action-assoc-in
  (testing "Assoc-in value"
    (is (= {:new-state {:nested {:foo :bar}}}
           (sut/handle-action {} {} [:db/ax.assoc-in [:nested :foo] :bar]))
        "it should assoc-in the given value at the specified path")))

(deftest handle-action-dissoc
  (testing "Dissoc key(s)"
    (is (= {:new-state {:baz :gaz}}
           (sut/handle-action {:foo :bar :baz :gaz} {} [:db/ax.dissoc :foo]))
        "it should dissoc the given key(s) from the state")))

(deftest handle-action-update
  (testing "Update value"
    (is (= {:new-state {:counter 2}}
           (sut/handle-action {:counter 1} {} [:db/ax.update :counter inc]))
        "it should update the value at the given key using the provided function")
    (is (= {:new-state {:counter 5}}
           (sut/handle-action {:counter 1} {} [:db/ax.update :counter + 1 1 1 1]))
        "it should update the value at the given key using the provided function and args")))

(deftest handle-action-update-in
  (testing "Update-in value"
    (is (= {:new-state {:nested {:counter 2}}}
           (sut/handle-action {:nested {:counter 1}} {} [:db/ax.update-in [:nested :counter] inc]))
        "it should update-in the value at the given path using the provided function")))

(deftest handle-action-focus-element
  (testing "Focus element effect"
    (is (= {:effects [[:dom/fx.focus-element :element]]}
           (sut/handle-action {} {} [:dom/ax.focus-element :element]))
        "it should return a focus-element effect")))

(deftest handle-action-set-input-text
  (testing "Set input text effect"
    (is (= {:effects [[:dom/fx.set-input-text :element "new text"]]}
           (sut/handle-action {} {} [:dom/ax.set-input-text :element "new text"]))
        "it should return a set-input-text effect")))

(deftest handle-action-end-editing
  (testing "End editing"
    (is (= {:new-state {:app/todo-items [{:item/title "new title"}]}}
           (sut/handle-action {:app/todo-items [{:item/title "old title"}]
                               :edit/keyup-code "Some key X"} {} [:edit/ax.end-editing "new title" 0]))
        "should update the title and remove the current keycode if the keycode is not 'Escape'")

    (is (= {:new-state {:app/todo-items [{:item/title "old title"}]}}
           (sut/handle-action {:app/todo-items [{:item/title "old title"}]
                               :edit/keyup-code "Escape"} {} [:edit/ax.end-editing "new title" 0]))
        "should not update the title and remove the current keycode if the keycode is 'Escape'")

    (is (= {:new-state {:app/todo-items [{:item/title "new title"}]}}
           (sut/handle-action {:app/todo-items [{:item/title "old title"}]} {} [:edit/ax.end-editing "  new title  " 0]))
        "should trim the title")

    (testing "Removal"
      (is (= {:new-state {:app/todo-items []}}
             (-> (sut/handle-action {:app/todo-items [{:item/title "delete me"}]} {} [:edit/ax.end-editing "" 0])
                 (update :new-state select-keys [:app/todo-items])))
          "should remove the item if the draft is empty")

      (is (= {:new-state {:app/todo-items []}}
             (-> (sut/handle-action {:app/todo-items [{:item/title "delete me"}]} {} [:edit/ax.end-editing " " 0])
                 (update :new-state select-keys [:app/todo-items])))
          "should remove the item if the trimmed draft is empty"))

    (testing "Mark-all checkbox state"
      (is (= {:new-state {:app/todo-items []
                          :app/mark-all-checkbox-checked? false}}
             (sut/handle-action {:app/todo-items [{:item/title "delete me"
                                                   :item/completed? true}]
                                 :app/mark-all-checkbox-checked? true} {} [:edit/ax.end-editing "" 0]))
          "it is unchecked if the item is removed, leaving an empty items list")

      (is (= {:new-state {:app/todo-items [{:item/completed? false}]
                          :app/mark-all-checkbox-checked? false}}
             (sut/handle-action {:app/todo-items [{:item/title "delete me"
                                                   :item/completed? true}
                                                  {:item/completed? false}]
                                 :app/mark-all-checkbox-checked? true} {} [:edit/ax.end-editing "" 0]))
          "it is unchecked if the item is removed, leaving a non-empty items list with some items not completed")

      (is (= {:new-state {:app/todo-items [{:item/completed? true}]
                          :app/mark-all-checkbox-checked? true}}
             (sut/handle-action {:app/todo-items [{:item/title "delete me"
                                                   :item/completed? true}
                                                  {:item/completed? true}]
                                 :app/mark-all-checkbox-checked? false} {} [:edit/ax.end-editing "" 0]))
          "it is checked if the item is removed, leaving a non-empty items list with all items completed"))))
(ns todomvc.actions-test
  (:require [todomvc.actions :as sut]
            [clojure.test :refer [deftest is testing]]))

#_{:clj-kondo/ignore [:private-call]}
(deftest test-get-mark-all-as-state
  (testing "false when all items are completed"
    (is (false? (sut/get-mark-all-as-state [{:item/completed true}
                                            {:item/completed true}]))))
  (testing "true when no items are completed"
    (is (true? (sut/get-mark-all-as-state [{:item/completed false}
                                           {:item/completed false}]))))
  (testing "true when some items are completed"
    (is (true? (sut/get-mark-all-as-state [{:item/completed true}
                                           {:item/completed false}]))))
  (testing "true when items list is empty"
    (is (true? (sut/get-mark-all-as-state [])))))

(deftest handle-action-set-mark-all-state
  (testing "it should set mark-all-state to true when all items are completed"
    (let [state {:app/todo-items [{:item/completed true}
                                  {:item/completed true}]}]
      (is (true?
           (-> (sut/handle-action state {} [:app/ax.set-mark-all-state])
               :new-state
               :app/mark-all-state)))))
  (testing "it should set mark-all-state to false when some items are not completed"
    (let [state {:app/todo-items [{:item/completed true}
                                  {:item/completed false}]}]
      (is (false?
           (-> (sut/handle-action state {} [:app/ax.set-mark-all-state])
               :new-state
               :app/mark-all-state))))
    (testing "it should set mark-all-state to false when all items are not completed"
      (let [state {:app/todo-items [{:item/completed false}
                                    {:item/completed false}]}]
        (is (false?
             (-> (sut/handle-action state {} [:app/ax.set-mark-all-state])
                 :new-state
                 :app/mark-all-state))))))
  (testing "it should set mark-all-state to false when items list is empty"
    (let [state {:app/todo-items []}]
      (is (false?
           (-> (sut/handle-action state {} [:app/ax.set-mark-all-state])
               :new-state
               :app/mark-all-state))))))


(deftest handle-action-mark-all-items-as
  (testing "it should mark all items as completed"
    (let [state {:app/todo-items [{:item/completed false}
                                  {:item/completed false}]}]
      (is (= [{:item/completed true}
              {:item/completed true}]
             (-> (sut/handle-action state {} [:app/ax.mark-all-items-as true])
                 :new-state
                 :app/todo-items)))))
  (testing "it should mark all items as uncompleted"
    (let [state {:app/todo-items [{:item/completed true}
                                  {:item/completed true}]}]
      (is (= [{:item/completed false}
              {:item/completed false}]
             (-> (sut/handle-action state {} [:app/ax.mark-all-items-as false])
                 :new-state
                 :app/todo-items))))))

#_{:clj-kondo/ignore [:private-call]}
(deftest handle-action-assoc
  (testing "it should assoc the given key-value pair(s) to the state"
    (is (= {:new-state {:foo :bar
                        :baz :gaz
                        :something :something}}
           (sut/handle-action {:something :something} {} [:db/ax.assoc :foo :bar :baz :gaz])))
    (testing "it replaces any value at an existing key")
    (is (= {:new-state {:foo :bar
                        :something :something}}
           (sut/handle-action {:foo :existing-value
                               :something :something} {} [:db/ax.assoc :foo :bar])))))

(deftest handle-action-prevent-default
  (testing "it should return a prevent-default effect"
    (is (= {:effects [[:dom/fx.prevent-default]]}
           (sut/handle-action {} {} [:dom/ax.prevent-default])))))

(deftest handle-action-assoc-in
  (testing "it should assoc-in the given value at the specified path"
    (is (= {:new-state {:nested {:foo :bar}}}
           (sut/handle-action {} {} [:db/ax.assoc-in [:nested :foo] :bar])))))

(deftest handle-action-dissoc
  (testing "it should dissoc the given key(s) from the state"
    (is (= {:new-state {:baz :gaz}}
           (sut/handle-action {:foo :bar :baz :gaz} {} [:db/ax.dissoc :foo])))))

(deftest handle-action-update
  (testing "it should update the value at the given key using the provided function"
    (is (= {:new-state {:counter 2}}
           (sut/handle-action {:counter 1} {} [:db/ax.update :counter inc])))))

(deftest handle-action-update-in
  (testing "it should update-in the value at the given path using the provided function"
    (is (= {:new-state {:nested {:counter 2}}}
           (sut/handle-action {:nested {:counter 1}} {} [:db/ax.update-in [:nested :counter] inc])))))

(deftest handle-action-focus-element
  (testing "it should return a focus-element effect"
    (is (= {:effects [[:dom/fx.focus-element :element]]}
           (sut/handle-action {} {} [:dom/ax.focus-element :element])))))

(deftest handle-action-set-input-text
  (testing "it should return a set-input-text effect"
    (is (= {:effects [[:dom/fx.set-input-text :element "new text"]]}
           (sut/handle-action {} {} [:dom/ax.set-input-text :element "new text"])))))

(deftest handle-action-end-editing
  (testing "should update the title and remove the current keycode if the keycode is not 'Escape'"
    (let [initial-state {:app/todo-items [{:item/title "old title"}]
                         :edit/keyup-code "Some key X"}]
      (is (= {:new-state {:app/todo-items [{:item/title "new title"}]}}
             (sut/handle-action initial-state {} [:edit/ax.end-editing "new title" 0])))))

  (testing "should not update the title and remove the current keycode if the keycode is 'Escape'"
    (let [initial-state {:app/todo-items [{:item/title "old title"}]
                         :edit/keyup-code "Escape"}]
      (is (= {:new-state {:app/todo-items [{:item/title "old title"}]}}
             (sut/handle-action initial-state {} [:edit/ax.end-editing "new title" 0])))))

  (testing "should trim the title"
    (let [initial-state {:app/todo-items [{:item/title "old title"}]}]
      (is (= {:new-state {:app/todo-items [{:item/title "new title"}]}}
             (sut/handle-action initial-state {} [:edit/ax.end-editing "  new title  " 0])))))

  (testing "should remove the item if the draft is empty"
    (let [initial-state {:app/todo-items [{:item/title "delete me"}]}]
      (is (= {:new-state {:app/todo-items []}}
             (-> (sut/handle-action initial-state {} [:edit/ax.end-editing "" 0])
                 (update :new-state select-keys [:app/todo-items]))))))

  (testing "should remove the item if the trimmed draft is empty"
    (let [initial-state {:app/todo-items [{:item/title "delete me"}]}]
      (is (= {:new-state {:app/todo-items []}}
             (-> (sut/handle-action initial-state {} [:edit/ax.end-editing " " 0])
                 (update :new-state select-keys [:app/todo-items]))))))

  (testing "should update mark-all state if the item is removed"
    (let [initial-state {:app/todo-items [{:item/title "delete me"
                                           :item/completed true}]
                         :app/mark-all-state false}]
      (is (= {:new-state {:app/todo-items []
                          :app/mark-all-state true}}
             (sut/handle-action initial-state {} [:edit/ax.end-editing "" 0]))
          "should set mark-all state to `true` if the last item is removed"))
    (let [initial-state {:app/todo-items [{:item/title "delete me"
                                           :item/completed true}
                                          {:item/completed false}]
                         :app/mark-all-state true}]
      (is (= {:new-state {:app/todo-items [{:item/completed false}]
                          :app/mark-all-state false}}
             (sut/handle-action initial-state {} [:edit/ax.end-editing "" 0]))
          "should set mark-all state to `false` if the last item remaining is not completed"))
    (let [initial-state {:app/todo-items [{:item/title "delete me"
                                           :item/completed true}
                                          {:item/completed true}]
                         :app/mark-all-state false}]
      (is (= {:new-state {:app/todo-items [{:item/completed true}]
                          :app/mark-all-state true}}
             (sut/handle-action initial-state {} [:edit/ax.end-editing "" 0]))
          "should set mark-all state to `false` if the last item remaining is not completed"))))

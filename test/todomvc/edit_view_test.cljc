(ns todomvc.edit-view-test
  (:require
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [todomvc.actions :as a]
   [test-util :as tu]
   [todomvc.util :as util]
   [todomvc.views :as sut]))

(deftest edit-view
  (testing "rendering the edit view"
    (let [initial-state {:edit/editing-item-index 0}
          edit-view (#'sut/edit-view initial-state 0 {})]
      (is (seq (l/select :input.edit edit-view))
          "it renders the edit view when the index matches the editing item index"))

    (let [initial-state {}
          edit-view (#'sut/edit-view initial-state 0 {})]
      (is (nil? edit-view)
          "it does not render the edit view when its index does not match the editing item index"))

    (let [initial-state  {:edit/editing-item-index 0
                          :edit/keyup-code "Escape"}
          edit-view (#'sut/edit-view initial-state 0 {})]
      (is (nil? edit-view)
          "it does not render the edit view when the keycode is 'Escape'")))

  (testing "edit-view on mount"
    (let [initial-state {:edit/editing-item-index 0}
          on-mount-actions (->> (#'sut/edit-view initial-state 0 {})
                                (tu/select-actions :input.edit [:replicant/on-mount]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {:replicant/node :input-dom-node}
                                                        on-mount-actions)]
      (is (some #{[:dom/fx.focus-element :input-dom-node]}
                (set effects))
          "it focuses the input element")
      (is (= initial-state
             new-state)
          "it does not modify the state")))

  (testing "it populates the edit view from the edited item"
    (let [item {:item/title "Title"}
          initial-state {:edit/editing-item-index 0}
          edit-view (#'sut/edit-view initial-state 0 item)]
      (is (= [(:item/title item)]
             (tu/select-attribute :input.edit [:value] edit-view))
          "it populates the input with the item title")))

  (testing "it updates the draft from the input event"
    (let [initial-state {:edit/editing-item-index 0}
          on-input-actions (->> (#'sut/edit-view initial-state 0 {})
                                (tu/select-actions :input.edit [:on :input]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {:replicant/js-event (util/->js {:target {:value "Input"}})}
                                                        on-input-actions)]
      (is (= "Input"
             (:edit/draft new-state))
          "it updates the draft")
      (is (empty? effects)
          "it does so without other side-effects")))

  (testing "it saves the keycode to the state on keyup"
    (let [initial-state {:edit/editing-item-index 0}
          on-keyup-actions (->> (#'sut/edit-view initial-state 0 {})
                                (tu/select-actions :input.edit [:on :keyup]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {:replicant/js-event (util/->js {:code "Escape"})}
                                                        on-keyup-actions)]
      (is (= "Escape"
             (:edit/keyup-code new-state))
          "it saves the keycode")
      (is (empty? effects)
          "it does so without other side-effects")))

  (testing "it removes the editing index on blur"
    (let [initial-state {:edit/editing-item-index 0}
          on-blur-actions (->> (#'sut/edit-view initial-state 0 {})
                               (tu/select-actions :input.edit [:on :blur]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {}
                                                        on-blur-actions)]
      on-blur-actions
      (is (nil?
           (:edit/editing-item-index new-state))
          "it removes the editing index")
      (is (empty? effects)
          "it does so without other side-effects")))

  (testing "it removes the editing index on form submit"
    (let [initial-state {:edit/editing-item-index 0}
          on-submit-actions (->> (#'sut/edit-view initial-state 0 {})
                                 (tu/select-actions :form [:on :submit]))
          {:keys [new-state effects]} (a/handle-actions initial-state
                                                        {}
                                                        on-submit-actions)]
      (is (nil?
           (:edit/editing-item-index new-state))
          "it removes the editing index")
      (is (some #{[:dom/fx.prevent-default]}
                (set effects))
          "it prevents the default form submission")))

  (testing "end editing"
    (doseq [[input trim-case] [["Input" "no trimming needed"]
                               ["  Input  " "trimming needed"]]]
      (let [item {:item/title "Title"}
            initial-state {:edit/editing-item-index 0
                           :edit/draft input
                           :app/todo-items [item]}
            on-unmount-actions (->> (#'sut/edit-view initial-state 0 item)
                                    (tu/select-actions :form [:replicant/on-unmount]))
            {:keys [new-state effects]} (a/handle-actions initial-state
                                                          {}
                                                          on-unmount-actions)]
        (testing trim-case
          (is (= (string/trim input)
                 (-> new-state :app/todo-items first :item/title))
              "it updates the item title correctly")
          (is (nil? (:edit/editing-item-index new-state))
              "it removes the editing index")
          (is (nil? (:edit/keyup-code new-state))
              "it removes the keycode")
          (is (empty? effects)
              "it does not have other side-effects"))))

    (doseq [[input input-case] [["" "Remove empty"]
                                ["  " "Remove blank"]]]
      (let [item {:item/title "Title2"}
            items [{:item/title "Title1"}
                   item
                   {:item/title "Title3"}]
            initial-state {:edit/editing-item-index 1
                           :edit/keyup-code "Enter"
                           :edit/draft input
                           :app/todo-items items}
            on-unmount-actions (->> (#'sut/edit-view initial-state 1 (second items))
                                    (tu/select-actions :form [:replicant/on-unmount]))
            {:keys [new-state effects]} (a/handle-actions initial-state
                                                          {}
                                                          on-unmount-actions)]
        (testing input-case
          (is (= [(first items) (last items)]
                 (:app/todo-items new-state))
              "it removes the item")
          (is (nil? (:edit/editing-item-index new-state))
              "it removes the editing index")
          (is (nil? (:edit/keyup-code new-state))
              "it removes the keycode")
          (is (empty? effects)
              "it does not have other side-effects"))))

    (let [completed-item {:item/title "Title2"
                          :item/completed? true}
          uncompleted-item {:item/title "Title2"
                            :item/completed? false}]
      (doseq [[items mark-all-state mark-all-case items-behaviour]
              [[[{:item/title "Title1"
                  :item/completed? false}
                 completed-item
                 {:item/title "Title3"
                  :item/completed? false}]
                false
                "remaining items are uncompleted"
                "it sets the mark-all state to false"]
               [[{:item/title "Title1"
                  :item/completed? true}
                 uncompleted-item
                 {:item/title "Title3"
                  :item/completed? true}]
                true
                "remaining items are completed"
                "it sets the mark-all state to true"]]]
        (let [input ""
              initial-state {:edit/editing-item-index 1
                             :edit/keyup-code "Enter"
                             :edit/draft input
                             :app/todo-items items}
              on-unmount-actions (->> (#'sut/edit-view initial-state 1 (second items))
                                      (tu/select-actions :form [:replicant/on-unmount]))
              {:keys [new-state]} (a/handle-actions initial-state
                                                    {}
                                                    on-unmount-actions)]
          (testing mark-all-case
            (is (= mark-all-state
                   (:app/mark-all-checkbox-checked? new-state))
                items-behaviour)))))

    (testing "Escape key"
      ; Notes:
      ; :app/mark-all-state
      ;   * The initial states has no :app/mark-all-state key, even though in “reality” it would have
      ;     We do this so that we can test that its not updated
      ;
      ; :edit/keyup-code and the element lifecycle
      ;   * The state rendering the view had no `:edit/keyup-code "Escape"` entry,
      ;     hence the edit UI was rendered
      ;   * The state captured at unmounting the view _had_ an `:edit/keyup-code "Escape"` entry,
      ;     and this should cause the indexed todo item to be left unchanged
      (let [item {:item/title "Title2"
                  :item/completed? true}
            initial-items [{:item/title "Title1"
                            :item/completed? false}
                           item
                           {:item/title "Title3"
                            :item/completed? false}]
            rendering-state {:edit/editing-item-index 1
                             :edit/keyup-code "KeyT" ; Doesn't matter, but if the "t" in "Input" was typed last...
                             :edit/draft "Input"
                             :app/todo-items initial-items}
            unmounting-state (assoc rendering-state :edit/keyup-code "Escape")
            on-unmount-actions (->> (#'sut/edit-view rendering-state 1 item)
                                    (tu/select-actions :form [:replicant/on-unmount]))
            {:keys [new-state effects]} (a/handle-actions unmounting-state
                                                          {}
                                                          on-unmount-actions)]
        (is (= initial-items
               (:app/todo-items new-state))
            "it doesn't update any item")
        (is (nil? (:edit/editing-item-index new-state))
            "it removes the editing index")
        (is (nil? (:edit/keyup-code new-state))
            "it removes the keycode")
        (is (empty? effects)
            "it does not have other side-effects")
        (is (nil? (:app/mark-all-checkbox-checked? new-state))
            "it does not update the mark-all state")))))
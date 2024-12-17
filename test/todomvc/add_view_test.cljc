(ns todomvc.add-view-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [todomvc.actions :as a]
   [test-util :as tu]
   [todomvc.util :as util]
   [todomvc.views :as sut]))

(deftest maybe-add
  (testing "Adding non-blank"
    (let [result (#'sut/maybe-add [] "New item")]
      (is (= 1
             (count result))
          "it adds an item")
      (is (= "New item"
             (:item/title (first result)))
          "it populates the title")
      (is (false? (:item/completed? (first result)))
          "it adds the item as uncompleted")
      (is (uuid? (:item/id (first result)))
          "it gives the item an id"))
    (is (= "Second item" (-> (#'sut/maybe-add [{:item/title "First item"}] "Second item")
                             second
                             :item/title))
        "it adds the item to the end of the list")
    (is (= "New item" (-> (#'sut/maybe-add [] "  New item  ")
                          first
                          :item/title))
        "it trims the string before adding using it as the title for the item"))

  (testing "Blank or empty"
    (is (= 0 (count (#'sut/maybe-add [] "")))
        "it does not add a new item when the string is empty")
    (is (= 0 (count (#'sut/maybe-add [] "   ")))
        "it does not add a new item when the string is blank")))

(defn test-add-view-mount [state]
  #_{:clj-kondo/ignore [:inline-def :clojure-lsp/unused-public-var]}
  (comment
    (def state {}))
  (let [on-mount-actions (->> (#'sut/add-view state)
                              (tu/select-actions :input.new-todo [:replicant/on-mount]))
        {:keys [new-state effects] :as result} (a/handle-actions state
                                                                 {:replicant/node :input-dom-node}
                                                                 on-mount-actions)]
    (is (= :input-dom-node
           (:add/draft-input-element new-state)))
    (is (empty? effects)
        "it does so without other side-effects")
    result))

(defn test-add-view-input [state add-text]
  #_{:clj-kondo/ignore [:inline-def :clojure-lsp/unused-public-var]}
  (comment
    (def state {:add/draft-input-element :input-dom-node})
    (def add-text "Input"))
  (let [on-input-actions (->> (#'sut/add-view state)
                              (tu/select-actions :input.new-todo [:on :input]))
        {:keys [new-state effects] :as result} (a/handle-actions state
                                                                 {:replicant/js-event (util/->js {:target {:value add-text}})}
                                                                 on-input-actions)]
    (is (= add-text
           (:add/draft new-state)))
    (is (empty? effects)
        "it does so without other side-effects")
    result))

(deftest add-view
  (testing "it saves draft input element on mount"
    (let [initial-state {:app/todo-items [{:item/completed? true
                                           :item/id "id-1"}]
                         :app/mark-all-checkbox-checked? true}
          {:keys [new-state]} (test-add-view-mount initial-state)]

      (testing "it updates the draft from the `.new-todo` input event"
        (let [input-text "Input"
              {:keys [new-state]} (test-add-view-input new-state input-text)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (#'sut/add-view new-state)
                                         (tu/select-actions :form [:on :submit]))
                  {:keys [new-state effects]} (a/handle-actions new-state
                                                                {}
                                                                on-submit-actions)]
              (is (= input-text
                     (-> new-state :app/todo-items second :item/title))
                  "it adds the new item to the todo items")
              (is (false? (-> new-state :app/todo-items second :item/completed?))
                  "it adds the new item as uncompleted")
              (is (false? (:app/mark-all-checkbox-checked? new-state))
                  "it updates the mark-all state to true")
              (is (= ""
                     (:add/draft new-state))
                  "it clears the draft")
              (is (some #{[:dom/fx.prevent-default]}
                        (set effects))
                  "it prevents the default form submit action")
              (is (some #{[:dom/fx.set-input-text :input-dom-node ""]}
                        (set effects))
                  "it clears the input element")))))

      (testing "it trims the input"
        (let [input-text "Input"
              untrimmed-test (str "  " input-text "  ")
              {:keys [new-state]} (test-add-view-input new-state untrimmed-test)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (#'sut/add-view new-state)
                                         (tu/select-actions :form [:on :submit]))
                  {:keys [new-state]} (a/handle-actions new-state
                                                        {}
                                                        on-submit-actions)]
              (is (= input-text
                     (-> new-state :app/todo-items second :item/title))
                  "it adds the new item with trimmed text to the todo items")))))

      (testing "it doesn't add an item when the input is empty"
        (let [input-text ""
              {:keys [new-state]} (test-add-view-input new-state input-text)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (#'sut/add-view new-state)
                                         (tu/select-actions :form [:on :submit]))
                  {:keys [new-state effects]} (a/handle-actions new-state
                                                                {}
                                                                on-submit-actions)]
              (is (= (:app/todo-items initial-state)
                     (:app/todo-items new-state))
                  "it does not add a new item to the todo items")
              (is (= ""
                     (:add/draft new-state))
                  "it clears the draft")
              (is (some #{[:dom/fx.prevent-default]}
                        (set effects))
                  "it prevents the default form submit action")
              (is (some #{[:dom/fx.set-input-text :input-dom-node ""]}
                        (set effects))
                  "the input element remains blank"))))))))
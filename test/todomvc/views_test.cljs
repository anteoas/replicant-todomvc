(ns todomvc.views-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [todomvc.actions :as a]
   [todomvc.views :as sut]))

#_{:clj-kondo/ignore [:private-call]}
(deftest maybe-add
  (testing "it should add a new item when the string is not blank"
    (let [result (sut/maybe-add [] "New item")]
      (is (= 1 (count result)))
      (is (= "New item" (:item/title (first result))))
      (is (false? (:item/completed (first result))))
      (is (uuid? (:item/id (first result))))))

  (testing "it should not add a new item when the string is empty"
    (is (= 0 (count (sut/maybe-add [] "")))))

  (testing "it should not add a new item when the string is blank"
    (is (= 0 (count (sut/maybe-add [] "   ")))))

  (testing "it should trim the string before adding a new item"
    (is (= "New item" (-> (sut/maybe-add [] "  New item  ")
                          first
                          :item/title))))

  (testing "items are added to the end of the list"
    (is (= "Second item" (-> (sut/maybe-add [{:item/title "First item"}] "Second item")
                             second
                             :item/title)))))

(defn- select-attribute
  [selector path data]
  (let [elements (l/select selector data)]
    (->> (keep (fn [element]
                 (when (map? (second element))
                   (get-in (second element) path)))
               elements))))

(defn- flatten-actions [actionss]
  (reduce into [] actionss))

(defn- select-actions [selector path data]
  (->> (select-attribute selector path data)
       flatten-actions))

(defn- handle-actions [state replicant-data actions]
  (reduce (fn [{state :new-state :as acc} action]
            (let [{:keys [new-state effects]} (a/handle-action state replicant-data action)]
              (cond-> acc
                new-state (assoc :new-state new-state)
                effects (update :effects into effects))))
          {:new-state state
           :effects []}
          actions))

#_{:clj-kondo/ignore [:private-call]}
(defn test-add-view-mount [state]
  #_{:clj-kondo/ignore [:inline-def :clojure-lsp/unused-public-var]}
  (comment
    (def state {}))
  (let [on-mount-actions (->> (sut/add-view state)
                              (select-actions :input.new-todo [:replicant/on-mount]))
        {:keys [new-state effects] :as result} (handle-actions state
                                                               {:replicant/node :input-dom-node}
                                                               on-mount-actions)]
    (is (= :input-dom-node
           (:add/draft-input-element new-state)))
    (is (empty? effects)
        "it does so without other side-effects")
    result))

#_{:clj-kondo/ignore [:private-call]}
(defn test-add-view-input [state add-text]
  #_{:clj-kondo/ignore [:inline-def :clojure-lsp/unused-public-var]}
  (comment
    (def state {:add/draft-input-element :input-dom-node})
    (def add-text "Input"))
  (let [on-input-actions (->> (sut/add-view state)
                              (select-actions :input.new-todo [:on :input]))
        {:keys [new-state effects] :as result} (handle-actions state
                                                               {:replicant/js-event (clj->js {:target {:value add-text}})}
                                                               on-input-actions)]
    (is (= add-text
           (:add/draft new-state)))
    (is (empty? effects)
        "it does so without other side-effects")
    result))

#_{:clj-kondo/ignore [:private-call]}
(deftest add-view
  (testing "it saves draft input element on mount"
    (let [{:keys [new-state]} (test-add-view-mount {})]

      (testing "it updates the draft from the `.new-todo` input event"
        (let [input-text "Input"
              {:keys [new-state]} (test-add-view-input new-state input-text)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (sut/add-view new-state)
                                         (select-actions :form [:on :submit]))
                  {:keys [new-state effects]} (handle-actions new-state
                                                              {}
                                                              on-submit-actions)]
              (is (= input-text
                     (-> new-state :app/todo-items first :item/title))
                  "it adds the new item to the todo items")
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
            (let [on-submit-actions (->> (sut/add-view new-state)
                                         (select-actions :form [:on :submit]))
                  {:keys [new-state]} (handle-actions new-state
                                                      {}
                                                      on-submit-actions)]
              (is (= input-text
                     (-> new-state :app/todo-items first :item/title))
                  "it adds the new item with trimmed text to the todo items")))))

      (testing "it doesn't add an item when the input is empty"
        (let [input-text ""
              {:keys [new-state]} (test-add-view-input new-state input-text)]

          (testing "it handles the form submit event"
            (let [on-submit-actions (->> (sut/add-view new-state)
                                         (select-actions :form [:on :submit]))
                  {:keys [new-state effects]} (handle-actions new-state
                                                              {}
                                                              on-submit-actions)]
              (is (empty? (:app/todo-items new-state))
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

(deftest app-view
  (testing "it shows a `.todoapp` element"
    (is (seq (l/select '.todoapp (sut/app-view {})))))

  (testing "the `.todoapp` element contains a `.header` element with a h1 element with the text `todos`"
    (is (= '([:h1 "todos"])
           (l/select '[.todoapp .header h1] (sut/app-view {})))))

  (testing "the `.todoapp` element contains an autofocused `.new-todo` input in the `.header` element"
    (is (= '(true)
           (select-attribute '[.todoapp .header input.new-todo]
                                [:autofocus]
                                (sut/app-view {})))
        "with an empty app state")

    (is (= '(true)
           (select-attribute '[.todoapp .header input.new-todo]
                                [:autofocus]
                                (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
        "with items in the app state"))

  (testing "the `.todoapp` element `.main` element"
    (is (empty? (l/select '[.todoapp .main] (sut/app-view {})))
        "it has no `.main` view when there are no items")
    (is (seq (l/select '[.todoapp .main] (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
        "it has a `.main` view when there are items"))

  (testing "the `.todoapp` element `.footer` element"
    (is (empty? (l/select '[.todoapp .footer] (sut/app-view {})))
        "it has no `.footer` when there are no items")
    (is (seq (l/select '[.todoapp .footer] (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
        "it has a `.footer` when there are items"))

  (testing "all form-submits have a prevent-default action"
    (is (every? (partial some #{[:dom/ax.prevent-default]})
                (->> (sut/app-view {:app/todo-items [{:item/title "First item"}]
                                    :edit/editing-item-index 0
                                    :app/item-filter :filter/all})
                     (select-attribute 'form [:on :submit])
                     (map set))))))
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
            (let [{:keys [new-state new-effects]} (a/handle-action state replicant-data action)]
              (cond-> acc
                new-state (assoc :new-state new-state)
                new-effects (update :effects concat new-effects))))
          {:new-state state
           :effects []}
          actions))

#_{:clj-kondo/ignore [:private-call]}
(comment
  (->> (select-actions 'input.new-todo [:replicant/on-mount]
                       (sut/add-view {:add/draft "New item"}))
       (handle-actions {} {:replicant/js-event (clj->js {:event {:target {:value "New item"}}})
                           :replicant/node :input-dom-node}))
  [[]
   ['input.new-todo [:on :input]]
   ['form [:on :submit]]]
  :rcf)

#_{:clj-kondo/ignore [:private-call]}
(deftest add-view
  (testing "its form-submit has a prevent-default effect"
    (is (some #{[:dom/ax.prevent-default]}
              (->> (sut/add-view {:add/draft "New item"})
                   (select-actions 'form [:on :submit])
                   set))))

  (testing "it saves draft input element on mount"
    (let [on-mount-actions (->> (sut/add-view {:add/draft "New item"})
                                (select-actions :input.new-todo [:replicant/on-mount]))
          {:keys [new-state effects]} (handle-actions {}
                                                      {:replicant/node :input-dom-node}
                                                      on-mount-actions)]
      (is (= :input-dom-node
             (:add/draft-input-element new-state)))
      (is (empty? effects)
          "it does so without other side-effects"))))

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

  (testing "all form-submits have a prevent-default effect"
    (is (every? (partial some #{[:dom/ax.prevent-default]})
                (->> (sut/app-view {:app/todo-items [{:item/title "First item"}]
                                    :edit/editing-item-index 0
                                    :app/item-filter :filter/all})
                     (select-attribute 'form [:on :submit])
                     (map set))))))
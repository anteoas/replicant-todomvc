(ns todomvc.views-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [todomvc.hiccup-attributes :as ha]
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

#_{:clj-kondo/ignore [:private-call]}
(deftest add-view
  (testing "its form-submit has a prevent-default effect"
    (is (some #{[:dom/ax.prevent-default]}
              (->> (sut/add-view {:add/draft "New item"})
                   (ha/select-attribute 'form [:on :submit])
                   first
                   set)))))

(deftest app-view
  (testing "it shows a `.todoapp` element"
    (is (seq (l/select '.todoapp (sut/app-view {})))))

  (testing "the `.todoapp` element contains a `.header` element with a h1 element with the text `todos`"
    (is (= '([:h1 "todos"])
           (l/select '[.todoapp .header h1] (sut/app-view {})))))

  (testing "the `.todoapp` element contains an autofocused `.new-todo` input in the `.header` element"
    (is (= '(true)
           (ha/select-attribute '[.todoapp .header input.new-todo]
                                [:autofocus]
                                (sut/app-view {})))
        "with an empty app state")

    (is (= '(true)
           (ha/select-attribute '[.todoapp .header input.new-todo]
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
                     (ha/select-attribute 'form [:on :submit])
                     (map set))))))
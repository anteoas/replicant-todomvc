(ns todomvc.app-view-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lookup.core :as l]
   [test-util :as tu]
   [todomvc.views :as sut]))

(deftest app-view
  (testing ".todoapp element"
    (is (seq (l/select '.todoapp (sut/app-view {})))
        "it shows a `.todoapp` element")
    (is (= '([:h1 "todos"])
           (l/select '[.todoapp .header h1] (sut/app-view {})))
        "it contains a `.header` element with a h1 element with the text `todos`")

    (testing "the .-new-todo input"
      (is (= '(true)
             (tu/select-attribute [:.todoapp :.header :input.new-todo]
                                  [:autofocus]
                                  (sut/app-view {})))
          "it is an `input` contained in the `.header` element inside `.todoapp`")
      (is (= '(true)
             (tu/select-attribute [:.todoapp :.header :input.new-todo]
                                  [:autofocus]
                                  (sut/app-view {})))
          "it is present with an empty app state")
      (is (= '(true)
             (tu/select-attribute [:.todoapp :.header :input.new-todo]
                                  [:autofocus]
                                  (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
          "it is present with items in the app state")))

  (testing "the `.todoapp` `.main` element"
    (is (empty? (l/select '[.todoapp .main] (sut/app-view {})))
        "it has no `.main` view when there are no items")
    (is (seq (l/select '[.todoapp .main] (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
        "it has a `.main` view when there are items"))

  (testing "the `.todoapp` `.footer` element"
    (is (empty? (l/select '[.todoapp .footer] (sut/app-view {})))
        "it has no `.footer` when there are no items")
    (is (seq (l/select '[.todoapp .footer] (sut/app-view {:app/todo-items [{:item/title "First item"}]})))
        "it has a `.footer` when there are items"))

  (testing "Prevent default"
    (is (every? (partial some #{[:dom/ax.prevent-default]})
                (->> (sut/app-view {:app/todo-items [{:item/title "First item"}]
                                    :edit/editing-item-index 0
                                    :app/item-filter :filter/all})
                     (tu/select-attribute 'form [:on :submit])
                     (map set)))
        "all form-submits have a prevent-default action")))
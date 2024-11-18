(ns todomvc.actions-test
  (:require [todomvc.actions :as sut]
            [clojure.test :refer [deftest is testing]]))

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
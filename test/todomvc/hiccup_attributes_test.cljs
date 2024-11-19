(ns todomvc.hiccup-attributes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [todomvc.hiccup-attributes :as sut]))

(deftest select-attribute
  (testing "it finds attributes given a selector and an attributes path"
    (is (= (sut/select-attribute '[a d.e] [:f] [:a
                                                [:b]
                                                [:c
                                                 [:d.e {:f :g}]
                                                 [:h.i {:j :k}]]])
           '(:g))))

  (testing "it finds all attributes given a selector and an attributes path matching multiple elements"
    (is (= (sut/select-attribute '[a .x] [:y] [:a
                                               [:b.x {:y :z1}]
                                               [:c.x {:y :z2}
                                                [:d.x {:y :z3}]
                                                [:h.i {:j :k}]]])
           '(:z1 :z2 :z3))))

  (testing "it finds attributes within attributes"
    (is (= (sut/select-attribute '[a] [:b :c] [:a {:b {:c {:d {:e [:f :g]}}}}])
           '({:d {:e [:f :g]}}))))

  (testing "we can address vectors in attributes"
    (is (= (sut/select-attribute '[a] [:b :c 2] [:a {:b {:c [:d :e :f :g]}}])
           '(:f))))

  (testing "it returns the whole attribute with an empty path"
    (is (= '({:d :e})
           (sut/select-attribute '[a c] [] [:a
                                            [:b
                                             [:c {:d :e}]]]))))

  (testing "it includes matched class attribute"
    (is (= (sut/select-attribute '[a.b.c] [] [:a.b {:class "c"
                                                    :d :e}])
           '({:class #{"b" "c"}
              :d :e}))))

  (testing "it returns an empty list if the attribute is not found"
    (is (= '()
           (sut/select-attribute '[a c] [:n] [:a
                                              [:b
                                               [:c {:d :e}]]]))))

  (testing "it returns an empty list if the selector does not match"
    (is (= '()
           (sut/select-attribute '[n] [] [:a
                                          [:b]
                                          [:c {:d :e}]])))))

(def haystack [:a
               [:b.x {:c {:d :e}}
                [:f]]
               [:g.x {:id "h"
                      :i :j}
                [:k {:l [[1 2 3]
                         [4 5 6]]}]]
               [:m {:n [[7 8 9]
                        [10 11 12]]}]])

(deftest collect-attributes
  (testing "it collects all attributes for the selector-paths"
    (is (= '(:e {:class #{"x"} :id "h" :i :j})
           (sut/collect-attributes [['[a b] [:c :d]]
                                    ['[a g#h] []]]
                                   haystack)))

    (is (= [:e]
           (sut/collect-attributes [['[a b] [:c :d]]]
                                   haystack)))

    (is (= [{:class #{"x"}, :id "h", :i :j}]
           (sut/collect-attributes [['[a g#h] []]]
                                   haystack)))

    (is (= [[[1 2 3] [4 5 6]]]
           (sut/collect-attributes [['[g#h k] [:l]]]
                                   haystack)))

    (is (= [[[1 2 3] [4 5 6]] [[7 8 9] [10 11 12]]]
           (sut/collect-attributes [['[g#h k] [:l]]
                                    ['m [:n]]]
                                   haystack)))

    (is (= [:e [[1 2 3] [4 5 6]] [[7 8 9] [10 11 12]]]
           (sut/collect-attributes [['[a b] [:c :d]]
                                    ['[g#h k] [:l]]
                                    ['m [:n]]]
                                   haystack)))))
(ns pez.hiccup-attributes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pez.hiccup-attributes :as sut]))

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

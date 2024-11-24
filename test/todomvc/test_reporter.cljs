(ns todomvc.test-reporter
  (:require [cljs.test]))

(def ^:private initial-state {:level 0
                              :seen-context nil})

(defonce ^:private !state (atom initial-state))

(defn- indent [level]
  (apply str (repeat (* 2 level) " ")))

(defn- set-state-from-env! []
  (let [contexts (:testing-contexts (cljs.test/get-current-env))]
    (swap! !state assoc
           :level (count contexts)
           :seen-context contexts)))

#_(defn hierarchical-reporter []
  (let [level (atom 0)]
    (fn [m]
      (case (:type m)
        :begin-test-ns
        (println (str "\n" (indent @level) "Namespace: " (:ns m)))

        :begin-test-var
        (println (str (indent @level) "Test: " (:var m)))

        :begin-test
        (swap! level inc)

        :end-test
        (swap! level dec)

        :pass
        (println (str (indent @level) "✔ " (:message m)))

        :fail
        (println (str (indent @level) "✘ " (:message m)))

        :error
        (println (str (indent @level) "⚠ " (:message m)))

        (report m)))))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [_m]
  (reset! !state initial-state))

(defmethod cljs.test/report [:cljs.test/default :end-test] [_m]
  (reset! !state initial-state))

(def ^:private original-pass (get-method cljs.test/report [:cljs.test/default :pass]))
(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  #_{:file nil,
     :end-column 39,
     :type :pass,
     :column 19,
     :line 158,
     :expected (some #{[:dom/fx.set-input-text :input-dom-node ""]} (set effects)),
     :end-line 159,
     :actual
     (some
      #{[:dom/fx.set-input-text :input-dom-node ""]}
      #{[:dom/fx.set-input-text :input-dom-node ""] [:dom/fx.prevent-default]}),
     :message "the input element remains blank"}
  #_{:report-counters {:test 3 :pass 34 :fail 4 :error 0}
     :testing-vars (#'todomvc.views-test/add-view)
     :testing-contexts ("it handles the form submit event"
                        "it doesn't add an item when the input is empty"
                        "it saves draft input element on mount")
     :formatter "#object [cljs$core$pr_str]"
     :reporter :cljs.test/default}
  (let [seen-context (:seen-context @!state)
        message (or (:message m) (pr-str (:expected m)))]
    (set-state-from-env!)
    (let [context (:seen-context @!state)]
      (when-not (or #_(nil? (:seen-context @!state))
                 (= seen-context
                    (:seen-context @!state)))
        (println (str (indent (:level @!state))
                      (first context)))))
    (println (str (indent (inc (:level @!state)))
                  (str "✔ " message))))
  (original-pass m))

(comment
  (ns-unmap cljs.test 'report)
  :rcf)
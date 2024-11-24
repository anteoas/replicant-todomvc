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

(defn- report-test [m bullet]
  (let [seen-context (:seen-context @!state)
        message (or (:message m) (pr-str (:expected m)))]
    (set-state-from-env!)
    (let [context (:seen-context @!state)]
      (when-not (= seen-context
                   (:seen-context @!state))
        (println (str (indent (:level @!state))
                      (first context)))))
    (println (str (indent (inc (:level @!state)))
                  (str bullet " " message)))))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [_m]
  (reset! !state initial-state))

(defmethod cljs.test/report [:cljs.test/default :end-test-ns] [_m]
  (reset! !state initial-state))

(def ^:private original-pass (get-method cljs.test/report [:cljs.test/default :pass]))
(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  (report-test m "✓")
  (original-pass m))

(def ^:private original-fail (get-method cljs.test/report [:cljs.test/default :fail]))
(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (report-test m "✗")
  (original-fail m))

(def ^:private original-error (get-method cljs.test/report [:cljs.test/default :error]))
(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (report-test m "⚠")
  (original-error m))

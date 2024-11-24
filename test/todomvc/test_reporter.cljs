(ns todomvc.test-reporter
  (:require [cljs.test]
            [shadow.test.node]))

(defn- bold [text]
  (str "\033[1m" text "\033[22m"))

(defn- gray [text]
  (str "\033[90m" text "\033[39m"))

(defn- green [text]
  (str "\033[32m" text "\033[22m"))

(defn- red [text]
  (str "\033[31m" text "\033[39m"))

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

(defn- report-test [m {:keys[color bullet bullet-color]}]
  (let [seen-context (:seen-context @!state)
        message (or (:message m) (pr-str (:expected m)))]
    (set-state-from-env!)
    (let [context (:seen-context @!state)]
      (when (and context (not= seen-context context))
        (print (str (indent (:level @!state))
                    (bold (first context))))))
    (println (str (indent (inc (:level @!state)))
                  (str (bullet-color bullet) " " (color message))))))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [_m]
  (reset! !state initial-state))

(defmethod cljs.test/report [:cljs.test/default :end-test-ns] [_m]
  (reset! !state initial-state))

(def ^:private original-pass (get-method cljs.test/report [:cljs.test/default :pass]))
(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  (report-test m {:color gray :bullet "✓" :bullet-color green})
  (original-pass m))

(def ^:private original-fail (get-method cljs.test/report [:cljs.test/default :fail]))
(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (report-test m {:color red :bullet "✗" :bullet-color red})
  (original-fail m))

(def ^:private original-error (get-method cljs.test/report [:cljs.test/default :error]))
(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (report-test m {:color red :bullet "✗" :bullet-color red})
  (original-error m))

(defn main [& args]
  (apply shadow.test.node/main args))
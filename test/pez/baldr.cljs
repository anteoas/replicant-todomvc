(ns pez.baldr
  (:require [cljs.test]
            [clojure.string :as string]))

(defn- bold [text]
  (str "\033[1m" text "\033[22m"))

(defn- gray [text]
  (str "\033[90m" text "\033[39m"))

(defn- green [text]
  (str "\033[32m" text "\033[22m"))

(defn- red [text]
  (str "\033[31m" text "\033[39m"))

(def ^:private initial-state {:level 0
                              :seen-context nil
                              :failure-prints []})

(defonce ^:private !state (atom initial-state))

(defn- indent [level]
  (apply str (repeat (* 2 level) " ")))

(defn- set-state-from-env! []
  (let [contexts (:testing-contexts (cljs.test/get-current-env))]
    (swap! !state assoc
           :level (+ 1 (count contexts))
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
  (swap! !state merge (select-keys initial-state [:level :seen-context])))

(def ^:private original-summary (get-method cljs.test/report [:cljs.test/default :summary]))
(defmethod cljs.test/report [:cljs.test/default :summary] [m]
  (println)
  (doseq [[i failure-print] (map-indexed vector (:failure-prints @!state))]
    (println (red (str (inc i) ") " (string/trim failure-print)))))
  (reset! !state initial-state)
  (original-summary m))

(def ^:private original-pass (get-method cljs.test/report [:cljs.test/default :pass]))
(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  (report-test m {:color gray :bullet "✓" :bullet-color green})
  (original-pass m))

(def ^:private original-fail (get-method cljs.test/report [:cljs.test/default :fail]))
(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (let [failure-printout (with-out-str (original-fail m))]
    (swap! !state update :failure-prints conj failure-printout))
  (report-test m {:color red :bullet (str (count (:failure-prints @!state)) ")") :bullet-color red}))

(def ^:private original-error (get-method cljs.test/report [:cljs.test/default :error]))
(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (let [error-printout (with-out-str (original-error m))]
    (swap! !state update :failure-prints conj error-printout))
  (report-test m {:color red :bullet (str (count (:failure-prints @!state)) ")") :bullet-color red}))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (println (str (indent 1) (map #(:name (meta %)) (:testing-vars (cljs.test/get-current-env))))))


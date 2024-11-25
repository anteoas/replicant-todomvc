(ns pez.baldr
  (:require [cljs.test]
            [clojure.string :as string]))

(defn- bold [text]
  (str "\033[1m" text "\033[22m"))

(defn- default [text]
  (str "\033[39m" text "\033[22m"))

(defn- gray [text]
  (str "\033[90m" text "\033[39m"))

(defn- green [text]
  (str "\033[1;32m" text "\033[22m"))

(defn- red [text]
  (str "\033[31m" text "\033[39m"))

(def ^:private initial-state {:contexts nil
                              :failure-prints []})

(defonce ^:private !state (atom initial-state))

(defn- indent [level]
  (apply str (repeat (* 2 level) " ")))

(defn- -report-test! [m env state {:keys [color bullet bullet-color]}]
  (let [seen-contexts (:contexts state)
        message (or (:message m) (pr-str (:expected m)))
        contexts (:testing-contexts env)
        common-prefix-length (count (take-while true? (map = (reverse seen-contexts)
                                                           (reverse contexts))))
        new-contexts (reverse (take (- (count contexts) common-prefix-length) contexts))]
    (when (seq new-contexts)
      (doseq [[idx ctx] (map-indexed vector new-contexts)]
        (println (str (indent (+ common-prefix-length idx 2))
                      (bold ctx)))))
    (println (str (indent (+ 2 (count contexts)))
                  (str (bullet-color bullet) " " (color message))))
    (assoc state :contexts contexts)))

(defn report-test! [m config]
  (reset! !state (-report-test! m (cljs.test/get-current-env) @!state config)))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [_m]
  (swap! !state merge (select-keys initial-state [:contexts])))

(def ^:private original-summary (get-method cljs.test/report [:cljs.test/default :summary]))
(defmethod cljs.test/report [:cljs.test/default :summary] [m]
  (when (seq (:failure-prints @!state))
    (println))
  (doseq [[i failure-print] (map-indexed vector (:failure-prints @!state))]
    (println (red (str (inc i) ") " (string/trim failure-print)))))
  (reset! !state initial-state)
  (original-summary m))

(def ^:private original-pass (get-method cljs.test/report [:cljs.test/default :pass]))
(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  (report-test! m {:color gray
                  :bullet "✓"
                  :bullet-color green})
  (original-pass m))

(def ^:private original-fail (get-method cljs.test/report [:cljs.test/default :fail]))
(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (let [failure-printout (with-out-str (original-fail m))]
    (swap! !state update :failure-prints conj failure-printout))
  (report-test! m {:color red
                  :bullet (str (count (:failure-prints @!state)) ")")
                  :bullet-color red}))

(def ^:private original-error (get-method cljs.test/report [:cljs.test/default :error]))
(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (let [error-printout (with-out-str (original-error m))]
    (swap! !state update :failure-prints conj error-printout))
  (report-test! m {:color red
                  :bullet (str (count (:failure-prints @!state)) ")")
                  :bullet-color red}))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (println (str (indent 1) (default (:var m)))))

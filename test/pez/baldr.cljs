(ns pez.baldr
  (:require [cljs.test]
            [clojure.string :as string]))

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

(defn- get-report [m env state {:keys [color bullet bullet-color]}]
  (let [seen-contexts (:contexts state)
        contexts (:testing-contexts env)
        common-contexts (take-while true? (map = (reverse seen-contexts) (reverse contexts)))
        common-prefix-length (count common-contexts)
        new-contexts (reverse (take (- (count contexts) common-prefix-length) contexts))
        message (or (:message m) (pr-str (:expected m)))]
    {:new-state (assoc state :contexts contexts)
     :printouts (cond-> []
                  (seq new-contexts) (into (map-indexed (fn [idx ctx]
                                                          (str (indent (+ common-prefix-length idx 2))
                                                               (default ctx)))
                                                        new-contexts))
                  :always (conj (str (indent (+ 2 (count contexts)))
                                     (str (bullet-color bullet) " " (color message)))))}))

(defn report! [m config]
  (let [{:keys [new-state printouts]} (get-report m (cljs.test/get-current-env) @!state config)]
    (reset! !state new-state)
    (doseq [printout printouts]
      (println printout))))

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
  (report! m {:color gray
              :bullet "✓"
              :bullet-color green})
  (original-pass m))

(def ^:private original-fail (get-method cljs.test/report [:cljs.test/default :fail]))
(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (let [failure-printout (with-out-str (original-fail m))]
    (swap! !state update :failure-prints conj failure-printout))
  (report! m {:color red
              :bullet (str (count (:failure-prints @!state)) ")")
              :bullet-color red}))

(def ^:private original-error (get-method cljs.test/report [:cljs.test/default :error]))
(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (let [error-printout (with-out-str (original-error m))]
    (swap! !state update :failure-prints conj error-printout))
  (report! m {:color red
              :bullet (str (count (:failure-prints @!state)) ")")
              :bullet-color red}))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (println (str (indent 1) (default (:var m)))))

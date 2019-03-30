(ns protor.catalog
  (:require [integrant.core :as ig]
            [clojure.main :as main]
            [clojure.string :as string]
            [protor.morph :as morph])
  (:import [clojure.lang PersistentQueue]))

(defn item-words [morph item-name]
  (-> item-name
      string/lower-case
      (string/replace #"\." ". ")
      (string/split #" ")
      (->>
       (remove string/blank?)
       (mapcat (comp
                morph/single-morph-of-the-type
                (partial morph/normal-morphs morph)))
       morph/best-normal-forms
       (into []))))

(defn all-item-words [{:keys [morph state]}]
  (-> state
      deref :receipts vals (->> (map :items) flatten (map :name)
                                (map (juxt identity (partial item-words morph)))
                                )))

(defn dequeue!
  [queue]
  (loop []
    (let [q     @queue
          value (peek q)
          nq    (pop q)]
      (when-not (empty? q)
        (if (compare-and-set! queue q nq)
          value
          (recur))))))


(defn repl [inputs answer-fn]
  (main/repl
   :read #(if (empty? @inputs) %2 (main/repl-read %1 %2))
   :prompt #(prn (peek @inputs))
   :eval (fn [output]
           (let [input (dequeue! inputs)]
             (answer-fn input output)))
   :print #(prn "Ok, answer-fn returns: " %)))

(defn classify! [{:keys [morph state] :as catalog}]
  (let [questions (all-item-words catalog)
        {:keys [short long] :or {short {} long {}}} (:catalog state)
        known (-> long keys set)
        questions* (remove known questions)
        inputs (atom (into PersistentQueue/EMPTY questions*))
        short* (atom short)
        long* (atom long)]
    (repl inputs (fn [[long-key short-key] answer]
                   (swap! long* assoc long-key answer)
                   (swap! short* assoc short-key answer)
                   (loop [[[long-key short-key] & tail] @inputs]
                     (when-let [pre-answer (@short* short-key)]
                       (println "Skipping " long-key " -> " pre-answer)
                       (dequeue! inputs)
                       (recur tail)))))
    (swap! state update-in [:catalog :short] merge @short*)
    (swap! state update-in [:catalog :long] merge @long*)
    true))

(defn item->category [short-key->category morph item-name]
  (let [short-key (item-words morph item-name)]
    (get short-key->category short-key)))

;; use after classify run
(defn sums-by-category [{:keys [morph state] :as catalog}]
  (let [state @state ;; FIXME
        short-key->category (-> state :catalog :short)]
    (loop [acc {}
           [{:keys [name sum] :as item} & rest] (-> state :receipts vals (->> (mapcat :items)))]
      (if item
        (if-let [category (item->category short-key->category morph name)]
          (recur (update-in acc [category] (fnil + 0) sum) rest)
          (do (println "Name not classified: " name)
              (recur acc rest)))
        acc))))


(defmethod ig/init-key :catalog [_ {:keys [morph state]}]
  {:state state
   :morph morph})

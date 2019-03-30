(ns protor.catalog
  (:require [integrant.core :as ig]
            [clojure.string :as string]
            [protor.morph :as morph]))

(defn item-words* [morph item-name]
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

(defn item-words [{:keys [morph state]}]
  (-> state
      deref :receipts vals (->> (map :items) flatten (map :name)
                                (map (juxt identity (partial item-words* morph)))
                                )))

(defmethod ig/init-key :catalog [_ {:keys [morph state]}]
  {:state state
   :morph morph
   :cache (atom {})})

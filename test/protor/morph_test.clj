(ns protor.morph-test
  (:require [clojure.test :refer [deftest is are]]
            [protor.morph :as sut])
  (:import [org.apache.lucene.morphology.russian RussianLuceneMorphology]))

(def morph (RussianLuceneMorphology.))

;; beware cyrillic chars
(deftest normal-morphs
  (are [res s]
      (= res (-> (sut/normal-morphs morph s)
                 (->> (sort-by (sut/neg :weight)))
                 first
                 (select-keys [:word :type])))

    {:type :verb, :word "замокнуть"} "замокнуть"
    {:type :verb, :word "работать"} "работал"
    {:type :preposition, :word "с"} "с"
    {:type :pronoun, :word "он"} "он"
    {:type :adjective, :word "детский"} "детского"
    {:type :noun, :word "сметана"} "сметана"
    ))

(ns protor.morph
  (:require [integrant.core :as ig]
            [medley.core :as medley]
            [clojure.string :as string])
  (:import [org.apache.lucene.morphology.russian RussianLuceneMorphology]
           [org.apache.lucene.morphology LuceneMorphology]))


(defn normal-forms-without-info [^LuceneMorphology morph ^String s]
  (if (.checkString morph s)
    (seq (.getNormalForms morph s))
    []))

;; beware cyrillic chars
(def word-types
  {"С" :noun
   "МС" :pronoun
   "ПРЕДЛ" :preposition
   "ИНФИНИТИВ" :verb
   "Г" :verb
   "КР_ПРИЧАСТИЕ" :adjective
   "П" :adjective})

(def types-weight
  {:noun 3
   :pronoun 0
   :preposition 0
   :verb 0
   :adjective 0})

(defn neg [f] (comp (fnil #(* -1 %) 0) f))

;; LuceneMorphology and Heuristic are very string-oriented :'(
;; no good interface inside
(defn normal-morphs [^LuceneMorphology morph ^String s]
  (if (.checkString morph s)
    (for [word-with-info (.getMorphInfo morph s)]
      (let [[word morph-info] (string/split word-with-info #"\|" 2)
            [x type & rest] (string/split morph-info #" ")
            word-type (word-types type)]
        {:word word
         :x x
         :rest rest
         :weight (types-weight word-type)
         :type word-type}))
    []))

(defn single-morph-of-the-type [normal-morphs]
  (medley/distinct-by :type normal-morphs))

(defn short-noun? [{:keys [word type] :as m}]
  (and (= :noun type) (-> word count (< 3))))

(defn best-normal-forms [normal-morphs]
  (->> normal-morphs
       (remove short-noun?)
       (filter (comp #{:noun} :type))
       ;; (remove #(some-> % :weight zero?))
       ;; (sort-by (neg :weight)) (take 2)
       (map :word)))

(defmethod ig/init-key :morph [_ _opts]
  (RussianLuceneMorphology.))

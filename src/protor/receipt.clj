(ns protor.receipt
  (:require [clojure.spec-alpha2 :as s]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [jsonista.core :as j]
            [camel-snake-kebab.core :refer [->kebab-case-keyword ->camelCaseString]]
            [clj-antlr.core :as antlr])
  (:import [org.antlr.v4.runtime BufferedTokenStream]))

(def json-parser (antlr/parser (slurp (io/resource "json.g4"))
                               {:throw? false :format :raw}))

(defn extract-json-from-start
  "First symbol should be correct starting point"
  [s]
  (let [{:keys [tree ^BufferedTokenStream tokens errors]} (antlr/parse json-parser s)]
    (.getText tokens tree)))

(s/def ::name string?)
(s/def ::sum int?)
(s/def ::price int?)
(s/def ::quantity int?)
(s/def ::item (s/keys :req-un [::name ::sum ::quantity ::price]))

(s/def ::items (s/coll-of ::item :min-count 1))
(s/def ::total-sum int?)
(s/def ::date-time string?)
(s/def ::fiscal-drive-number string?)
(s/def ::fiscal-document-number int?)
(s/def ::fiscal-sign int?)
(s/def ::receipt (s/keys :req-un [::items ::total-sum ::date-time
                                  ::fiscal-drive-number ::fiscal-document-number
                                  ::fiscal-sign]))

(defn body->json [input]
  (loop [json nil body input]
    (if-let [idx (string/index-of body "{")]
      (let [sbody (subs body idx)
            json* (extract-json-from-start sbody)]
        ;; (log/debug (pr-str {:try-json-at idx}))
        (recur (max-key count json json*)
               (subs body (+ idx (count json*)))))
      json)))

(def mapper (j/object-mapper {:encode-key-fn ->camelCaseString :decode-key-fn ->kebab-case-keyword}))

(defn with-unique-key [{:keys [fiscal-drive-number fiscal-document-number fiscal-sign] :as receipt}]
  (let [id (str fiscal-drive-number "/" fiscal-document-number "/" fiscal-sign)]
    (assoc receipt :id id)))

(defn json->receipt [json]
  (-> json
      (j/read-value mapper)
      :document
      :receipt
      with-unique-key))

(def body->receipt (comp json->receipt
                         body->json))

(s/fdef body->receipt
  :args (s/cat :json string?)
  :ret ::receipt)


(ns user
  (:require [integrant.core :as ig]
            [clojure.pprint :as pprint]
            [clojure.spec-alpha2 :as s]
            [clojure.repl] ;; FIXME in cljsh
            [robert.hooke] ;; FIXME in cljsh
            [cljsh.source :refer [source-expand-ns]]
            [eftest.runner :refer [find-tests run-tests]]
            [clojure.java.io :as io]
            [clj-antlr.core :as antlr]
            [cljsh.complement]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [jsonista.core :as j]
            [aprint.core :refer [aprint ap]]
            [integrant.repl :refer [clear halt go init prep suspend resume reset]]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.find :as find]
            [protor.main :as main]
            [protor.state :as state]
            [protor.morph :as morph]
            [protor.catalog :as catalog]
            [protor.receipt :as receipt]))

(defn read-edn-string [s]
  (binding [*data-readers* {'inst clojure.instant/read-instant-calendar}]
    (edn/read-string s)))

(integrant.repl/set-prep! #(main/read-config "config.edn"))

;; integrant.repl ignores edn config
;; however this is not sufficient
#_(def clj+edn (update-in find/clj [:extensions] conj ".edn"))
#_(defn reset []
  (suspend)
  (#'repl/do-refresh {:platform clj+edn} 'integrant.repl/resume))
;; interesting debug
(comment
  (clojure.tools.namespace.dir/scan-dirs repl/refresh-tracker [] {:platform clj+edn})

  ;; print some last header
  (-> integrant.repl.state/system :state deref :incoming last :headers (->> (reduce-kv #(merge %1 %3) {})) (get "Received") println)
  )

(defonce _patch (cljsh.complement/patch))

(defn process-all-by-ts []
  (reduce (fn [acc x]
            (let [{:keys [id] :as receipt} (-> x :body state/plain-body :body receipt/body->receipt)]
              (assoc acc id receipt)))
          {}
          (-> integrant.repl.state/system :state deref :incoming)))

(defn save-all-by-ts! []
  (-> integrant.repl.state/system :state (swap! update-in [:receipts] merge (process-all-by-ts)))
  true)

(println "System: #'integrant.repl.state/system\nRun with (reset)\nRun tests: (run-tests (find-tests \"test\"))")

(defn test-sym [var-sym]
  (let [ns-sym (-> var-sym namespace symbol)]
    (require ns-sym :reload)
    (run-tests (find-tests var-sym))))

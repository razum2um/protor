(ns user
  (:require [integrant.core :as ig]
            [clojure.repl] ;; FIXME in cljsh
            [robert.hooke] ;; FIXME in cljsh
            [cljsh.source :refer [source-expand-ns]]
            [cljsh.complement]
            [clojure.edn :as edn]
            [aprint.core :refer [aprint ap]]
            [integrant.repl :refer [clear halt go init prep suspend resume reset]]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.find :as find]
            [protor.main :as main]))

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

(println "System: #'integrant.repl.state/system\nRun with (reset)")

(ns user
  (:require [integrant.core :as ig]
            [clojure.repl] ;; FIXME in cljsh
            [cljsh.source :refer [source-expand-ns]]
            [aprint.core :refer [aprint ap]]
            [integrant.repl :refer [clear halt go init prep suspend resume reset]]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.find :as find]
            [protor.main :as main]))

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
  )

(println "System: #'integrant.repl.state/system\nRun with (reset)")

(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [protor.main :as main]))

(integrant.repl/set-prep! #(main/read-config "config.edn"))

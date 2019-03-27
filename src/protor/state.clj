(ns protor.state
  (:require [integrant.core :as ig]))

(defmethod ig/init-key :state [_ {:keys [handler] :as opts}]
  (println "start state")
  )

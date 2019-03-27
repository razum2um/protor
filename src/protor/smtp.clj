(ns protor.smtp
  (:require [integrant.core :as ig]))

(defmethod ig/init-key :smtp [_ {:keys [handler] :as opts}]
  (println "start smtp")
  )

(defmethod ig/halt-key! :smtp [_ smtp]
  (println "stop smtp")
  )

(ns protor.state
  (:require [integrant.core :as ig]
            [duratom.core :as duratom]))

(defmethod ig/init-key :state [_ {:keys [duratom] :as opts}]
  (let [args (conj duratom :init {:incoming []})]
    (apply duratom/duratom args)))

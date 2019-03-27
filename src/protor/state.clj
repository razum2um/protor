(ns protor.state
  (:require [integrant.core :as ig]
            [duratom.core :as duratom])
  (:import [duratom.core Duratom]))

(defprotocol State
  (add-message [this msg]))

(extend-protocol State
  Duratom
  (add-message [state {:protor.smtp/keys [mime-message raw-data] :as msg}]
    (swap! state update-in [:incoming] conj msg)))


(defmethod ig/init-key :state [_ {:keys [duratom] :as opts}]
  (let [args (conj duratom :init {:incoming []})]
    (apply duratom/duratom args)))

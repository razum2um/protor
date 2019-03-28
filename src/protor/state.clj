(ns protor.state
  (:require [integrant.core :as ig]
            [duratom.core :as duratom]
            [protor.receipt :as receipt])
  (:import [duratom.core Duratom]))

(defn plain-text? [s] (re-find #"text/plain" s))

(defn plain-body [body]
  (first (filter #(-> % :content-type plain-text?) body)))

(defn with-message [state {:keys [body] :as msg}]
  (let [{:keys [id] :as receipt} (-> body plain-body :body receipt/body->receipt)]
    (-> state
        (update-in [:incoming] conj msg)
        (assoc-in [:receipts id] receipt))))

(defprotocol State
  (add-message [this msg]))

(extend-protocol State
  Duratom
  (add-message [state msg]
    (swap! state with-message msg)))


(defmethod ig/init-key :state [_ {:keys [duratom] :as opts}]
  (let [args (conj duratom :init {:incoming [] :receipts {}})]
    (apply duratom/duratom args)))

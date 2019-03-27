(ns protor.smtp
  (:require [integrant.core :as ig])
  (:import [org.subethamail.smtp.helper SimpleMessageListener]))

;; see https://github.com/whilo/bote/blob/master/src/bote/core.clj
(defn- message-listener [accept-fn? message-fn]
  (proxy [SimpleMessageListener] []
    (accept [from to] (accept-fn? from to))

    (deliver [from to data]
      (let [mime-message (javax.mail.internet.MimeMessage.
                          (javax.mail.Session/getDefaultInstance
                           (java.util.Properties.)) data)]
        (message-fn {:from from
                     :to to
                     :subject (.getSubject mime-message)
                     :headers  (->> (.getAllHeaders mime-message)
                                    enumeration-seq
                                    (map (fn [h] [(.getName h)
                                                 (.getValue h)]))
                                    (into {}))
                     :recipients (map str (.getAllRecipients mime-message))
                     :content-type (.getContentType mime-message)
                     :content (.getContent mime-message)
                     :encoding (.getEncoding mime-message)
                     :sent-date (.getSentDate mime-message)
                     ::mime-message mime-message
                     ::raw-data data})))))

(defn create-smtp-server [message-fn {:keys [accept-fn? port enable-tls? require-tls?]
                                      :or {accept-fn? (fn [from to] true)
                                           port 2525}}]
  (let [server
        (org.subethamail.smtp.server.SMTPServer.
         (org.subethamail.smtp.helper.SimpleMessageListenerAdapter.
          (message-listener accept-fn? message-fn)))]
    (when enable-tls? (.setEnableTLS server true))
    (when require-tls? (.setRequireTLS server true))
    (.setPort server port)
    server))

(defmethod ig/init-key :smtp [_ {:keys [state server] :as opts}]
  (let [smtp (create-smtp-server #(swap! state update-in [:incoming] conj %) server)]
    (.start smtp)
    smtp))

(defmethod ig/halt-key! :smtp [_ smtp]
  (.stop smtp))

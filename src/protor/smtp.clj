(ns protor.smtp
  (:require [integrant.core :as ig]
            [protor.state :as state]
            [less.awful.ssl :refer :all])
  (:import [org.subethamail.smtp.helper SimpleMessageListener SimpleMessageListenerAdapter]
           [org.subethamail.smtp.server SMTPServer]
           [javax.net.ssl SSLSocket]
           [java.net InetAddress Socket]))

(defn- smtp-server [handler ssl]
  (let [ssl-ctx (apply ssl-context ssl)]
    (proxy [SMTPServer] [handler]
      (createSSLSocket [^Socket socket]
        (let [^InetAddress addr (.getInetAddress socket)
              port (.getPort socket)
              host (.getHostName addr)
              ^SSLSocket sock (-> ssl-ctx
                                  .getSocketFactory
                                  (.createSocket host port))]
          (let [x (.getSupportedCipherSuites sock)]
            (println {:cipher-suites x})
            (.setEnabledCipherSuites sock x))
          (.setUseClientMode sock false)
          sock)))))

;; see https://github.com/whilo/bote/blob/master/src/bote/core.clj
(defn- message-listener [accept-fn? message-fn]
  (proxy [SimpleMessageListener] []
    (accept [from to] (accept-fn? from to))

    (deliver [from to data]
      (let [mime-message (javax.mail.internet.MimeMessage.
                          (javax.mail.Session/getDefaultInstance
                           (java.util.Properties.)) data)]
        (message-fn (with-meta
                      {:from from
                       :to to
                       :subject (.getSubject mime-message)
                       :headers  (->> (.getAllHeaders mime-message)
                                      enumeration-seq
                                      (map (fn [h] [(.getName h)
                                                    (.getValue h)]))
                                      (into {}))
                       :recipients (map str (.getAllRecipients mime-message))
                       :content-type (.getContentType mime-message)
                       :encoding (.getEncoding mime-message)
                       :sent-date (.getSentDate mime-message)}
                      {::mime-message mime-message
                       ::content (.getContent mime-message)
                       ::raw-data data}))))))

(defn create-smtp-server [message-fn {:keys [accept-fn? port enable-tls? require-tls? host ssl]
                                      :or {accept-fn? (fn [from to] true)
                                           port 2525}}]
  (let [handler (SimpleMessageListenerAdapter. (message-listener accept-fn? message-fn))
        server (if ssl
                 (smtp-server handler ssl)
                 (SMTPServer. handler))]
    (when enable-tls? (.setEnableTLS server true))
    (when require-tls? (.setRequireTLS server true))
    (when host (.setBindAddress server (InetAddress/getByName host)))
    (.setPort server port)
    server))

(defmethod ig/init-key :smtp [_ {:keys [state server] :as opts}]
  (let [smtp (create-smtp-server #(state/add-message state %) server)]
    (.start smtp)
    smtp))

(defmethod ig/halt-key! :smtp [_ smtp]
  (.stop smtp))

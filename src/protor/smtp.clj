(ns protor.smtp
  (:require [integrant.core :as ig]
            [protor.state :as state]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure-mail.message :refer [read-message]]
            [less.awful.ssl :refer :all])
  (:import [org.subethamail.smtp.helper SimpleMessageListener SimpleMessageListenerAdapter]
           [org.subethamail.smtp.server SMTPServer]
           [javax.net.ssl SSLSocket]
           [java.net InetAddress Socket]))

(defn- smtp-server [handler ssl ssl-protocols]
  (let [ssl-ctx (apply ssl-context ssl)]
    (proxy [SMTPServer] [handler]
      (createSSLSocket [^Socket socket]
        (let [^InetAddress addr (.getInetAddress socket)
              port (.getPort socket)
              host (.getHostName addr)
              ^SSLSocket sock (-> ssl-ctx .getSocketFactory
                                  (.createSocket socket host port true))]
          ;; defaults seems ok
          #_(let [supported-ciphers (set (.getSupportedCipherSuites sock))
                ciphers (filter supported-ciphers preferred-ciphers)]
            (log/debug (pr-str {:ciphers (seq ciphers)}))
            (.setEnabledCipherSuites sock ciphers))
          (let [supported-protocols (.getSupportedProtocols sock)
                protocols (if ssl-protocols
                            (into-array (filter (set supported-protocols) ssl-protocols))
                            supported-protocols)]
            (log/debug (pr-str {:enabled-protocols (seq protocols)}))
            (.setEnabledProtocols sock protocols))
          (.setUseClientMode sock false)
          sock)))))

(defn- wrap-coll [x]
  (if (sequential? x) x (list x)))

;; see https://github.com/whilo/bote/blob/master/src/bote/core.clj
(defn- message-listener [accept-fn? message-fn]
  (proxy [SimpleMessageListener] []
    (accept [from to] (accept-fn? from to))

    (deliver [from to data]
      (let [mime-message (javax.mail.internet.MimeMessage.
                          (javax.mail.Session/getDefaultInstance
                           (java.util.Properties.)) data)]
        (-> mime-message
            read-message
            ;; from/to are array, reg. from see RFC 822/A.2.7. Agent for member of a committee
            ;; see current delivery
            (assoc :sender from :receiver to)
            ;; https://github.com/owainlewis/clojure-mail/issues/67
            (update-in [:body] wrap-coll)
            (with-meta {::mime-message mime-message
                        ::raw-data data})
            message-fn)))))

(defn create-smtp-server [message-fn {:keys [accept-fn? port enable-tls? require-tls?
                                             host ssl ssl-protocols]
                                      :or {accept-fn? (fn [from to] true)
                                           port 2525}}]
  (let [handler (SimpleMessageListenerAdapter. (message-listener accept-fn? message-fn))
        server (if ssl
                 (smtp-server handler ssl ssl-protocols)
                 (SMTPServer. handler))]
    (when enable-tls? (.setEnableTLS server true))
    (when require-tls? (.setRequireTLS server true))
    (when host
      (.setBindAddress server (InetAddress/getByName host))
      (.setHostName server host))
    (.setPort server port)
    server))

(defn get-accept-fn [{:keys [host]} _from to]
  (string/ends-with? to (str "@" host)))

(defmethod ig/init-key :smtp [_ {:keys [state server] :as opts}]
  (let [smtp (create-smtp-server #(state/add-message state %)
                                 (assoc server :accept-fn? (partial get-accept-fn server)))]
    (.start smtp)
    smtp))

(defmethod ig/halt-key! :smtp [_ smtp]
  (.stop smtp))

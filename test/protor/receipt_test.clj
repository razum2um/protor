(ns protor.receipt-test
  (:require [clojure.test :refer [deftest is are]]
            [clojure.spec.alpha :as sa] ;; :sa/problems
            [clojure.spec-alpha2 :as s]
            [clojure.spec-alpha2.gen :as gen]
            [clojure.spec-alpha2.test :as test]
            [protor.receipt :as sut]))

(defmacro result-or-ex [x]
  `(try
     ~x
     (catch Throwable t#
       (.getName (class t#)))))

(defn submap?
  "Is m1 a subset of m2?"
  [m1 m2]
  (if (and (map? m1) (map? m2))
    (every? (fn [[k v]] (and (contains? m2 k)
                             (submap? v (get m2 k))))
            m1)
    (= m1 m2)))

  (def forward-body-sample "\r\n\r\n\r\n-------- Forwarded Message --------\r\nDate: \tThu, 28 Mar 2019 00:42:15 +0700\r\nFrom: \tJon Dow <john@example.com>\r\nTo: \thome@example.com\r\n\r\n\r\n{}\r\n{\"x\" : 1}\r\n{\r\n\"document\" : {\r\n\"receipt\" : {\r\n\"operator\" : \"Cashier\",\r\n\"userInn\" : \"222100142924\",\r\n\"rawData\" :\r\n\"AwDvABEEEAA5MjgzNDQwMzAwMDc5ODQ1DQQUADAwMDI2MTAwNDYwNjM1NTAgICAg+gMMADIyMjEwMDE0MjkyNBAEBAA5bQAA9AMEALwjllw1BAYAMQT4sEMaDgQEAOUAAAASBAQAEgAAAB4EAQAB\\/AMCAJJUIwQwAAYEBgCC4aWjrjo3BAIAklT\\/AwIAAAETBAIAklSvBAEABrAEAQAAvAQBAAG+BAEABAcEAgCSVDkEAQAAvwQBAADABAEAAMEEAQAA\\/QMcAISu4K7lrqKgIJGipeKroK2gIICrparhpaWiraAfBAEABLkEAQACUQQCAJJU\",\r\n\"totalSum\" : 21650,\r\n\"items\" : [\r\n{\r\n\"ndsSum\" : 0,\r\n\"quantity\" : 1,\r\n\"productType\" : 1,\r\n\"nds\" : 6,\r\n\"price\" : 21650,\r\n\"paymentType\" : 4,\r\n\"name\" : \"Total:\",\r\n\"sum\" : 21650\r\n}\r\n],\r\n\"ecashTotalSum\" : 0,\r\n\"requestNumber\" : 18,\r\n\"shiftNumber\" : 229,\r\n\"fiscalDriveNumber\" : \"9283440300079845\",\r\n\"ndsNo\" : 21650,\r\n\"provisionSum\" : 0,\r\n\"dateTime\" : \"2019-03-23T12:17:00\",\r\n\"kktRegId\" : \"0002610046063550\",\r\n\"fiscalSign\" : 4172301082,\r\n\"cashTotalSum\" : 21650,\r\n\"prepaidSum\" : 0,\r\n\"receiptCode\" : 3,\r\n\"fiscalDocumentNumber\" : 27961,\r\n\"operationType\" : 1,\r\n\"fiscalDocumentFormatVer\" : 2,\r\n\"creditSum\" : 0,\r\n\"taxationType\" : 4,\r\n\"messageFiscalSign\" : -9149444334024598000\r\n}\r\n}\r\n}\r\n\r\n\r\nSent from my iPhone\r\n)")


(test/instrument `protor.receipt/body->receipt)

(deftest json->receipt
  (let [receipt (sut/body->receipt forward-body-sample)
        item (-> receipt :items first)]

    (is (= item (s/conform ::sut/item item)))
    (is (= nil (s/explain-data ::sut/item item)))

    ;; replaced by test/instrument
    ;; (is (= receipt (s/conform ::sut/receipt receipt)))
    ;; (is (= nil (s/explain-data ::sut/receipt receipt)))

    ;; see clojure/spec-alpha2 src/test/clojure/clojure/test_clojure/spec.clj
    #_(are [spec x conformed ed]
        (let [co (result-or-ex (s/conform spec x))
              e (result-or-ex (::sa/problems (s/explain-data spec x)))]
          (when (not= conformed co) (println "conform fail\n\texpect=" conformed
                                             "\n\tactual=" co))
          (when (not (every? true? (map submap? ed e)))
            (println "explain failures\n\texpect=" ed
                     "\n\tactual failures=" e
                     "\n\tsubmap?=" (map submap? ed e)))
          (and (= conformed co) (every? true? (map submap? ed e))))

      :sut/item item item nil
      )))

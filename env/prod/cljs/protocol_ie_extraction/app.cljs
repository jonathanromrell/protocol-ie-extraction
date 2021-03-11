(ns protocol-ie-extraction.app
  (:require [protocol-ie-extraction.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)

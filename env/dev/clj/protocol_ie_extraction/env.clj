(ns protocol-ie-extraction.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [protocol-ie-extraction.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[protocol-ie-extraction started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[protocol-ie-extraction has shut down successfully]=-"))
   :middleware wrap-dev})

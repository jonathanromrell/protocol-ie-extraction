(ns protocol-ie-extraction.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[protocol-ie-extraction started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[protocol-ie-extraction has shut down successfully]=-"))
   :middleware identity})

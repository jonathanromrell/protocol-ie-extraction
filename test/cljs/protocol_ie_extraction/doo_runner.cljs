(ns protocol-ie-extraction.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [protocol-ie-extraction.core-test]))

(doo-tests 'protocol-ie-extraction.core-test)


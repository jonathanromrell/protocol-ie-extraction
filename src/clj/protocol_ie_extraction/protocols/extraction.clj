(ns protocol-ie-extraction.protocols.extraction
  (:require [clojure.java.io :as io]
            [pdfboxing.text :as text]))

(defn file->plain-text [file]
  (text/extract file))

(comment
  (def TEST_FILE_NAMES ["Bristol Myers Squibb, Plaque Psoriasis.pdf"
                        "Eli Lilly, AMAG, Ulcerative Colitis.pdf"
                        "Eli Lilly, AMAP, Ulcerative Colitis .pdf"
                        "Eli Lilly, KGAK, Atopic Dermatitis.pdf"
                        "Eli Lilly, PYAB, COVID-19.pdf"
                        "Emerald Health, COVID-19.pdf"
                        "Otsuka, 331-201-00072, PTSD.pdf"
                        "Otsuka, 331-201-00242, BPD.pdf"
                        "Sage, 217-PPD-301, PPD.pdf"
                        "Seres, SER-103, RCDI.pdf"
                        "Seres, SER-287, Ulcerative Colitis.pdf"
                        "Takeda, TAK-101-2001, Celiac.pdf"
                        "UCB, Plaque Psoriasis.pdf"
                        "Vanda, Motion Syros, Motion sickness.pdf"
                        "Vanda, Tradipitant, Atopic Dermatitis.pdf"
                        "Verrica, Cove-2, Warts.pdf"])

  (defn read-test-file [filename]
    (->> filename
         (str "protocols/")
         io/resource
         (#(File. (.toURI %)))))
  )
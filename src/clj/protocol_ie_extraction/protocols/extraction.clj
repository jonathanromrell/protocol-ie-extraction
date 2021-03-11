(ns protocol-ie-extraction.protocols.extraction
  (:require [clojure.java.io :as io]
            [pdfboxing.text :as text]
            [pdf-transforms.core :as pdft])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (java.io PrintWriter File ByteArrayOutputStream)
           (org.fit.pdfdom PDFDomTree)))

(defn pdf-file->plain-text [file]
  (text/extract file))

(defn pdf-file->xml-string [file]
  (with-open [pdf (PDDocument/load file)
              bos (ByteArrayOutputStream.)
              out (PrintWriter. bos)]
    (.writeText (PDFDomTree.) pdf out)
    (.toString bos "utf-8")))

(defn- component->text [component]
  (->> component
       :vals
       flatten
       (map :text)
       (clojure.string/join " ")))

(defn- pdf-file->page-heights [file]
  (with-open [doc (PDDocument/load file)]
    (->> doc
         .getNumberOfPages
         range
         (map #(->> %
                    (.getPage doc)
                    .getBBox
                    .getHeight))
         doall)))

(defn pdf-file->components [file]
  (let [components (pdft/transform file)
        absolute-page-coordinates (->> file
                                       pdf-file->page-heights
                                       (reductions +)
                                       vec)]
    (map (fn [{:keys [page-number y0 y1] :as component}]
           (assoc component
             :text (component->text component)
             :absolute-y0 (+' y0 (nth absolute-page-coordinates (dec page-number)))
             :absolute-y1 (+' y1 (nth absolute-page-coordinates (dec page-number)))))
         components)))

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

  (filter (fn [c]
            (->> c :text (re-find #"(?i)inclusion criteria")))
          text-components)

  (filter (fn [c]
            (->> c :page-number #{25 26}))
          text-components)
  )
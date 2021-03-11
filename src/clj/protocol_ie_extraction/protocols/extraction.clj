(ns protocol-ie-extraction.protocols.extraction
  (:require [clojure.java.io :as io]
            [pdfboxing.text :as text]
            [pdf-transforms.core :as pdft]
            [clojure.pprint :as pp])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (java.io PrintWriter File ByteArrayOutputStream)
           (org.fit.pdfdom PDFDomTree)))

(defn do-prn [x]
  (pp/pprint x)
  x)

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
  (let [absolute-page-coordinates (->> file
                                       pdf-file->page-heights
                                       (reductions +)
                                       vec)]
    (->> file
         pdft/transform
         (map (fn [{:keys [page-number y0 y1] :as component}]
                (assoc component
                  :text (component->text component)
                  :absolute-y0 (+' y0 (nth absolute-page-coordinates (dec page-number)))
                  :absolute-y1 (+' y1 (nth absolute-page-coordinates (dec page-number))))))
         (sort-by (juxt :absolute-y0 :x0 :absolute-y1 :x1))
         (map-indexed (fn [i component]
                        (assoc component :index i))))))

(defn component-is-inclusion-label-candidate? [component]
  (->> component
       :text
       (re-find #"(?i)inclusion criteria")))

(defn component-is-exclusion-label-candidate? [component]
  (->> component
       :text
       (re-find #"(?i)exclusion criteria")))

(def NUMBERED_BULLET_REGEX #"[\s\[\(]*([1-9][0-9]*)[\)\.\]]*\s\p{all}+")

(defn component-is-numbered-bullet-candidate? [component]
  (->> component
       :text
       (re-matches NUMBERED_BULLET_REGEX)))

(defn numbered-bullet-component->number [component]
  (->> component
       :text
       (re-matches NUMBERED_BULLET_REGEX)
       second
       clojure.edn/read-string)) ;convert numeric characters into a dynamically sized number type (int, long bigint)

(defn components->numbered-lists [components]
  (loop [remaining-candidates (filter component-is-numbered-bullet-candidate? components)
         in-progress-list []
         lists-found []]
    (if-not (seq remaining-candidates)
      lists-found
      (let [next-candidate (first remaining-candidates)
            next-number-expected (inc (count in-progress-list))
            next-number (numbered-bullet-component->number next-candidate)]
        (if (= next-number next-number-expected)
          (recur (rest remaining-candidates)
                 (conj in-progress-list next-candidate)
                 lists-found)
          (recur (if (seq in-progress-list)
                   remaining-candidates
                   (rest remaining-candidates))
                 []
                 (if (seq in-progress-list)
                   (conj lists-found
                         {:absolute-y0 (-> in-progress-list first :absolute-y0)
                          :start-index (-> in-progress-list first :index)
                          :end-index (-> in-progress-list last :index)
                          :count (count in-progress-list)
                          ;debug
                          :text (->> in-progress-list
                                     (map :text))
                          })
                   lists-found)))))))

(defn candidates->criteria-text [components label-candidates list-candidates]
  (->> label-candidates
       (map (fn [label]
              (some #(when (and (> (:absolute-y0 %) (:absolute-y0 label))
                                (> (:count %) 1))
                       [(select-keys label [:absolute-y0 :text]) %])
                    list-candidates)))
       (keep identity)
       (apply (partial min-key (fn [[label-candidate
                                     nearest-list]]
                                 (- (:absolute-y0 nearest-list)
                                    (:absolute-y0 label-candidate)))))
       ((fn [[label-candidate {:keys [start-index end-index]}]]
          (->> components
               (drop start-index)
               (take (inc (- end-index start-index)))
               (map :text)
               (clojure.string/join "\n"))))))

(defn components->ie-criteria [components]
  (let [numbered-lists (components->numbered-lists components)
        inclusion-label-candidates (filter component-is-inclusion-label-candidate?
                                           components)
        exclusion-label-candidates (filter component-is-exclusion-label-candidate?
                                           components)]
    {:inclusion-criteria (candidates->criteria-text components inclusion-label-candidates numbered-lists)
     :exclusion-criteria (candidates->criteria-text components exclusion-label-candidates numbered-lists)}))

(defn pdf-file->ie-criteria [file]
  (->> file
       pdf-file->components
       components->ie-criteria))

(comment
  (def TEST_FILE_NAMES ["Bristol Myers Squibb, Plaque Psoriasis.pdf"
                        "Sage, 217-PPD-301, PPD.pdf"
                        "Eli Lilly, PYAB, COVID-19.pdf"
                        "Emerald Health, COVID-19.pdf"
                        "Takeda, TAK-101-2001, Celiac.pdf"

                        "Eli Lilly, AMAG, Ulcerative Colitis.pdf"
                        "Eli Lilly, AMAP, Ulcerative Colitis .pdf"
                        "Eli Lilly, KGAK, Atopic Dermatitis.pdf"

                        "Otsuka, 331-201-00072, PTSD.pdf"
                        "Otsuka, 331-201-00242, BPD.pdf"

                        "Seres, SER-103, RCDI.pdf"
                        "Seres, SER-287, Ulcerative Colitis.pdf"
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
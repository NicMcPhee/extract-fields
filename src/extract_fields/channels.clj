(ns extract-fields.channels
  (require [clojure.core.async :as async]
  	   [clojure.java.io :as io]
  	   [me.raynes.fs :as fs]
	   [semantic-csv.core :as sc]
	   [iota])
  (:gen-class))

;Genome diversity (% unique Plush genomes):       1.0
;Syntactic diversity (% unique Push programs):    1.0
;Total error diversity:                           0.166
;Error (vector) diversity:                        0.171
;Behavioral diversity:                            0.171

;Average genome size in population (length): 130.187
;Average program size in population (points): 158.764
;Lexicase best size: 184
;Zero cases best size: 184
;Genome size: 238

(defn add-labels
  [treatment problem filename]
  {:treatment treatment, :problem problem, :filename filename})

(defn label-line [treatment problem file-label generation line]
  {:treatment treatment, :problem problem, 
   :filename file-label, :generation generation,
   :line line})

(def patterns-and-labels
  [
    [#"Genome diversity \(% unique Plush genomes\):\s*(\d+.\d*)", "Genome.Diversity"]
    [#"Syntactic diversity \(% unique Push programs\):\s*(\d+.\d*)", "Syntactic.Diversity"]
    [#"Total error diversity:\s*(\d+.\d*)", "TotalError.Diversity"]
    [#"Error \(vector\) diversity:\s*(\d+.\d*)", "ErrorVector.Diversity"]
    [#"Behavioral diversity:\s*(\d+.\d*)", "Behavioral.Diversity"]
    [#"Average genome size in population \(length\):\s*(\d+.\d*)", "AvgGenome.Size"]
    [#"Average program size in population \(points\):\s*(\d+.\d*)", "AvgProgram.Size"]
    [#"Lexicase best size:\s*(\d+.\d*)", "LexicaseBest.Size"]
    [#"Zero cases best size:\s*(\d+.\d*)", "ZeroCasesBest.Size"]
    [#"Genome size:\s*(\d+.\d*)", "TotalErrorBest.Size"]
  ])

(defn process-line [{:keys [treatment, problem, filename, line, generation]}]
  (loop [patterns-and-labels patterns-and-labels]
    (let [[pattern label] (first patterns-and-labels)]
      (when pattern
        (if-let [m (re-find pattern line)]
          {:treatment treatment, :problem problem,
           :filename filename, :generation generation,
 	   :val-type label, :value (second m)}
	  (recur (rest patterns-and-labels)))))))

(defn file-label [filename]
  (fs/base-name filename true))

; (def *chan-size* 10)

(defn file->entries [treatment problem file-channel-size log-file]
  (let [generation (volatile! nil)
        entries (async/chan file-channel-size (comp (map process-line) (remove nil?)))]
    (async/go
      (with-open [rdr (new java.io.BufferedReader (new java.io.FileReader log-file))] ; (clojure.java.io/reader log-file)]
        (doseq [line (remove nil? (line-seq rdr))]
	  (if-let [gen-match (re-find #"Processing generation:\s*(\d+)" line)]
    	    (vreset! generation (second gen-match))
  	    (async/>! entries (label-line treatment problem (file-label log-file) @generation line))))
	(async/close! entries)))
    entries))

;; (defn files->lines [treatment problem log-file-names]
;;   (async/merge 
;;     (map (partial file->lines treatment problem) log-file-names)
;;     (* 100 (count log-file-names))))

; (def NUM-PIPES 40)

; (def *merged-chan-size* 1)

(defn files->entries [treatment problem log-file-names merge-channel-size file-channel-size]
  (let [entry-channels (map (partial file->entries treatment problem file-channel-size) log-file-names)]
    (async/merge entry-channels merge-channel-size)))

(def headers 
  (clojure.string/join "," 
    ["Treatment", "Problem", "Filename", "Generation", "ValType", "Value"]))

(defn write-entries [output-file entries]
  (with-open [writer (io/writer output-file)]
    (.write writer headers)
    (.newLine writer)
    (loop []
      (when-let [{:keys [treatment, problem, filename, generation, val-type, value]}
           		 (async/<!! entries)]
        (.write writer 
	  (clojure.string/join "," 
	    [treatment, problem, filename, generation, val-type, value]))
	(.newLine writer)
	(recur)))))

(defn process-files
  ([treatment problem output-file input-files]
   (process-files treatment problem output-file input-files 1 1))
  ([treatment problem output-file input-files merge-channel-size]
   (process-files treatment problem output-file input-files merge-channel-size 1))
  ([treatment problem output-file input-files merge-channel-size file-channel-size]
  (let [entries (files->entries treatment problem input-files merge-channel-size file-channel-size)]
    (write-entries output-file entries))))

; (defn channel-process-files [output-file treatment problem log-file-names]
;   (let [entries (files->entries treatment problem log-file-names)]
; (sc/spit-csv output-file {:batch-size 100}
;   (into [] (r/fold 10 r/cat r/append! 
;   	       (r/map (comp (partial into []) (partial process-file treatment problem)) (vec log-file-names))))))

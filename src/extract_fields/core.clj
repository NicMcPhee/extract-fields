(ns extract-fields.core
  (require [clojure.core.reducers :as r]
  	   [me.raynes.fs :as fs]
	   [semantic-csv.core :as sc])
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

(defn add-labels [treatment problem filename]
   {:treatment treatment, :problem problem, :filename filename})

(defn parse-lines [{:keys [treatment, problem, filename]}]
  (let [file-label (fs/base-name filename true)]
    (with-open [rdr (clojure.java.io/reader filename)]
      (doall (for [line (line-seq rdr)]
        {:treatment treatment, :problem problem, 
	 :filename file-label, :line line})))))

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

;;; NEED TO GET GENERATIONS IN HERE SOMEHOW. PROBABLY ATOMS?

(defn process-line [generation {:keys [treatment, problem, filename, line]}]
  (if-let [gen-match (re-find #"Processing generation:\s*(\d+)" line)]
    ((constantly nil) (vreset! generation (second gen-match)))
    (loop [patterns-and-labels patterns-and-labels]
      (let [[pattern label] (first patterns-and-labels)]
        (when pattern
          (if-let [m (re-find pattern line)]
            {:treatment treatment, :problem problem,
  	     :filename filename, :generation @generation,
 	     :val-type label, :value (second m)}
	    (recur (rest patterns-and-labels))))))))

(defn process-file [treatment problem]
  (let [generation (volatile! nil)]
    (comp
      (partial remove nil?)
      (partial map (partial process-line generation))
      parse-lines
      (partial add-labels treatment problem))))

(defn serial-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (mapcat (process-file treatment problem) log-file-names)))

(defn -main
  "Extract various data from log files.
  The first argument is a the name of the desired output file,
  followed by the treatment label and the 
  problem label. The rest are all names of
  log files. This will extract several kinds of diversity
  (genome, syntactic, total error, error vector, and behavioral)
  and size (average genome, average program, lexicase best, 
  zero cases best, total error best) data and save it in
  a columnar file appropriate for loading into something like R."
  [output-file treatment problem & log-file-names]
  (serial-process-files output-file treatment problem log-file-names))
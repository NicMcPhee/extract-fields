(ns extract-fields.reducers
  (require [clojure.core.reducers :as r]
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

(defn label-line [treatment problem file-label line]
  {:treatment treatment, :problem problem, 
   :filename file-label, :line line})

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

(defn file-label [filename]
  (fs/base-name filename true))

(defn process-file [treatment problem log-file]
  (let [generation (volatile! nil)]
    (->> (iota/seq log-file)
    	 (r/filter identity)
    	 (r/map (partial label-line treatment problem (file-label log-file)))
         (r/map (partial process-line generation))
	 (r/remove nil?))))

(defn process-files [treatment problem log-file-names]
  (->> log-file-names
       (r/map (partial add-labels treatment problem))
       ; (r/mapcat file->lines)
       (r/map process-line)
       (r/remove nil?)))

(defn fold-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (into [] (r/fold 10 r/cat r/append! 
    	       (r/map (comp (partial into []) (partial process-file treatment problem)) (vec log-file-names))))))

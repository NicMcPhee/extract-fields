(ns extract-fields.core
  (require [clojure.core.reducers :as r]
  	   [me.raynes.fs :as fs]
	   [semantic-csv.core :as sc]
	   [extract-fields.reducers :as efr])
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

(defn pmap-process-file [treatment problem]
  (let [generation (volatile! nil)]
    (comp
      (partial remove nil?)
      (partial pmap (partial process-line generation))
      parse-lines
      (partial add-labels treatment problem))))

; Took 101,246 msecs to process 100 syllables runs with just add-delete-only
; This seems to get a lot of parallelization in ways I don't fully understand.
(defn serial-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (mapcat (process-file treatment problem) log-file-names)))

; Took 86,296 msecs to process 100 syllables runs with just add-delete-only
; 85% of the time for the "serial" version.
(defn pmap-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (apply concat (pmap (process-file treatment problem) log-file-names))))

; Took 85,105 msecs to process 100 syllables runs with just add-delete-only
; 84% of the time for the "serial" version.
; THIS ALSO ISN'T CORRECT. The volatile generation variable is interacting
; badly with the multiple threads, because each file doesn't get their own.
(defn many-pmap-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (apply concat (pmap (pmap-process-file treatment problem) log-file-names))))

; Took 93,153 msecs to process 100 syllables runs with just add-delete-only
; 92% of the time for the "serial" version.
(defn r-mapcat-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (into [] (r/mapcat (process-file treatment problem) (vec log-file-names)))))

; Took 110,769 msecs to process 100 syllables runs with just add-delete-only
; 109% of the time for the "serial" version.
(defn r-fold-map-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (into [] (r/fold 1 r/cat r/append! 
    	     	     (r/map (process-file treatment problem) (vec log-file-names))))))

; Took 82534 msecs to process 100 syllables runs with just add-delete-only
; 83% of the time for the "serial" version.
; Seems to have an odd "pulsing" behavior where there will be quite a few
; cores active (4-10), and then it will drop back to 1, and then go back
; up to several, and back down, etc.
; A crap-load of work for a not terribly impressive improvement.
(defn r-fold-process-files [output-file treatment problem log-file-names]
  (sc/spit-csv output-file {:batch-size 100}
    (apply concat (into [] (r/fold 1 r/cat
    	     	     (r/monoid (fn [acc item]
		     	         (r/append! acc ((process-file treatment problem) item)))
		     	       (constantly []))
    	     	     (vec log-file-names))))))

(def filenames 
  (for [i (range 100)] 
    ; (str "/home/thelmuth/Results/decay-add-delete/add-delete-only/syllables/log" 
    ; (str "/home/thelmuth/Results/parent-selection-v2/lexicase/syllables/log" 
    (str "/home/thelmuth/Results/decay-add-delete/size-neutral-add-delete/syllables/log"
     i ".txt")))

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

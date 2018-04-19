(defproject extract-fields "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
  		 [org.clojure/core.async "0.4.474"]
  		 [me.raynes/fs "1.4.6"]
		 [semantic-csv "0.2.1-alpha1"]
		 [iota "1.1.3"]]
  :main ^:skip-aot extract-fields.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

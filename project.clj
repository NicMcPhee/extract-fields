(defproject extract-fields "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
  		 [me.raynes/fs "1.4.6"]
		 [semantic-csv "0.2.1-alpha1"]]
  :main ^:skip-aot extract-fields.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

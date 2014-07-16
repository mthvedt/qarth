(defproject qarth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:example {:resource-paths ["test-resources"]
                       :source-paths ["test"]
                       :plugins [[lein-ring "0.8.10"]]}
             :github {:plugins [[codox "0.6.4"]]
                      :codox
                      {:src-dir-uri
                       "http://github.com/eightnotrump/qarth/blob/master",
                       :src-linenum-anchor-prefix "L",
                       :exclude [qarth.support qarth.ring],
                       :output-dir "doc/codox"}}
             :debug {:dependencies [[log4j/log4j "1.2.17"]]}}
  :aliases {"example" ["trampoline" "with-profile" "example" "run" "-m"]
            "exdebug" ["trampoline" "with-profile" "example,debug" "run" "-m"]}
  :dependencies [[org.clojure/clojure "1.5.1"]

                 [org.clojure/data.codec "0.1.0"]
                 [crypto-random "1.2.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-http "0.9.2"]
                 [cheshire "5.3.1"]
                 [org.clojure/data.xml "0.0.7"]
                 
                 [ring/ring-core "1.2.2"]
                 [org.scribe/scribe "1.3.6"]
                 [com.cemerick/friend "0.2.0"]

                 [ring/ring-jetty-adapter "1.2.2" :scope "test"]
                 [compojure "1.1.8" :scope "test"]])

(defproject qarth "0.1.2"
  :description "OAuth for serious people"
  :url "https://github.com/mthvedt/qarth"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :scm {:name "git"
        :url "https://github.com/mthvedt/qarth"}
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
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [org.clojure/data.codec "0.1.0"]
                 [crypto-random "1.2.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-http "2.2.0"]
                 [cheshire "5.6.1"]
                 [org.clojure/data.xml "0.0.7"]
                 
                 [ring/ring-core "1.4.0"]
                 [org.scribe/scribe "1.3.5"]
                 [com.cemerick/friend "0.2.1"]

                 [ring/ring-jetty-adapter "1.4.0" :scope "test"]
                 [compojure "1.5.0" :scope "test"]])

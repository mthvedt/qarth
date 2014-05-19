(defproject qarth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :warn-on-reflection true
  :profiles {:example {:resource-paths ["test-resources"]
                       :source-paths ["test"]
                       :plugins [[lein-ring "0.8.10"]]}}
  :aliases {"example" ["trampoline" "with-profile" "example" "run" "-m"]}
  :dependencies [[org.scribe/scribe "1.3.6"]
                 [com.cemerick/friend "0.2.0"]

                 [ring/ring-core "1.2.2"]

                 [crypto-random "1.2.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/clojure "1.5.1"]

                 [ring/ring-jetty-adapter "1.2.2" :scope "test"]
                 [compojure "1.1.8" :scope "test"]])

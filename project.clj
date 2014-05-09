(defproject qarth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:example {:resource-paths ["test-resources"]
                       :source-paths ["test"]
                       :main qarth.test}}
  :dependencies [[slingshot "0.10.2"]
                 [crypto-random "1.2.0"]

                 [org.scribe/scribe "1.3.2"]

                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/clojure "1.5.1"]])

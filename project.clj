(defproject org.platypope/method-fn "0.3.0-SNAPSHOT"
  :description "A Clojure library augmenting Java methods as functions"
  :url "http://github.com/llasram/method-fn"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :dependencies [[org.clojure/clojure "1.6.0"]])

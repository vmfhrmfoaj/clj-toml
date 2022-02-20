(ns dev.tool
  (:require [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.tools.deps.alpha :as dep.tool]
            [clojure.tools.deps.alpha.util.maven :as dep.mvn])
  (:import clojure.lang.DynamicClassLoader
           java.lang.management.ManagementFactory))

(defn- xml-tag-in
  [xml tag]
  (cond
    (map? xml)
    , (if (= tag (:tag xml))
        xml (xml-tag-in (:content xml) tag))
    (sequential? xml)
    , (->> xml
           (filter coll?)
           (reduce #(when-let [xml (xml-tag-in %2 tag)]
                      (reduced xml))
                   nil))
    :else nil))

(defn- repo []
  (let [mvn-conf-file (io/file (System/getProperty "user.home") ".m2" "settings.xml")
        custom-repos (when (.exists mvn-conf-file)
                       (let [conf (some-> mvn-conf-file (io/reader) (xml/parse))
                             mirrors (some-> conf
                                             (xml-tag-in :settings)
                                             (xml-tag-in :mirrors)
                                             :content)]
                         (some->> mirrors
                                  (filter coll?)
                                  (map (juxt #(some-> % (xml-tag-in :mirrorOf) :content (first))
                                             #(some-> % (xml-tag-in :url)      :content (first))))
                                  (into {}))))]
    (merge dep.mvn/standard-repos custom-repos)))

(defn- dyn-class-loader []
  (->> (.getContextClassLoader (Thread/currentThread))
       (iterate #(.getParent %))
       (take-while #(instance? DynamicClassLoader %))
       (last)))


(defn add-lib
  "Add a library dynamically.

  Example:
   (add-lib [\"org.clojure/core.async\" \"1.3.610\"])"
  [[lib ver] & exclusions]
  ;; NOTE
  ;;  Reference implementation `add-libs` in https://github.com/clojure/tools.deps.alpha/blob/add-lib3/src/main/clojure/clojure/tools/deps/alpha/repl.clj
  (let [loader (dyn-class-loader)
        sym (symbol (cond-> lib
                      (not (namespace (symbol lib)))
                      (str "/" lib)))
        deps {sym {:mvn/version ver :exclusions (conj exclusions 'org.clojure/clojure)}}
        lib (dep.tool/resolve-deps {:deps deps :mvn/repos (repo)} nil)
        paths (->> lib
                   (vals)
                   (map :paths)
                   (flatten))]
    ;; NOTE
    ;;  To support `vmfhrmfoaj/my-lein-utils` dev tool.
    (when-let [thread (some-> 'clojure.core/auto-refresh-thread (find-var) (var-get))]
      (when-not (= loader (.getContextClassLoader thread))
        (.setContextClassLoader thread loader)))
    (->> paths
         (map io/file)
         (map #(.toURL %))
         (run! #(.addURL loader %)))
    paths))

(defn on-reload
  "This function will be called automatically after reloading the namespace if you add `line-dev-helper` to plugins in 'project.clj' file."
  [_tracker]
  ;; NOTE
  ;;  This function is called when the source file is changed.
  ;;  To do this, you need to install the `lein-dev-helper` package.
  ;;  You can manually install the package from the following site:
  ;;  https://gitlab.com/vmfhrmfoaj/lein-dev-helper
  )

(defn get-jvm-args
  "Return JVM arguments." []
  (.getInputArguments (ManagementFactory/getRuntimeMXBean)))


(comment
  (add-lib ["org.clojure/data.xml" "0.0.8"])
  (add-lib ['org.clojure/core.async "1.3.610"])
  (add-lib ["environ" "1.2.0"]))

(defproject com.github.vmfhrmfoaj/clj-toml "1.0.0-0.1"
  :description "TOML for Clojure"
  :url "https://github.com/vmfhrmfoaj/clj-toml"
  :author "Jinseop Kim"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}

  :source-paths ["src"]

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [instaparse "1.4.10"]]

  :profiles
  {:uberjar {:aot :all
             :native-image {:opts ["-Dclojure.compiler.direct-linking=true"]}}
   :dev {:source-paths ["test" "tool"]
         :dependencies [[org.clojure/data.xml "0.0.8"]
                        [org.clojure/tools.deps.alpha "0.12.1109"]]
         ;; You can manually install a package that can handle the following options:
         ;; - https://gitlab.com/vmfhrmfoaj/my-lein-utils
         :auto-refresh ^:replace {:on-reload "dev.tool/on-reload"
                                  :notify-command ["notify-send" "--hint" "int:transient:1" "Reload"]
                                  :verbose false}
         :test-refresh ^:replace {:changes-only true
                                  :timeout-in-sec 5
                                  :notify-command ["notify-send" "--hint" "int:transient:1" "Test"]}}})

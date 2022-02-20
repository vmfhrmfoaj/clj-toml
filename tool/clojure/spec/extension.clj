(ns clojure.spec.extension
  (:require [clojure.spec.alpha :as spec]))

(defn attach-gen!
  "Attatch a generator to a spec.

  Example:
  (attatch-gen ::token #(gen/fmap str (gen/uuid)))"
  [spec gen-fn]
  (and (get (swap! @#'spec/registry-ref (fn [registry]
                                          (cond-> registry
                                            (get registry spec)
                                            (update spec #(spec/with-gen % gen-fn)))))
            spec)
       true))

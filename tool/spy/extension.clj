(ns spy.extension)

(defn any
  "Return an object that is overridden `equal` method to always return `true`." []
  (proxy [java.lang.Object] []
    (equals [_that] true)))

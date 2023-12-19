(ns conduit.infra.hiccup
  (:require
    [hiccup2.core :as h]
    [hiccup.util :as util]))

(defmacro defhtml
  "Define a function, but wrap its output in an implicit [[hiccup.core/html]]
  macro."
  {:clj-kondo/lint-as 'clojure.core/defn}
  [name & fdecl]
  (let [[fhead fbody] (split-with #(not (or (list? %) (vector? %))) fdecl)
        wrap-html     (fn [[args & body]] `(~args (h/html {:mode :html} ~@body)))]
    `(defn ~name
       ~@fhead
       ~@(if (vector? (first fbody))
           (wrap-html fbody)
           (map wrap-html fbody)))))

(defn wrap-attrs
  "Add an optional attribute argument to a function that returns a element
  vector."
  [func]
  (fn [& args]
    (if (map? (first args))
      (let [[tag & body] (apply func (rest args))]
        (if (map? (first body))
          (apply vector tag (merge (first body) (first args)) (rest body))
          (apply vector tag (first args) body)))
      (apply func args))))

(defn- update-arglists [arglists]
  (for [args arglists]
    (vec (cons 'attr-map? args))))

(defmacro defelem
  "Defines a function that will return a element vector. If the first argument
  passed to the resulting function is a map, it merges it with the attribute
  map of the returned element value."
  {:clj-kondo/lint-as :hiccup.def/defelem}
  [name & fdecl]
  `(do (defn ~name ~@fdecl)
       (alter-meta! (var ~name) update-in [:arglists] #'update-arglists)
       (alter-var-root (var ~name) wrap-attrs)))

(defelem link-to
  "Wraps some content in a HTML hyperlink with the supplied URL."
  [url & content]
  [:a {:href (util/to-uri url)} content])
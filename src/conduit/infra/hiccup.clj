(ns conduit.infra.hiccup
  (:require
   [hiccup2.core :as h]
   [hiccup.util :as util]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defmacro defhtml
  "Define a function, but wrap its output in an implicit [[hiccup2.core/html]]
  macro."
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
  [name & fdecl]
  `(do (defn ~name ~@fdecl)
       (alter-meta! (var ~name) update-in [:arglists] #'update-arglists)
       (alter-var-root (var ~name) wrap-attrs)))

(defelem link-to
  "Wraps some content in a HTML hyperlink with the supplied URL.
  (link-to attr-map? url content)
  (link-to \"/foo\" \"Foo\") => [:a {:href \"/foo\"} \"Foo\"]"
  [url & content]
  [:a {:href (util/to-uri url)} content])

(defn hyper
  "Returns an attr-map with a hyper attribute set to the given argument as a hiccup raw string.
  (hyper string attr-map?)
  (hyper \"foo\") => {:_ #object[hiccup.util.RawString \"foo\"]}
  (hyper \"foo\" {:href \"/goofy/goober\"}) =>
    {:_ #object[hiccup.util.RawString 0x47051022 \"foo\"],
     :href \"/goofy/goober\"}"
  ([hyp] (hyper hyp {}))
  ([hyp attr-map]
   (let [hyp-raw (util/raw-string hyp)]
     (merge {:_ hyp-raw} attr-map))))

(comment
  (hyper "foo")
  (hyper "foo" {:href "/goofy/goober"}))

(defn- input-field
  "Creates a new <input> element."
  [type name value]
  [:input {:type  type
           :name  name
           :id    name
           :value value}])

(defelem hidden-field
  "Creates a new <input type=\"hidden\"> element."
  [name value]
  (input-field "hidden" name value))

(defhtml anti-forgery-field
  "Create a hidden field with the session anti-forgery token as its value.
  This ensures that the form it's inside won't be stopped by the anti-forgery
  middleware."
  []
  (hidden-field "__anti-forgery-token" (force *anti-forgery-token*)))

(defhtml htmx-csrf []
  [:script {:type "text/javascript" :async "true"}
   (util/raw-string
    (str "
       document.body.addEventListener('htmx:configRequest', (event) => {
         event.detail.headers['X-CSRF-TOKEN'] = '"
         (force *anti-forgery-token*)
         "'
       })
       "))])
(comment (htmx-csrf))

(ns conduit.utils.dep-macro)

(defn- ->fn-name
  "take the name of a factory and return the name of the function
  (->fn-name '->foo-bar') => 'foo-bar'"
  [name]
  (-> name
      (str)
      (subs 2)
      (symbol)))

(comment
  (->fn-name '->foo-bar))

(defmacro deffact [name & args]
  "takes a deffact function and returns factory function
  (deffact ->foo-bar [deps] [args] (body)) =>
    (defn ->foo-bar [deps]
      (fn foo-bar [args] (body)))"
  (let [fn-name# (->fn-name name)
        docstring (if (string? (first args))
                    (first args)
                    nil)
        deps# (if docstring
                (second args)
                (first args))
        args# (if docstring
                ; if there is a docstring, the args are the third element
                (nth args 2)
                (second args))
        body  (if docstring
                ; if there is a docstring, the body is the everything but the first four elements
                (drop 3 args)
                (drop 2 args))]

    `(defn ~name {:doc ~docstring} ~deps#
       (fn ~fn-name# ~args# ~@body))))


(comment
  (macroexpand-1 '(deffact ->foo-bar [deps] [args] (body)))
  (macroexpand-1 '(deffact ->foo-bar "docstring" [deps]  [args] (body) (body2)))
  ,)

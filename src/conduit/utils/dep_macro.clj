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

(defmacro defact [name & args]
  "takes a function and returns factory function
  (defact name doc-string? [deps] condition-map? [args] body...)
  (deffact ->foo-bar [deps] [args] (body)) =>
    (defn ->foo-bar [deps]
      (fn foo-bar [args] (body)))"
  (let [fn-name# (->fn-name name)
        [docstring & args] (if (string? (first args))
                             args
                             (concat [nil] args))
        [deps# & args] args
        [condition-map# fn-args# & body] (if (map? (first args))
                                           args
                                           (concat [{}] args))]

    `(defn ~name {:doc ~docstring} ~deps# ~condition-map#
       (fn ~fn-name# ~fn-args# ~@body))))

(comment
  (macroexpand-1 '(defact ->foo-bar [deps] [args] (do body)))
  (macroexpand-1 '(defact ->foo-bar "docstring" [deps] [args] (do body) (print "foo")))
  (macroexpand-1 '(defact ->foo-bar "docstring" [deps] {:pre [(deps? deps)]} [args] (do body) (print "foo"))))

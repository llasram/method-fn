(ns method.fn
  "Method function tagged literal constructors."
  (:require [clojure.string :as str]
            [clojure.reflect :as r]))

(def ^:private library
  "Collection of all generated method functions."
  (atom {}))

(defmacro ^:private with-library
  "If a value exists in the library at the key-path `keys`, return
than value.  Otherwise `eval` `body`, retain the result in the
library, and return it."
 [keys body]
  `(let [keys# ~keys]
     @(get-in (swap! library
                     (fn [library#]
                       (if (get-in library# keys#)
                         library#
                         (assoc-in library# keys#
                                   (-> ~body eval delay)))))
              keys#)))

(defn reflection
  "Return source for a function invoking via reflection the unbound
method named `msym`."
  [msym]
  (with-library [:reflection msym]
    `(fn ~(symbol (str "r-" msym))
       ~@(map (fn [n]
                (let [args (repeatedly n gensym)]
                  `([this# ~@args] (. this# ~msym ~@args))))
              (range 20)))))

(defn ^:private arity-set
  "Return the set of arities for the method `msym` of class `cls`."
  ([cls msym] (arity-set (constantly true) cls msym))
  ([f cls msym]
     (->> cls r/reflect :members
          (filter #(and (-> % :name (= msym))
                        (-> % :flags f)))
          (map (comp count :parameter-types))
          set)))

(defn instance
  "Return source for a function invoking the instance method `sym`,
fully-qualified such that `sym`'s namespace indicates the class and
its name indicates the method."
  [sym]
  (if-not (namespace sym)
    (reflection sym)
    (let [csym (-> sym namespace symbol)
          cls (resolve csym)
          csym (symbol (.getName ^Class cls))
          msym (-> sym name symbol)
          fqsym (symbol (str csym) (str msym))
          this (with-meta (gensym 'this) {:tag csym})
          counts (arity-set cls msym)]
      (with-library [:instance fqsym]
        `(fn ~(symbol (str csym "-i-" msym))
           ~@(map (fn [n]
                    (let [args (repeatedly n gensym)]
                      `([~this ~@args] (. ~this ~msym ~@args))))
                  counts))))))
(defn static
  "Return source for a function invoking the static method `sym`,
fully-qualified such that `sym`'s namespace indicates the class and
its name indicates the method."
  [sym]
  (let [csym (-> sym namespace symbol)
        cls (resolve csym)
        csym (symbol (.getName ^Class cls))
        msym (-> sym name symbol)
        fqsym (symbol (str csym) (str msym))
        counts (arity-set :static cls msym)]
    (with-library [:static fqsym]
      `(fn ~(symbol (str csym "-s-" msym))
         ~@(map (fn [n]
                  (let [args (repeatedly n gensym)]
                    `([~@args] (. ~csym ~msym ~@args))))
                counts)))))

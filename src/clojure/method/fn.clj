(ns method.fn
  "Method/constructor function tagged literal constructors."
  (:require [clojure.string :as str]
            [clojure.reflect :as r]))

(def ^:private library
  "Collection of all generated method function classes."
  (atom {}))

(defn ^:private get-or-add
  [library keys f]
  (if (get-in library keys)
    library
    (let [form (delay (->> (f) eval class (list 'new)))]
      (assoc-in library keys form))))

(defn ^:private with-library*
  [keys f] (-> library (swap! get-or-add keys f) (get-in keys) deref))

(defmacro ^:private with-library
  "If a value exists in the library at the key-path `keys`, return
than value.  Otherwise `eval` `body`, retain the result in the
library, and return it."
  [keys & body]
  `(with-library* ~keys (^:once fn* [] ~@body)))

(defn ^:private reflection
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
     (->> cls r/type-reflect :members
          (filter #(let [flags (:flags %)]
                     (and (= msym (:name %))
                          (:public flags)
                          (f flags))))
          (map (comp count :parameter-types))
          set)))

(defn ^:private instance
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

(defmacro i
  "Function invoking the instance method `sym`.  If `sym` is
namespace-qualified, then the namespace is used as the method invocation class.
Otherwise, the function will perform reflection when called to determine the
appropriate method."
  [sym] (instance sym))

(defn ^:private static
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

(defmacro s
  "Function invoking the static method `sym`, fully-qualified such that `sym`'s
namespace indicates the class and its name indicates the method."
  [sym] (static sym))

(defn ^:private constructor
  "Return source for a function invoking the constructors for the class
identified by the symbol `csym`."
  [csym]
  (let [cls (resolve csym)
        csym (symbol (.getName ^Class cls))
        counts (arity-set cls csym)]
    (with-library [:constructor csym]
      `(fn ~(symbol (str csym "-c"))
         ~@(map (fn [n]
                  (let [args (repeatedly n gensym)]
                    `([~@args] (new ~csym ~@args))))
                counts)))))

(defmacro c
  "Function invoking constructors for the class named by the symbol `csym`."
  [csym] (constructor csym))
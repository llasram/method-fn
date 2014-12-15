(ns method.fn
  "Method/constructor function tagged literal constructors."
  (:require [clojure.string :as str]
            [clojure.reflect :as r])
  (:import [java.util.concurrent ConcurrentHashMap]
           [clojure.lang Namespace MethodFnNamespace Var$Unbound]))

(defn ^:private reflection
  "Return source for a function invoking via reflection the unbound
method named `msym`."
  [msym]
  (let [this (gensym 'this)]
    `(fn ~(symbol (str "r|" msym))
       ~@(map (fn [n]
                (let [args (repeatedly n gensym)]
                  `([~this ~@args] (. ~this ~msym ~@args))))
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
          cls ^Class (resolve csym)
          csym (-> cls .getName symbol)
          msym (-> sym name symbol)
          fqsym (symbol (str csym) (str msym))
          this (with-meta (gensym 'this) {:tag csym})
          counts (arity-set cls msym)]
      `(fn ~(symbol (str "i|" csym "|" msym))
         ~@(map (fn [n]
                  (let [args (repeatedly n gensym)]
                    `([~this ~@args] (. ~this ~msym ~@args))))
                counts)))))

(defn ^:private static
  "Return source for a function invoking the static method `sym`,
fully-qualified such that `sym`'s namespace indicates the class and
its name indicates the method."
  [sym]
  (let [csym (-> sym namespace symbol)
        cls ^Class (resolve csym)
        csym (-> cls .getName symbol)
        msym (-> sym name symbol)
        fqsym (symbol (str csym) (str msym))
        counts (arity-set :static cls msym)]
    `(fn ~(symbol (str "s|" csym "|" msym))
       ~@(map (fn [n]
                (let [args (repeatedly n gensym)]
                  `([~@args] (. ~csym ~msym ~@args))))
              counts))))

(defn ^:private constructor
  "Return source for a function invoking the constructors for the class
identified by the symbol `csym`."
  [csym]
  (let [cls ^Class (resolve csym)
        csym (-> cls .getName symbol)
        counts (arity-set cls csym)]
    `(fn ~(symbol (str "c|" csym))
       ~@(map (fn [n]
                (let [args (repeatedly n gensym)]
                  `([~@args] (new ~csym ~@args))))
              counts))))

(def ^:private source-fn
  {"i" instance,
   "s" static,
   "c" constructor,
   })

(defn ^:private function-for
  [f sym]
  (if-not (instance? Var$Unbound f)
    f
    (let [[k & ns] (str/split (name sym) #"\|" 3)
          f (source-fn k), sym (apply symbol ns)]
      (eval (f sym)))))

(def ^:private ^:const magic-ns
  "Namespace name symbol for \"magic\" namespace."
  'method.fn.!magic)

(let [field (doto (.getDeclaredField Namespace "namespaces")
              (.setAccessible true))
      nses ^ConcurrentHashMap (.get field nil)]
  (.put nses magic-ns
        (proxy [MethodFnNamespace] [magic-ns]
          (findInternedVar [sym]
            (let [v (.intern ^Namespace this sym)]
              (alter-var-root v function-for sym)
              v)))))

(defn ^:private mangle
  [kind sym]
  (let [ns (namespace sym), n (name sym)
        sym (if-not ns
              (str kind "|" n)
              (let [cname (.getName ^Class (resolve (symbol ns)))]
                (str kind "|" cname "|" n)))
        ns (name magic-ns)]
    (symbol ns sym)))

(defn ^:private i*
  "Function form of `i`, for data reader."
  [sym] (mangle "i" sym))

(defmacro i
  "Function invoking the instance method `sym`.  If `sym` is
  namespace-qualified, then the namespace is used as the method invocation
  class.  Otherwise, the function will perform reflection when called to
  determine the appropriate method."
  [sym] (i* sym))

(defn ^:private s*
  "Function form of `s`, for data reader."
  [sym] (mangle "s" sym))

(defmacro s
  "Function invoking the static method `sym`, fully-qualified such that
  `sym`'s namespace indicates the class and its name indicates the method."
  [sym] (s* sym))

(defn ^:private c*
  "Function form of `c`, for data reader."
  [sym] (mangle "c" sym))

(defmacro c
  "Function invoking constructors for the class named by the symbol `sym`."
  [sym] (c* sym))

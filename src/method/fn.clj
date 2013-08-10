(ns method.fn
  "Method function tagged literal constructors."
  (:require [clojure.string :as str]
            [clojure.reflect :as r]))

(defn reflection
  [msym]
  (eval
   `(fn ~(symbol (str "r-" msym))
      ~@(map (fn [n]
               (let [args (repeatedly n gensym)]
                 `([this# ~@args] (. this# ~msym ~@args))))
             (range 20)))))

(defn ^:private arity-set
  ([cls msym] (arity-set (constantly true) cls msym))
  ([f cls msym]
     (->> cls r/reflect :members
          (filter #(and (-> % :name (= msym))
                        (-> % :flags f)))
          (map (comp count :parameter-types))
          set)))

(defn instance
  [sym]
  (if-not (namespace sym)
    (reflection sym)
    (let [csym (-> sym namespace symbol)
          cls (resolve csym)
          csym (symbol (.getName ^Class cls))
          msym (-> sym name symbol)
          this (with-meta (gensym 'this) {:tag csym})
          counts (arity-set cls msym)
          fqsym (symbol (str csym) (str msym))]
      (eval
       `(fn ~(symbol (str csym "-i-" msym))
          ~@(map (fn [n]
                   (let [args (repeatedly n gensym)]
                     `([~this ~@args] (. ~this ~msym ~@args))))
                 counts))))))
(defn static
  [sym]
  (let [csym (-> sym namespace symbol)
        cls (resolve csym)
        csym (symbol (.getName ^Class cls))
        msym (-> sym name symbol)
        counts (arity-set :static cls msym)
        fqsym (symbol (str csym) (str msym))]
    (eval
     `(fn ~(symbol (str csym "-s-" msym))
        ~@(map (fn [n]
                 (let [args (repeatedly n gensym)]
                   `([~@args] (. ~csym ~msym ~@args))))
               counts)))))

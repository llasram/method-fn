# method-fn

A Clojure library augmenting Java methods as functions.

## Installation

Method-fn is available on Clojars.  Add this `:dependency` to your Leiningen
`project.clj`:

```clj
[org.platypope/method-fn "0.3.0"]
```

## Usage

Method-fn provides macros and Clojure 1.4+ tagged literals for producing Clojure
functions from symbols representing Java methods.  Even if you are only using
the tagged literal syntax, you must ensure the namespace has been loaded prior
to using the tagged literals:

```clj
(require '[method.fn :as mf])
```

Then method-fn provides several different forms of method-as-function
macros and tagged literals.

Bare instance methods, via reflection:

```clj
(map #mf/i toUpperCase ["foo" "bar"]) ;;=> ("FOO" "BAR")
(map (mf/i toUpperCase) ["foo" "bar"]) ;;=> ("FOO" "BAR")
```

Class-qualified instance methods:

```clj
(map #mf/i String/toUpperCase ["foo" "bar"]) ;;=> ("FOO" "BAR")
(map (mf/i String/toUpperCase) ["foo" "bar"]) ;;=> ("FOO" "BAR")
```

Class-qualified static methods:

```clj
(map #mf/s Math/sqrt [1 4 9]) ;;=> (1.0 2.0 3.0)
(map (mf/s Math/sqrt) [1 4 9]) ;;=> (1.0 2.0 3.0)
```

And class constructors:

```clj
(map #mf/c BigInteger ["1" "2" "3"])
(map (mf/c BigInteger) ["1" "2" "3"])
```

The tagged literal form even works with threading macros:

```clj
(-> x #mf/i String/trim #mf/i String/toUpperCase)
```

## Rationale

Method-fn has the following benefits over Clojure’s anonymous function syntax
and `memfn` macro:

* Same-length or shorter syntax.
* Provides all available method arities.
* Type-hints the invocation target for class-qualified instance methods.
* Generates only one function class per method.
* Tagged literal form works inside of threading macros.

## License

Copyright © 2013-2014 Marshall Bockrath-Vandegrift

Distributed under the Eclipse Public License, the same as Clojure.

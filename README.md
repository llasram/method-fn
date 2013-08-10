# method-fn

A Clojure library augmenting Java methods as functions.

## Installation

Method-fn is available on Clojars.  Add this `:dependency` to your
Leiningen `project.clj`:

```clj
[org.platypope/method-fn "0.1.0"]
```

## Usage

Method-fn provides Clojure 1.4+ tagged literals for producing Clojure
functions from symbols representing Java methods.  First, you must
ensure the namespace has been loaded prior to using the tagged literal
syntax:

```clj
(require 'method.fn)
```

Then method-fn provides several different forms of method-as-function
tagged literals.

Bare instance methods, via reflection:

```clj
(map #mf/i toUpperCase ["foo" "bar"]) ;;=> ("FOO" "BAR")
```

Class-qualified instance methods:

```clj
(map #mf/i String/toUpperCase ["foo" "bar"]) ;;=> ("FOO" "BAR")
```

And class-qualified static methods:

```clj
(map #mf/s Math/sqrt [1 4 9]) ;;=> (1.0 2.0 3.0)
```

## Rationale

Method-fn has the following benefits over Clojure’s anonymous function
syntax and `memfn` macro:

* Same-length or shorter syntax.
* Provides all available method arities.
* Type-hints the invocation target for class-qualified instance
  methods.
* Generates only one function class and instance per method.

## License

Copyright © 2013 Marshall Bockrath-Vandegrift

Distributed under the Eclipse Public License, the same as Clojure.

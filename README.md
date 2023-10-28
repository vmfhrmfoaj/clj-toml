[![Clojars Project](https://img.shields.io/clojars/v/com.github.vmfhrmfoaj/clj-toml.svg)](https://clojars.org/com.github.vmfhrmfoaj/clj-toml)

# clj-toml

`clj-toml` is [TOML](https://toml.io) for Clojure.
TOML is Tom's Obvious, Minimal Language.

> TOML is like INI, only better (Tom Preston-Werner)

`clj-toml` uses [Instaparse](https://github.com/Engelberg/instaparse) for parsing.
Instaparse does all the heavy lifting, we're just sitting pretty.  
`clj-toml` support TOML [v1.0.0](https://toml.io/en/v1.0.0).

If you don't use metadata(i.e. location), [toml-clj](https://github.com/tonsky/toml-clj) is better choice.


## Usage

Add the following line to your leiningen dependencies:
```clojure
[com.github.vmfhrmfoaj/clj-toml "1.0.0-0.1"]
```

Require `clj-toml` in your namespace header:
```clojure
(ns example.core
  (:require [clj-toml.core :as toml]))
```

REPL:
```clojure
#_user> (use 'clj-toml.core)
;;=> nil

#_user> (def example "
title = \"TOML\"\n
[Foo]\n
bar=[1,2,3]")

#_user> (parse-toml example)
;;=> {"title" "TOML", "Foo" {"bar" [1 2 3]}}

#_user> (meta (parse-toml example))
;;=> {"title" #:clj-toml.core{:start 1, :end 15},
;;    "Foo" {:clj-toml.core/start 17,
;;           :clj-toml.core/end 22,
;;           "bar" #:clj-toml.core{:start 24, :end 35}}}

#_user> (subs example 1 15)
;;=> "title = \"TOML\"" 

#_user> (subs example 17 22)
;;=> "[Foo]" 
```

#### Use cases

- [event-handler](https://gitlab.com/vmfhrmfoaj/event-handler/-/blob/main/src/event_handler/config.clj)


### Version

```
x.x.x-z.z < z.z: patch number
^^^^^
x.x.x: TOML version
```


## License

Copyright © 2022 Jinseop Kim.

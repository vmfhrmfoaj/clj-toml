# clj-toml

`clj-toml` is [TOML](https://toml.io) for Clojure.
TOML is Tom's Obvious, Minimal Language.

> TOML is like INI, only better (Tom Preston-Werner)

`clj-toml` uses [Instaparse](https://github.com/Engelberg/instaparse) for parsing.
Instaparse does all the heavy lifting, we're just sitting pretty.  
`clj-toml` support TOML [v1.0.0](https://toml.io/en/v1.0.0).


## Usage

Currently `clj-toml` v1.0.0 has not been released yet.
You can install `clj-toml` locally by using `lein install` command.

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


### Version

```
x.x.x-z.z < patch number
^^^^^
TOML version
```


## License

Copyright © 2022 Jinseop Kim.

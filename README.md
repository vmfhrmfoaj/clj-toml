# clj-toml

clj-toml is [TOML](https://github.com/vmfhrmfoaj/clj-toml) for Clojure. TOML is Tom's Obvious, Minimal Language. 

> TOML is like INI, only better (Tom Preston-Werner)

clj-toml uses [Instaparse](https://github.com/Engelberg/instaparse) for parsing.
Instaparse does all the heavy lifting, we're just sitting pretty.  
Currently clj-toml support TOML [v1.0.0](https://toml.io/en/v1.0.0).

## Usage

Work is underway towards a TOML 0.4.0-compliant release. Use the 0.3.1 release (TOML 0.1.0-compliant) in the meantime:

Leiningen:
  ```clojure
  [clj-toml "1.0.0"]
  ```

Test:
  ```clojure
  lein test
  ```

Use:
  ```clojure
  (use 'clj-toml.core)

  (parse-toml "
   title = \"TOML\"
   [Foo]
   bar=[1,2,3]")
  ;; {"title" "TOML" "foo" {"bar" [1 2 3]}}

  (meta (parse-toml "
   title = \"TOML\"
   [Foo]
   bar=[1,2,3]"))
  ;; {...}
  ```

## License

Copyright © 2022 Jinseop Kim.

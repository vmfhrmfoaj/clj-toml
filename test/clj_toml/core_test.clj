(ns clj-toml.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-toml.core :as target])
  (:import java.time.OffsetDateTime
           java.time.LocalDate
           java.time.LocalDateTime
           java.time.LocalTime))

(defmacro ^{:private true
            :style/indent 0}
  multi-line
  [& lines]
  (str/join "\n" lines))


(deftest key-value-pair-test
  (testing "Basic key/value pair"
    (let [src "key = \"value\""
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"key" "value"} res))
      (is (= "key = \"value\""
             (subs src
                   (get-in md ["key" ::target/start])
                   (get-in md ["key" ::target/end])))))

    (let [src "key  =  \"value\""
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"key" "value"} res))
      (is (= "key  =  \"value\""
             (subs src
                   (get-in md ["key" ::target/start])
                   (get-in md ["key" ::target/end])))))))

(deftest comment-tset
  (let [src "# comment"
        res (target/parse-toml src)]
    (is (nil? res)))

  (let [src "key = \"value\" # comment"
        res (target/parse-toml src)
        md (meta res)]
    (is (= {"key" "value"} res))
    (is (= "key = \"value\""
           (subs src
                 (get-in md ["key" ::target/start])
                 (get-in md ["key" ::target/end])))))

  (let [src (multi-line
              "# begin"
              "key = \"value\""
              "# end")
        res (target/parse-toml src)
        md (meta res)]
    (is (= {"key" "value"} res))
    (is (= "key = \"value\""
           (subs src
                 (get-in md ["key" ::target/start])
                 (get-in md ["key" ::target/end]))))))

(deftest key-test
  (testing "Bare key"
    (let [src (multi-line
                "key = \"value\""
                "bare_key = \"value\""
                "bare-key = \"value\""
                "1234 = \"value\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"key" "value"
              "bare_key" "value"
              "bare-key" "value"
              "1234" "value"}
             res))
      (is (= "key = \"value\""
             (subs src
                   (get-in md ["key" ::target/start])
                   (get-in md ["key" ::target/end]))))
      (is (= "bare_key = \"value\""
             (subs src
                   (get-in md ["bare_key" ::target/start])
                   (get-in md ["bare_key" ::target/end]))))
      (is (= "bare-key = \"value\""
             (subs src
                   (get-in md ["bare-key" ::target/start])
                   (get-in md ["bare-key" ::target/end]))))
      (is (= "1234 = \"value\""
             (subs src
                   (get-in md ["1234" ::target/start])
                   (get-in md ["1234" ::target/end]))))))

  (testing "Quoted key"
    (let [src (multi-line
                "\"127.0.0.1\" = \"value\""
                "\"character encoding\" = \"value\""
                "\"ʎǝʞ\" = \"value\""
                "'key2' = \"value\""
                "'quoted \"value\"' = \"value\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"127.0.0.1" "value"
              "character encoding" "value"
              "ʎǝʞ" "value"
              "key2" "value"
              "quoted \"value\"" "value"}
             res))
      (is (= "\"127.0.0.1\" = \"value\""
             (subs src
                   (get-in md ["127.0.0.1" ::target/start])
                   (get-in md ["127.0.0.1" ::target/end]))))
      (is (= "\"character encoding\" = \"value\""
             (subs src
                   (get-in md ["character encoding" ::target/start])
                   (get-in md ["character encoding" ::target/end]))))
      (is (= "\"ʎǝʞ\" = \"value\""
             (subs src
                   (get-in md ["ʎǝʞ" ::target/start])
                   (get-in md ["ʎǝʞ" ::target/end]))))
      (is (= "'key2' = \"value\""
             (subs src
                   (get-in md ["key2" ::target/start])
                   (get-in md ["key2" ::target/end]))))
      (is (= "'quoted \"value\"' = \"value\""
             (subs src
                   (get-in md ["quoted \"value\"" ::target/start])
                   (get-in md ["quoted \"value\"" ::target/end]))))))

  (testing "Dotted key"
    (let [src (multi-line
                "name = \"Orange\""
                "physical.color = \"orange\""
                "physical.shape = \"round\""
                "site.\"google.com\" = true")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"name" "Orange"
              "physical" {"color" "orange"
                          "shape" "round"}
              "site" {"google.com" true}}
             res))
      (is (= "name = \"Orange\""
             (subs src
                   (get-in md ["name" ::target/start])
                   (get-in md ["name" ::target/end]))))
      (is (= "physical.color = \"orange\""
             (subs src
                   (get-in md ["physical" "color" ::target/start])
                   (get-in md ["physical" "color" ::target/end]))))
      (is (= "physical.shape = \"round\""
             (subs src
                   (get-in md ["physical" "shape" ::target/start])
                   (get-in md ["physical" "shape" ::target/end]))))
      (is (= "site.\"google.com\" = true"
             (subs src
                   (get-in md ["site" "google.com" ::target/start])
                   (get-in md ["site" "google.com" ::target/end])))))

    (let [src (multi-line
                "fruit.name = \"banana\""
                "fruit. color = \"yellow\""
                "fruit . flavor = \"banana\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"fruit" {"name" "banana"
                       "color" "yellow"
                       "flavor" "banana"}}
             res)
          "allow the space around dot(.)")
      (is (= "fruit.name = \"banana\""
             (subs src
                   (get-in md ["fruit" "name" ::target/start])
                   (get-in md ["fruit" "name" ::target/end]))))
      (is (= "fruit. color = \"yellow\""
             (subs src
                   (get-in md ["fruit" "color" ::target/start])
                   (get-in md ["fruit" "color" ::target/end]))))
      (is (= "fruit . flavor = \"banana\""
             (subs src
                   (get-in md ["fruit" "flavor" ::target/start])
                   (get-in md ["fruit" "flavor" ::target/end])))))

    (let [src (multi-line
                "fruit.apple.smooth = true"
                "fruit.orange = 2")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"fruit" {"apple" {"smooth" true}
                       "orange" 2}}
             res))
      (is (= "fruit.apple.smooth = true"
             (subs src
                   (get-in md ["fruit" "apple" "smooth" ::target/start])
                   (get-in md ["fruit" "apple" "smooth" ::target/end]))))
      (is (= "fruit.orange = 2"
             (subs src
                   (get-in md ["fruit" "orange" ::target/start])
                   (get-in md ["fruit" "orange" ::target/end])))))

    (let [src "3.14159 = \"pi\""
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"3" {"14159" "pi"}} res))
      (is (= "3.14159 = \"pi\""
             (subs src
                   (get-in md ["3" "14159" ::target/start])
                   (get-in md ["3" "14159" ::target/end])))))))

(deftest string-test
  (testing "Basic string"
    (let [src "str = \"I'm a string. \\\"You can quote me\\\". Name\\tJos\\u00E9\\nLocation\\tSF.\""
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"str" "I'm a string. \"You can quote me\". Name\tJos\u00E9\nLocation\tSF."} res))
      (is (= "str = \"I'm a string. \\\"You can quote me\\\". Name\\tJos\\u00E9\\nLocation\\tSF.\""
             (subs src
                   (get-in md ["str" ::target/start])
                   (get-in md ["str" ::target/end]))))))

  (testing "Multi-line basic string"
    (let [src (multi-line
                "str1 = \"\"\""
                "Roses are red"
                "Violets are blue\"\"\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"str1" "Roses are red\nViolets are blue"} res))
      (is (= (multi-line
               "str1 = \"\"\""
               "Roses are red"
               "Violets are blue\"\"\"")
             (subs src
                   (get-in md ["str1" ::target/start])
                   (get-in md ["str1" ::target/end])))))

    (let [src (multi-line
                "str1 = \"\"\""
                "Roses are red"
                ""
                "  Violets are blue\"\"\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"str1" "Roses are red\n\n  Violets are blue"} res))
      (is (= src (subs src
                       (get-in md ["str1" ::target/start])
                       (get-in md ["str1" ::target/end])))))

    (let [src (multi-line
                "str1 = \"The quick brown fox jumps over the lazy dog.\""
                ""
                "str2 = \"\"\""
                "The quick brown \\"
                ""
                ""
                "  fox jumps over \\"
                "    the lazy dog.\"\"\""
                ""
                "str3 = \"\"\"\\"
                "       The quick brown \\"
                "       fox jumps over \\"
                "       the lazy dog.\\"
                "       \"\"\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"str1" "The quick brown fox jumps over the lazy dog."
              "str2" "The quick brown fox jumps over the lazy dog."
              "str3" "The quick brown fox jumps over the lazy dog."}
             res))
      (is (= "str1 = \"The quick brown fox jumps over the lazy dog.\""
             (subs src
                   (get-in md ["str1" ::target/start])
                   (get-in md ["str1" ::target/end]))))
      (is (= (multi-line
               "str2 = \"\"\""
               "The quick brown \\"
               ""
               ""
               "  fox jumps over \\"
               "    the lazy dog.\"\"\"")
             (subs src
                   (get-in md ["str2" ::target/start])
                   (get-in md ["str2" ::target/end]))))
      (is (= (multi-line
               "str3 = \"\"\"\\"
               "       The quick brown \\"
               "       fox jumps over \\"
               "       the lazy dog.\\"
               "       \"\"\"")
             (subs src
                   (get-in md ["str3" ::target/start])
                   (get-in md ["str3" ::target/end])))))

    (let [src (multi-line
                "str4 = \"\"\"Here are two quotation marks: \"\". Simple enough.\"\"\""
                "str5 = \"\"\"Here are three quotation marks: \"\"\\\".\"\"\""
                "str6 = \"\"\"Here are fifteen quotation marks: \"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\".\"\"\""
                "str7 = \"\"\"\"This,\" she said, \"is just a pointless statement.\"\"\"\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"str4" "Here are two quotation marks: \"\". Simple enough."
              "str5" "Here are three quotation marks: \"\"\"."
              "str6" "Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"."
              "str7" "\"This,\" she said, \"is just a pointless statement.\""}
             res))
      (is (= "str4 = \"\"\"Here are two quotation marks: \"\". Simple enough.\"\"\""
             (subs src
                   (get-in md ["str4" ::target/start])
                   (get-in md ["str4" ::target/end]))))
      (is (= "str5 = \"\"\"Here are three quotation marks: \"\"\\\".\"\"\""
             (subs src
                   (get-in md ["str5" ::target/start])
                   (get-in md ["str5" ::target/end]))))
      (is (= "str6 = \"\"\"Here are fifteen quotation marks: \"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\".\"\"\""
             (subs src
                   (get-in md ["str6" ::target/start])
                   (get-in md ["str6" ::target/end]))))
      (is (= "str7 = \"\"\"\"This,\" she said, \"is just a pointless statement.\"\"\"\""
             (subs src
                   (get-in md ["str7" ::target/start])
                   (get-in md ["str7" ::target/end]))))))

  (testing "Literal string"
    (let [src (multi-line
                "winpath  = 'C:\\Users\\nodejs\\templates'"
                "winpath2 = '\\\\ServerX\\admin$\\system32\\'"
                "quoted   = 'Tom \"Dubs\" Preston-Werner'"
                "regex    = '<\\i\\c*\\s*>'")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"winpath" "C:\\Users\\nodejs\\templates"
              "winpath2" "\\\\ServerX\\admin$\\system32\\"
              "quoted" "Tom \"Dubs\" Preston-Werner"
              "regex" "<\\i\\c*\\s*>"}
             res))
      (is (= "winpath  = 'C:\\Users\\nodejs\\templates'"
             (subs src
                   (get-in md ["winpath" ::target/start])
                   (get-in md ["winpath" ::target/end]))))
      (is (= "winpath2 = '\\\\ServerX\\admin$\\system32\\'"
             (subs src
                   (get-in md ["winpath2" ::target/start])
                   (get-in md ["winpath2" ::target/end]))))
      (is (= "quoted   = 'Tom \"Dubs\" Preston-Werner'"
             (subs src
                   (get-in md ["quoted" ::target/start])
                   (get-in md ["quoted" ::target/end]))))
      (is (= "regex    = '<\\i\\c*\\s*>'"
             (subs src
                   (get-in md ["regex" ::target/start])
                   (get-in md ["regex" ::target/end]))))))

  (testing "Multi-line literal string"
    (let [src (multi-line
                "regex2 = '''I [dw]on't need \\d{2} apples'''"
                "lines  = '''"
                "The first newline is"
                "trimmed in raw strings."
                "   All other whitespace"
                "   is preserved."
                "'''")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"regex2" "I [dw]on't need \\d{2} apples"
              "lines" (multi-line "The first newline is"
                                  "trimmed in raw strings."
                                  "   All other whitespace"
                                  "   is preserved.")}
             res))
      (is (= "regex2 = '''I [dw]on't need \\d{2} apples'''"
             (subs src
                   (get-in md ["regex2" ::target/start])
                   (get-in md ["regex2" ::target/end]))))
      (is (= (multi-line
               "lines  = '''"
               "The first newline is"
               "trimmed in raw strings."
               "   All other whitespace"
               "   is preserved."
               "'''")
             (subs src
                   (get-in md ["lines" ::target/start])
                   (get-in md ["lines" ::target/end])))))

    (let [src (multi-line
                "quot15 = '''Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"'''"
                "apos15 = \"Here are fifteen apostrophes: '''''''''''''''\""
                "str = ''''That,' she said, 'is still pointless.''''")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"quot15" "Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\""
              "apos15" "Here are fifteen apostrophes: '''''''''''''''"
              "str" "'That,' she said, 'is still pointless.'"}
             res))
      (is (= "quot15 = '''Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"'''"
             (subs src
                   (get-in md ["quot15" ::target/start])
                   (get-in md ["quot15" ::target/end]))))
      (is (= "apos15 = \"Here are fifteen apostrophes: '''''''''''''''\""
             (subs src
                   (get-in md ["apos15" ::target/start])
                   (get-in md ["apos15" ::target/end]))))
      (is (= "str = ''''That,' she said, 'is still pointless.''''"
             (subs src
                   (get-in md ["str" ::target/start])
                   (get-in md ["str" ::target/end])))))))

(deftest integer-test
  (testing "Basic integer"
    (let [src (multi-line
                "int1 = +99"
                "int2 = 42"
                "int3 = 0"
                "int4 = -17")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"int1" 99
              "int2" 42
              "int3" 0
              "int4" -17}
             res))
      (is (= "int1 = +99"
             (subs src
                   (get-in md ["int1" ::target/start])
                   (get-in md ["int1" ::target/end]))))
      (is (= "int2 = 42"
             (subs src
                   (get-in md ["int2" ::target/start])
                   (get-in md ["int2" ::target/end]))))
      (is (= "int3 = 0"
             (subs src
                   (get-in md ["int3" ::target/start])
                   (get-in md ["int3" ::target/end]))))
      (is (= "int4 = -17"
             (subs src
                   (get-in md ["int4" ::target/start])
                   (get-in md ["int4" ::target/end])))))

    (let [src (multi-line
                "int5 = 1_000"
                "int6 = 5_349_221")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"int5" 1000
              "int6" 5349221}
             res)
          "underscore(_) can be used as the seperator to enhance readability")
      (is (= "int5 = 1_000"
             (subs src
                   (get-in md ["int5" ::target/start])
                   (get-in md ["int5" ::target/end]))))
      (is (= "int6 = 5_349_221"
             (subs src
                   (get-in md ["int6" ::target/start])
                   (get-in md ["int6" ::target/end]))))))

  (testing "Hex, Octor and binary"
    (let [src (multi-line
                "hex1 = 0xDEADBEEF"
                "hex2 = 0xdeadbeef"
                "hex3 = 0xdead_beef"
                "oct1 = 0o01234567"
                "oct2 = 0o755"
                "bin1 = 0b11010110")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"hex1" 0xDEADBEEF
              "hex2" 0xdeadbeef
              "hex3" 0xdeadbeef
              "oct1" 01234567
              "oct2" 0755
              "bin1" 2r11010110}
             res))
      (is (= "hex1 = 0xDEADBEEF"
             (subs src
                   (get-in md ["hex1" ::target/start])
                   (get-in md ["hex1" ::target/end]))))
      (is (= "hex2 = 0xdeadbeef"
             (subs src
                   (get-in md ["hex2" ::target/start])
                   (get-in md ["hex2" ::target/end]))))
      (is (= "hex3 = 0xdead_beef"
             (subs src
                   (get-in md ["hex3" ::target/start])
                   (get-in md ["hex3" ::target/end]))))
      (is (= "oct1 = 0o01234567"
             (subs src
                   (get-in md ["oct1" ::target/start])
                   (get-in md ["oct1" ::target/end]))))
      (is (= "oct2 = 0o755"
             (subs src
                   (get-in md ["oct2" ::target/start])
                   (get-in md ["oct2" ::target/end]))))
      (is (= "bin1 = 0b11010110"
             (subs src
                   (get-in md ["bin1" ::target/start])
                   (get-in md ["bin1" ::target/end])))))))

(deftest float-test
  (testing "Default float"
    (let [src (multi-line
                "flt1 = +1.0"
                "flt2 = 3.1415"
                "flt3 = -0.01"
                "flt4 = 5e+22"
                "flt5 = 1e06"
                "flt6 = -2E-2"
                "flt7 = 6.626e-34")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"flt1" 1.0
              "flt2" 3.1415
              "flt3" -0.01
              "flt4" 5e+22
              "flt5" 1e06
              "flt6" -2E-2
              "flt7" 6.626e-34}
             res))
      (is (= "flt1 = +1.0"
             (subs src
                   (get-in md ["flt1" ::target/start])
                   (get-in md ["flt1" ::target/end]))))
      (is (= "flt2 = 3.1415"
             (subs src
                   (get-in md ["flt2" ::target/start])
                   (get-in md ["flt2" ::target/end]))))
      (is (= "flt3 = -0.01"
             (subs src
                   (get-in md ["flt3" ::target/start])
                   (get-in md ["flt3" ::target/end]))))
      (is (= "flt4 = 5e+22"
             (subs src
                   (get-in md ["flt4" ::target/start])
                   (get-in md ["flt4" ::target/end]))))
      (is (= "flt5 = 1e06"
             (subs src
                   (get-in md ["flt5" ::target/start])
                   (get-in md ["flt5" ::target/end]))))
      (is (= "flt6 = -2E-2"
             (subs src
                   (get-in md ["flt6" ::target/start])
                   (get-in md ["flt6" ::target/end]))))
      (is (= "flt7 = 6.626e-34"
             (subs src
                   (get-in md ["flt7" ::target/start])
                   (get-in md ["flt7" ::target/end])))))

    ;; NOTE
    ;;  exclude tests for NaN, because result of `(= ##NaN ##NaN)` is flase
    (let [src (multi-line
                "sf1 = inf"
                "sf2 = +inf"
                "sf3 = -inf"
                "#sf4 = nan"
                "#sf5 = +nan"
                "#sf6 = -nan")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"sf1" ##Inf
              "sf2" ##Inf
              "sf3" ##-Inf
              ;; "sf4" ##NaN
              ;; "sf5" ##NaN
              ;; "sf6" ##NaN
              }
             res))
      (is (= "sf1 = inf"
             (subs src
                   (get-in md ["sf1" ::target/start])
                   (get-in md ["sf1" ::target/end]))))
      (is (= "sf2 = +inf"
             (subs src
                   (get-in md ["sf2" ::target/start])
                   (get-in md ["sf2" ::target/end]))))
      (is (= "sf3 = -inf"
             (subs src
                   (get-in md ["sf3" ::target/start])
                   (get-in md ["sf3" ::target/end])))))))

(deftest boolean-test
  (testing "Default boolean"
    (let [src (multi-line
                "bool1 = true"
                "bool2 = false")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"bool1" true
              "bool2" false}
             res))
      (is (= "bool1 = true"
             (subs src
                   (get-in md ["bool1" ::target/start])
                   (get-in md ["bool1" ::target/end]))))
      (is (= "bool2 = false"
             (subs src
                   (get-in md ["bool2" ::target/start])
                   (get-in md ["bool2" ::target/end])))))))

(deftest datetime-test
  (testing "Offset date-time"
    (let [src (multi-line
                "odt1 = 1979-05-27T07:32:00Z"
                "odt2 = 1979-05-27T00:32:00-07:00"
                "odt3 = 1979-05-27T00:32:00.999999-07:00")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"odt1" (OffsetDateTime/parse "1979-05-27T07:32:00Z")
              "odt2" (OffsetDateTime/parse "1979-05-27T00:32:00-07:00")
              "odt3" (OffsetDateTime/parse "1979-05-27T00:32:00.999999-07:00")}
             res))
      (is (= "odt1 = 1979-05-27T07:32:00Z"
             (subs src
                   (get-in md ["odt1" ::target/start])
                   (get-in md ["odt1" ::target/end]))))
      (is (= "odt2 = 1979-05-27T00:32:00-07:00"
             (subs src
                   (get-in md ["odt2" ::target/start])
                   (get-in md ["odt2" ::target/end]))))
      (is (= "odt3 = 1979-05-27T00:32:00.999999-07:00"
             (subs src
                   (get-in md ["odt3" ::target/start])
                   (get-in md ["odt3" ::target/end])))))

    (let [src "odt4 = 1979-05-27 07:32:00Z"
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"odt4" (OffsetDateTime/parse "1979-05-27T07:32:00Z")} res))
      (is (= "odt4 = 1979-05-27 07:32:00Z"
             (subs src
                   (get-in md ["odt4" ::target/start])
                   (get-in md ["odt4" ::target/end]))))))

  (testing "Local date-time"
    (let [src (multi-line
                "ldt1 = 1979-05-27T07:32:00"
                "ldt2 = 1979-05-27T00:32:00.999999")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"ldt1" (LocalDateTime/parse "1979-05-27T07:32:00")
              "ldt2" (LocalDateTime/parse "1979-05-27T00:32:00.999999")}
             res))
      (is (= "ldt1 = 1979-05-27T07:32:00"
             (subs src
                   (get-in md ["ldt1" ::target/start])
                   (get-in md ["ldt1" ::target/end]))))
      (is (= "ldt2 = 1979-05-27T00:32:00.999999"
             (subs src
                   (get-in md ["ldt2" ::target/start])
                   (get-in md ["ldt2" ::target/end]))))))

  (testing "Local date"
    (is (= {"ld1" (LocalDate/parse "1979-05-27")} (target/parse-toml "ld1 = 1979-05-27"))))

  (testing "Local time"
    (is (= {"lt1" (LocalTime/parse "07:32:00")
            "lt2" (LocalTime/parse "00:32:00.999999")}
           (target/parse-toml (multi-line
                                "lt1 = 07:32:00"
                                "lt2 = 00:32:00.999999"))))))

(deftest array-test
  (testing "Basic array"
    (let [src (multi-line
                "integers = ["
                "  1,"
                "  2,"
                "  3 ]")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"integers" [1 2 3]} res))
      (is (= (multi-line
               "integers = ["
               "  1,"
               "  2,"
               "  3 ]")
             (subs src
                   (get-in md ["integers" ::target/start])
                   (get-in md ["integers" ::target/end])))))

    (let [src (multi-line
                "integers = [ 1, 2, 3 ]"
                "colors = [ \"red\", \"yellow\", \"green\" ]"
                "nested_arrays_of_ints = [ [ 1, 2 ], [3, 4, 5] ]"
                "nested_mixed_array = [ [ 1, 2 ], [\"a\", \"b\", \"c\"] ]"
                "string_array = [ \"all\", 'strings', \"\"\"are the same\"\"\", '''type''' ]")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"integers" [1 2 3]
              "colors" ["red" "yellow" "green"]
              "nested_arrays_of_ints" [[1 2] [3 4 5]]
              "nested_mixed_array" [[1 2] ["a" "b" "c"]]
              "string_array" ["all" "strings" "are the same" "type"]}
             res))
      (is (= "integers = [ 1, 2, 3 ]"
             (subs src
                   (get-in md ["integers" ::target/start])
                   (get-in md ["integers" ::target/end]))))
      (is (= "colors = [ \"red\", \"yellow\", \"green\" ]"
             (subs src
                   (get-in md ["colors" ::target/start])
                   (get-in md ["colors" ::target/end]))))
      (is (= "nested_arrays_of_ints = [ [ 1, 2 ], [3, 4, 5] ]"
             (subs src
                   (get-in md ["nested_arrays_of_ints" ::target/start])
                   (get-in md ["nested_arrays_of_ints" ::target/end]))))
      (is (= "nested_mixed_array = [ [ 1, 2 ], [\"a\", \"b\", \"c\"] ]"
             (subs src
                   (get-in md ["nested_mixed_array" ::target/start])
                   (get-in md ["nested_mixed_array" ::target/end]))))
      (is (= "string_array = [ \"all\", 'strings', \"\"\"are the same\"\"\", '''type''' ]"
             (subs src
                   (get-in md ["string_array" ::target/start])
                   (get-in md ["string_array" ::target/end])))))

    (let [src (multi-line
                "integers2 = ["
                "  1, 2, 3"
                "]"
                ""
                "integers3 = ["
                "  1,"
                "  2,"
                "]")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"integers2" [1 2 3]
              "integers3" [1 2]}
             res))
      (is (= (multi-line
               "integers2 = ["
               "  1, 2, 3"
               "]")
             (subs src
                   (get-in md ["integers2" ::target/start])
                   (get-in md ["integers2" ::target/end]))))
      (is (= (multi-line
               "integers3 = ["
               "  1,"
               "  2,"
               "]")
             (subs src
                   (get-in md ["integers3" ::target/start])
                   (get-in md ["integers3" ::target/end])))))))

(deftest table-test
  (testing "Basic table"
    (let [src (multi-line
                "[table-1]"
                "key1 = \"some string\""
                "key2 = 123"
                ""
                "[table-2]"
                "key1 = \"another string\""
                "key2 = 456")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"table-1" {"key1" "some string"
                         "key2" 123}
              "table-2" {"key1" "another string"
                         "key2" 456}}
             res))
      (is (= "[table-1]"
             (subs src
                   (get-in md ["table-1" ::target/start])
                   (get-in md ["table-1" ::target/end]))))
      (is (= "key1 = \"some string\""
             (subs src
                   (get-in md ["table-1" "key1" ::target/start])
                   (get-in md ["table-1" "key1" ::target/end]))))
      (is (= "key2 = 123"
             (subs src
                   (get-in md ["table-1" "key2" ::target/start])
                   (get-in md ["table-1" "key2" ::target/end]))))
      (is (= "[table-2]"
             (subs src
                   (get-in md ["table-2" ::target/start])
                   (get-in md ["table-2" ::target/end]))))
      (is (= "key1 = \"another string\""
             (subs src
                   (get-in md ["table-2" "key1" ::target/start])
                   (get-in md ["table-2" "key1" ::target/end]))))
      (is (= "key2 = 456"
             (subs src
                   (get-in md ["table-2" "key2" ::target/start])
                   (get-in md ["table-2" "key2" ::target/end])))))

    (let [src (multi-line
                "[table]"
                "key1 = \"some string\""
                "key2 = 123"
                ""
                "[table.lv-1]"
                "key1 = \"another string\""
                "key2 = 456"
                ""
                "[table.lv-1.lv-2]"
                "key1 = \"another string, again\""
                "key2 = 789")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"table" {"key1" "some string"
                       "key2" 123
                       "lv-1" {"key1" "another string"
                               "key2" 456
                               "lv-2" {"key1" "another string, again"
                                       "key2" 789}}}}
             res))
      (is (= "[table]"
             (subs src
                   (get-in md ["table" ::target/start])
                   (get-in md ["table" ::target/end]))))
      (is (= "key1 = \"some string\""
             (subs src
                   (get-in md ["table" "key1" ::target/start])
                   (get-in md ["table" "key1" ::target/end]))))
      (is (= "key2 = 123"
             (subs src
                   (get-in md ["table" "key2" ::target/start])
                   (get-in md ["table" "key2" ::target/end]))))
      (is (= "[table.lv-1]"
             (subs src
                   (get-in md ["table" "lv-1" ::target/start])
                   (get-in md ["table" "lv-1" ::target/end]))))
      (is (= "key1 = \"another string\""
             (subs src
                   (get-in md ["table" "lv-1" "key1" ::target/start])
                   (get-in md ["table" "lv-1" "key1" ::target/end]))))
      (is (= "key2 = 456"
             (subs src
                   (get-in md ["table" "lv-1" "key2" ::target/start])
                   (get-in md ["table" "lv-1" "key2" ::target/end]))))
      (is (= "[table.lv-1.lv-2]"
             (subs src
                   (get-in md ["table" "lv-1" "lv-2" ::target/start])
                   (get-in md ["table" "lv-1" "lv-2" ::target/end]))))
      (is (= "key1 = \"another string, again\""
             (subs src
                   (get-in md ["table" "lv-1" "lv-2" "key1" ::target/start])
                   (get-in md ["table" "lv-1" "lv-2" "key1" ::target/end]))))
      (is (= "key2 = 789"
             (subs src
                   (get-in md ["table" "lv-1" "lv-2" "key2" ::target/start])
                   (get-in md ["table" "lv-1" "lv-2" "key2" ::target/end])))))

    (let [src (multi-line
                "[dog.\"tater.man\"]"
                "type.name = \"pug\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"dog" {"tater.man" {"type" {"name" "pug"}}}} res))
      (is (= "[dog.\"tater.man\"]"
             (subs src
                   (get-in md ["dog" "tater.man" ::target/start])
                   (get-in md ["dog" "tater.man" ::target/end]))))
      (is (= "type.name = \"pug\""
             (subs src
                   (get-in md ["dog" "tater.man" "type" "name" ::target/start])
                   (get-in md ["dog" "tater.man" "type" "name" ::target/end])))))

    (let [src (multi-line
                "name = \"Fido\""
                "breed = \"pug\""
                ""
                "[owner]"
                "name = \"Regina Dogman\""
                "member_since = 1999-08-04")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"name" "Fido"
              "breed" "pug"
              "owner" {"name" "Regina Dogman"
                       "member_since" (LocalDate/parse "1999-08-04")}}
             res))
      (is (= "name = \"Fido\""
             (subs src
                   (get-in md ["name" ::target/start])
                   (get-in md ["name" ::target/end]))))
      (is (= "breed = \"pug\""
             (subs src
                   (get-in md ["breed" ::target/start])
                   (get-in md ["breed" ::target/end]))))
      (is (= "[owner]"
             (subs src
                   (get-in md ["owner" ::target/start])
                   (get-in md ["owner" ::target/end]))))
      (is (= "name = \"Regina Dogman\""
             (subs src
                   (get-in md ["owner" "name" ::target/start])
                   (get-in md ["owner" "name" ::target/end]))))
      (is (= "member_since = 1999-08-04"
             (subs src
                   (get-in md ["owner" "member_since" ::target/start])
                   (get-in md ["owner" "member_since" ::target/end]))))))

  (testing "Table with dotted keys"
    (let [src (multi-line
                "[fruit]"
                "apple.color = \"red\""
                "apple.taste.sweet = true"
                ""
                "[fruit.apple.texture]"
                "smooth = true")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"fruit" {"apple" {"color" "red"
                                "taste" {"sweet" true}
                                "texture" {"smooth" true}}}}
             res))
      (is (= "[fruit]"
             (subs src
                   (get-in md ["fruit" ::target/start])
                   (get-in md ["fruit" ::target/end]))))
      (is (= "apple.color = \"red\""
             (subs src
                   (get-in md ["fruit" "apple" "color" ::target/start])
                   (get-in md ["fruit" "apple" "color" ::target/end]))))
      (is (= "apple.taste.sweet = true"
             (subs src
                   (get-in md ["fruit" "apple" "taste" "sweet" ::target/start])
                   (get-in md ["fruit" "apple" "taste" "sweet" ::target/end]))))
      (is (= "[fruit.apple.texture]"
             (subs src
                   (get-in md ["fruit" "apple" "texture" ::target/start])
                   (get-in md ["fruit" "apple" "texture" ::target/end]))))
      (is (= "smooth = true"
             (subs src
                   (get-in md ["fruit" "apple" "texture" "smooth" ::target/start])
                   (get-in md ["fruit" "apple" "texture" "smooth" ::target/end]))))))

  (testing "Inline table"
    (let [src (multi-line
                "name = { first = \"Tom\", last = \"Preston-Werner\" }"
                "point = { x = 1, y = 2 }"
                "animal = { type.name = \"pug\" }")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"name" {"first" "Tom"
                      "last" "Preston-Werner"}
              "point" {"x" 1 "y" 2}
              "animal" {"type" {"name" "pug"}}}
             res))
      (is (= "name = { first = \"Tom\", last = \"Preston-Werner\" }"
             (subs src
                   (get-in md ["name" ::target/start])
                   (get-in md ["name" ::target/end]))))
      (is (= "first = \"Tom\""
             (subs src
                   (get-in md ["name" "first" ::target/start])
                   (get-in md ["name" "first" ::target/end]))))
      (is (= "last = \"Preston-Werner\""
             (subs src
                   (get-in md ["name" "last" ::target/start])
                   (get-in md ["name" "last" ::target/end]))))
      (is (= "point = { x = 1, y = 2 }"
             (subs src
                   (get-in md ["point" ::target/start])
                   (get-in md ["point" ::target/end]))))
      (is (= "x = 1"
             (subs src
                   (get-in md ["point" "x" ::target/start])
                   (get-in md ["point" "x" ::target/end]))))
      (is (= "y = 2"
             (subs src
                   (get-in md ["point" "y" ::target/start])
                   (get-in md ["point" "y" ::target/end]))))
      (is (= "animal = { type.name = \"pug\" }"
             (subs src
                   (get-in md ["animal" ::target/start])
                   (get-in md ["animal" ::target/end]))))
      (is (= "type.name = \"pug\""
             (subs src
                   (get-in md ["animal" "type" "name" ::target/start])
                   (get-in md ["animal" "type" "name" ::target/end])))))

    (let [src (multi-line
                "[product]"
                "type = { name = \"Nail\" }")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"product" {"type" {"name" "Nail"}}} res))
      (is (= "[product]"
             (subs src
                   (get-in md ["product" ::target/start])
                   (get-in md ["product" ::target/end]))))
      (is (= "type = { name = \"Nail\" }"
             (subs src
                   (get-in md ["product" "type" ::target/start])
                   (get-in md ["product" "type" ::target/end]))))
      (is (= "name = \"Nail\""
             (subs src
                   (get-in md ["product" "type" "name" ::target/start])
                   (get-in md ["product" "type" "name" ::target/end]))))))

  (testing "Array table"
    (let [src (multi-line
                "[[products]]"
                "name = \"Hammer\""
                "sku = 738594937"
                ""
                "[[products]]"
                ""
                "[[products]]"
                "name = \"Nail\""
                "sku = 284758393"
                ""
                "color = \"gray\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"products" [{"name" "Hammer"
                           "sku" 738594937}
                          {}
                          {"name" "Nail"
                           "sku" 284758393
                           "color" "gray"}]}
             res))
      (is (= "[[products]]"
             (subs src
                   (get-in md ["products" 0 ::target/start])
                   (get-in md ["products" 0 ::target/end]))))
      (is (= "name = \"Hammer\""
             (subs src
                   (get-in md ["products" 0 "name" ::target/start])
                   (get-in md ["products" 0 "name" ::target/end]))))
      (is (= "sku = 738594937"
             (subs src
                   (get-in md ["products" 0 "sku" ::target/start])
                   (get-in md ["products" 0 "sku" ::target/end]))))
      (is (= "[[products]]"
             (subs src
                   (get-in md ["products" 1 ::target/start])
                   (get-in md ["products" 1 ::target/end]))))
      (is (= "[[products]]"
             (subs src
                   (get-in md ["products" 2 ::target/start])
                   (get-in md ["products" 2 ::target/end]))))
      (is (= "name = \"Nail\""
             (subs src
                   (get-in md ["products" 2 "name" ::target/start])
                   (get-in md ["products" 2 "name" ::target/end]))))
      (is (= "sku = 284758393"
             (subs src
                   (get-in md ["products" 2 "sku" ::target/start])
                   (get-in md ["products" 2 "sku" ::target/end]))))
      (is (= "color = \"gray\""
             (subs src
                   (get-in md ["products" 2 "color" ::target/start])
                   (get-in md ["products" 2 "color" ::target/end])))))

    (let [src (multi-line
                "[[fruits]]"
                "name = \"apple\""
                ""
                "[fruits.physical]"
                "color = \"red\""
                "shape = \"round\""
                ""
                "[[fruits.varieties]]"
                "name = \"red delicious\""
                ""
                "[[fruits.varieties]]"
                "name = \"granny smith\""
                ""
                ""
                "[[fruits]]"
                "name = \"banana\""
                ""
                "[[fruits.varieties]]"
                "name = \"plantain\"")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"fruits" [{"name" "apple"
                         "physical" {"color" "red"
                                     "shape" "round"}
                         "varieties" [{"name" "red delicious"}
                                      {"name" "granny smith"}]}
                        {"name" "banana"
                         "varieties" [{"name" "plantain"}]}]}
             res))
      (is (= "[[fruits]]"
             (subs src
                   (get-in md ["fruits" 0 ::target/start])
                   (get-in md ["fruits" 0 ::target/end]))))
      (is (= "name = \"apple\""
             (subs src
                   (get-in md ["fruits" 0 "name" ::target/start])
                   (get-in md ["fruits" 0 "name" ::target/end]))))
      (is (= "[fruits.physical]"
             (subs src
                   (get-in md ["fruits" 0 "physical" ::target/start])
                   (get-in md ["fruits" 0 "physical" ::target/end]))))
      (is (= "color = \"red\""
             (subs src
                   (get-in md ["fruits" 0 "physical" "color" ::target/start])
                   (get-in md ["fruits" 0 "physical" "color" ::target/end]))))
      (is (= "shape = \"round\""
             (subs src
                   (get-in md ["fruits" 0 "physical" "shape" ::target/start])
                   (get-in md ["fruits" 0 "physical" "shape" ::target/end]))))
      (is (= "[[fruits.varieties]]"
             (subs src
                   (get-in md ["fruits" 0 "varieties" 0 ::target/start])
                   (get-in md ["fruits" 0 "varieties" 0 ::target/end]))))
      (is (= "name = \"red delicious\""
             (subs src
                   (get-in md ["fruits" 0 "varieties" 0 "name" ::target/start])
                   (get-in md ["fruits" 0 "varieties" 0 "name" ::target/end]))))
      (is (= "[[fruits.varieties]]"
             (subs src
                   (get-in md ["fruits" 0 "varieties" 1 ::target/start])
                   (get-in md ["fruits" 0 "varieties" 1 ::target/end]))))
      (is (= "name = \"granny smith\""
             (subs src
                   (get-in md ["fruits" 0 "varieties" 1 "name" ::target/start])
                   (get-in md ["fruits" 0 "varieties" 1 "name" ::target/end]))))
      (is (= "[[fruits]]"
             (subs src
                   (get-in md ["fruits" 1 ::target/start])
                   (get-in md ["fruits" 1 ::target/end]))))
      (is (= "name = \"banana\""
             (subs src
                   (get-in md ["fruits" 1 "name" ::target/start])
                   (get-in md ["fruits" 1 "name" ::target/end]))))
      (is (= "[[fruits.varieties]]"
             (subs src
                   (get-in md ["fruits" 1 "varieties" 0 ::target/start])
                   (get-in md ["fruits" 1 "varieties" 0 ::target/end]))))
      (is (= "name = \"plantain\""
             (subs src
                   (get-in md ["fruits" 1 "varieties" 0 "name" ::target/start])
                   (get-in md ["fruits" 1 "varieties" 0 "name" ::target/end]))))))

  (testing "Inline array table"
    (let [src (multi-line
                "points = [ { x = 1, y = 2, z = 3 },"
                "           { x = 7, y = 8, z = 9 },"
                "           { x = 2, y = 4, z = 8 } ]")
          res (target/parse-toml src)
          md (meta res)]
      (is (= {"points" [{"x" 1 "y" 2 "z" 3}
                        {"x" 7 "y" 8 "z" 9}
                        {"x" 2 "y" 4 "z" 8}]}
             res))
      (is (= (multi-line
               "points = [ { x = 1, y = 2, z = 3 },"
               "           { x = 7, y = 8, z = 9 },"
               "           { x = 2, y = 4, z = 8 } ]")
             (subs src
                   (get-in md ["points" ::target/start])
                   (get-in md ["points" ::target/end]))))
      (is (= "{ x = 1, y = 2, z = 3 }"
             (subs src
                   (get-in md ["points" 0 ::target/start])
                   (get-in md ["points" 0 ::target/end]))))
      (is (= "x = 1"
             (subs src
                   (get-in md ["points" 0 "x" ::target/start])
                   (get-in md ["points" 0 "x" ::target/end]))))
      (is (= "y = 2"
             (subs src
                   (get-in md ["points" 0 "y" ::target/start])
                   (get-in md ["points" 0 "y" ::target/end]))))
      (is (= "z = 3"
             (subs src
                   (get-in md ["points" 0 "z" ::target/start])
                   (get-in md ["points" 0 "z" ::target/end]))))
      (is (= "{ x = 7, y = 8, z = 9 }"
             (subs src
                   (get-in md ["points" 1 ::target/start])
                   (get-in md ["points" 1 ::target/end]))))
      (is (= "x = 7"
             (subs src
                   (get-in md ["points" 1 "x" ::target/start])
                   (get-in md ["points" 1 "x" ::target/end]))))
      (is (= "y = 8"
             (subs src
                   (get-in md ["points" 1 "y" ::target/start])
                   (get-in md ["points" 1 "y" ::target/end]))))
      (is (= "z = 9"
             (subs src
                   (get-in md ["points" 1 "z" ::target/start])
                   (get-in md ["points" 1 "z" ::target/end]))))
      (is (= "{ x = 2, y = 4, z = 8 }"
             (subs src
                   (get-in md ["points" 2 ::target/start])
                   (get-in md ["points" 2 ::target/end]))))
      (is (= "x = 2"
             (subs src
                   (get-in md ["points" 2 "x" ::target/start])
                   (get-in md ["points" 2 "x" ::target/end]))))
      (is (= "y = 4"
             (subs src
                   (get-in md ["points" 2 "y" ::target/start])
                   (get-in md ["points" 2 "y" ::target/end]))))
      (is (= "z = 8"
             (subs src
                   (get-in md ["points" 2 "z" ::target/start])
                   (get-in md ["points" 2 "z" ::target/end])))))))

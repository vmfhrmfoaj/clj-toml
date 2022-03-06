(ns clj-toml.core
  (:require [clojure.string :as str]
            [instaparse.core :as insta]
            [instaparse.gll :as gll])
  (:import java.lang.Character
           java.time.LocalDate
           java.time.LocalDateTime
           java.time.LocalTime
           java.time.OffsetDateTime))

(def toml-grammar
  "ABNF grammar for TOML syntax."
  ";; modified version of https://raw.githubusercontent.com/toml-lang/toml/1.0.0/toml.abnf

;; This document describes TOML's syntax, using the ABNF format (defined in
;; RFC 5234 -- https://www.ietf.org/rfc/rfc5234.txt).
;;
;; All valid TOML documents will match this description, however certain
;; invalid documents would need to be rejected as per the semantics described
;; in the supporting text description.

;; It is possible to try this grammar interactively, using instaparse.
;;     http://instaparse.mojombo.com/
;;
;; To do so, in the lower right, click on Options and change `:input-format` to
;; ':abnf'. Then paste this entire ABNF document into the grammar entry box
;; (above the options). Then you can type or paste a sample TOML document into
;; the beige box on the left. Tada!

;; Overall Structure

toml = expression *( <newline> expression )

<expression> =  <ws> [ <comment> ]
<expression> =/ <ws> table <ws> [ <comment> ]
<expression> =/ <ws> keyval <ws> [ <comment> ]

;; Whitespace

<ws> = *wschar
<wschar> =  %x20  ; Space
<wschar> =/ %x09  ; Horizontal tab

;; Newline

newline =  %x0A     ; LF
newline =/ %x0D.0A  ; CRLF

;; Comment

comment-start-symbol = %x23 ; #
<non-ascii> = %x80-D7FF / %xE000-10FFFF
<non-eol> = %x09 / %x20-7F / non-ascii

comment = comment-start-symbol *non-eol

;; Key-Value pairs

keyval = key <keyval-sep> val

<key> = simple-key / dotted-key
simple-key = quoted-key / unquoted-key

<unquoted-key> = 1*( ALPHA / DIGIT / %x2D / %x5F ) ; A-Z / a-z / 0-9 / - / _
<quoted-key> = basic-string / literal-string
dotted-key = simple-key 1*( <dot-sep> simple-key )

dot-sep   = ws %x2E ws  ; . Period
keyval-sep = ws %x3D ws ; =

<val> = string / boolean / array / inline-table / date-time / float / integer

;; String

string = ml-basic-string / basic-string / ml-literal-string / literal-string

;; Basic String

basic-string = <quotation-mark> *basic-char <quotation-mark>

<quotation-mark> = %x22            ; \"

<basic-char> = basic-unescaped / escaped
<basic-unescaped> = wschar / %x21 / %x23-5B / %x5D-7E / non-ascii
escaped = <escape> escape-seq-char

escape = %x5C                     ; \\
<escape-seq-char> =  %x22         ; \"   quotation mark  U+0022
<escape-seq-char> =/ %x5C         ; \\   reverse solidus U+005C
<escape-seq-char> =/ %x62         ; b    backspace       U+0008
<escape-seq-char> =/ %x66         ; f    form feed       U+000C
<escape-seq-char> =/ %x6E         ; n    line feed       U+000A
<escape-seq-char> =/ %x72         ; r    carriage return U+000D
<escape-seq-char> =/ %x74         ; t    tab             U+0009
<escape-seq-char> =/ %x75 4HEXDIG ; uXXXX                U+XXXX
<escape-seq-char> =/ %x55 8HEXDIG ; UXXXXXXXX            U+XXXXXXXX

;; Multiline Basic String

ml-basic-string = <ml-basic-string-delim> [ newline ] ml-basic-body
                  <ml-basic-string-delim>
ml-basic-string-delim = 3quotation-mark
<ml-basic-body> = *mlb-content *( mlb-quotes 1*mlb-content ) [ mlb-quotes ]

<mlb-content> = mlb-char / newline / mlb-escaped-nl
<mlb-char> = mlb-unescaped / escaped
<mlb-quotes> = 1*2quotation-mark
<mlb-unescaped> = wschar / %x21 / %x23-5B / %x5D-7E / non-ascii
mlb-escaped-nl = escape ws newline *( wschar / newline )

;; Literal String

literal-string = <apostrophe> *literal-char <apostrophe>

<apostrophe> = %x27 ; ' apostrophe

<literal-char> = %x09 / %x20-26 / %x28-7E / non-ascii

;; Multiline Literal String

ml-literal-string = <ml-literal-string-delim> [ newline ] ml-literal-body
                    <ml-literal-string-delim>
ml-literal-string-delim = 3apostrophe
<ml-literal-body> = *mll-content *( mll-quotes 1*mll-content ) [ mll-quotes ]

<mll-content> = mll-char / newline
<mll-char> = %x09 / %x20-26 / %x28-7E / non-ascii
<mll-quotes> = 1*2apostrophe

;; Integer

<integer> = dec-int / hex-int / oct-int / bin-int

<minus> = %x2D                       ; -
<plus> = %x2B                        ; +
underscore = %x5F                    ; _
<digit1-9> = %x31-39                 ; 1-9
<digit0-7> = %x30-37                 ; 0-7
<digit0-1> = %x30-31                 ; 0-1

hex-prefix = %x30.78               ; 0x
oct-prefix = %x30.6F               ; 0o
bin-prefix = %x30.62               ; 0b

dec-int = [ minus / plus ] unsigned-dec-int
<unsigned-dec-int> = DIGIT / digit1-9 1*( DIGIT / <underscore> DIGIT )

hex-int = <hex-prefix> HEXDIG *( HEXDIG / <underscore> HEXDIG )
oct-int = <oct-prefix> digit0-7 *( digit0-7 / <underscore> digit0-7 )
bin-int = <bin-prefix> digit0-1 *( digit0-1 / <underscore> digit0-1 )

;; Float

<float> = float-number
<float> =/ special-float

float-number = float-int-part ( exp / frac [ exp ] )
<float-int-part> = [ minus / plus ] unsigned-dec-int
<frac> = decimal-point zero-prefixable-int
<decimal-point> = %x2E               ; .
<zero-prefixable-int> = DIGIT *( DIGIT / <underscore> DIGIT )

<exp> = \"e\" float-exp-part
<float-exp-part> = [ minus / plus ] zero-prefixable-int

<special-float> = ( inf / neg-inf / nan )
inf  = [ plus ] %x69.6e.66  ; inf
neg-inf = minus %x69.6e.66  ; -inf
nan = <[ minus / plus ]> %x6e.61.6e  ; nan

;; Boolean

<boolean> = true / false

true    = %x74.72.75.65     ; true
false   = %x66.61.6C.73.65  ; false

;; Date and Time (as defined in RFC 3339)

<date-time>      = offset-date-time / local-date-time / local-date / local-time

<date-fullyear>  = 4DIGIT
<date-month>     = 2DIGIT  ; 01-12
<date-mday>      = 2DIGIT  ; 01-28, 01-29, 01-30, 01-31 based on month/year
time-delim     = \"T\" / %x20 ; T, t, or space
<time-hour>      = 2DIGIT  ; 00-23
<time-minute>    = 2DIGIT  ; 00-59
<time-second>    = 2DIGIT  ; 00-58, 00-59, 00-60 based on leap second rules
<time-secfrac>   = \".\" 1*DIGIT
<time-numoffset> = ( \"+\" / \"-\" ) time-hour \":\" time-minute
<time-offset>    = \"Z\" / time-numoffset

<partial-time>   = time-hour \":\" time-minute \":\" time-second [ time-secfrac ]
<full-date>      = date-fullyear \"-\" date-month \"-\" date-mday
<full-time>      = partial-time time-offset

;; Offset Date-Time

offset-date-time = full-date time-delim full-time

;; Local Date-Time

local-date-time = full-date time-delim partial-time

;; Local Date

local-date = full-date

;; Local Time

local-time = partial-time

;; Array

array = <array-open> [ array-values ] <ws-comment-newline> <array-close>

array-open =  %x5B ; [
array-close = %x5D ; ]

<array-values> =  <ws-comment-newline> val <ws-comment-newline> <array-sep> array-values
<array-values> =/ <ws-comment-newline> val <ws-comment-newline> <[ array-sep ]>

array-sep = %x2C  ; , Comma

ws-comment-newline = *( wschar / [ <comment> ] newline )

;; Table

<table> = std-table / array-table

;; Standard Table

std-table = <std-table-open> key <std-table-close>

std-table-open  = %x5B ws     ; [ Left square bracket
std-table-close = ws %x5D     ; ] Right square bracket

;; Inline Table

inline-table = <inline-table-open> [ inline-table-keyvals ] <inline-table-close>

inline-table-open  = %x7B ws     ; {
inline-table-close = ws %x7D     ; }
inline-table-sep   = ws %x2C ws  ; , Comma

<inline-table-keyvals> = keyval [ <inline-table-sep> inline-table-keyvals ]

;; Array Table

array-table = <array-table-open> key <array-table-close>

array-table-open  = %x5B.5B ws  ; [[ Double left square bracket
array-table-close = ws %x5D.5D  ; ]] Double right square bracket

;; Built-in ABNF terms, reproduced here for clarity

<ALPHA> = %x41-5A / %x61-7A ; A-Z / a-z
<DIGIT> = %x30-39 ; 0-9
<HEXDIG> = DIGIT / \"A\" / \"B\" / \"C\" / \"D\" / \"E\" / \"F\"")

(def toml-parser
  "Parser for TOML syntax."
  (insta/parser toml-grammar :input-format :abnf))

(let [tb-key
      (fn [arr-info ks]
        (loop [new-ks []
               old-ks []
               ks ks]
          (let [[k & ks] ks
                old-ks (conj old-ks k)
                arr-idx (get-in arr-info (concat old-ks [::idx]))]
            (if k
              (recur (cond-> (conj new-ks k)
                       arr-idx (conj arr-idx))
                     old-ks
                     ks)
              new-ks))))
      merge-exps
      (fn [& xs]
        (loop [res {}
               md {}
               tb-ks nil
               arr-info {}
               xs xs]
          (let [[[tag ks val :as x] & xs] xs
                {:keys [::gll/start-index ::gll/end-index]} (meta x)
                [res md tb-ks arr-info]
                (condp = tag
                  :keyval
                  , (let [ks (concat tb-ks ks)
                          kv-md {::start start-index ::end end-index}
                          val-md (-> (meta val)
                                     (dissoc ::gll/start-index)
                                     (dissoc ::gll/end-index))
                          kv-md (cond-> (merge val-md kv-md)
                                  (get-in res ks)
                                  (assoc ::warning (str "replaced a key decleared at "
                                                        (get-in md (concat ks [::start]))
                                                        " position")))]
                      [(assoc-in res ks val)
                       (assoc-in md ks kv-md)
                       tb-ks
                       arr-info])
                  :std-table
                  , (let [tb-ks (tb-key arr-info ks)
                          tb (not-empty (get-in res tb-ks))]
                      [(assoc-in res tb-ks {})
                       (assoc-in md tb-ks (cond-> {::start start-index
                                                   ::end   end-index}
                                            tb (assoc ::warning (str "replaced a table decleared at "
                                                                     (get-in md (concat tb-ks [::start]))
                                                                     " position"))))
                       tb-ks
                       arr-info])
                  :array-table
                  , (let [existing? (get-in arr-info ks)
                          arr-info (update-in arr-info ks #(if % {::idx (inc (::idx %))} {::idx 0}))
                          tb-ks (tb-key arr-info ks)
                          tb (not-empty (get-in res tb-ks))]
                      [(if existing?
                         (assoc-in res tb-ks {})
                         (assoc-in res (drop-last tb-ks) []))
                       (assoc-in md tb-ks (cond-> {::start start-index
                                                   ::end   end-index}
                                            tb (assoc ::warning (str "replaced a table decleared at "
                                                                     (get-in md (concat tb-ks [::start]))
                                                                     " position"))))
                       tb-ks
                       arr-info])
                  nil)]
            (if (empty? xs)
              (when res
                (with-meta res md))
              (recur res md tb-ks arr-info xs)))))
      tm {:array (fn [& xs]
                   (with-meta
                     (into [] xs)
                     (->> xs
                          (map-indexed #(when-let [md (meta %2)]
                                          (let [{:keys [::gll/start-index ::gll/end-index]} md
                                                md (cond-> (-> md
                                                               (dissoc ::gll/start-index)
                                                               (dissoc ::gll/end-index))
                                                     start-index (assoc ::start start-index)
                                                     end-index   (assoc ::end end-index))]
                                            (vector %1 md))))
                          (remove nil?)
                          (into {}))))
          :basic-string str
          :bin-int #(Long/parseLong (apply str %&) 2)
          :dec-int #(Long/parseLong (apply str %&))
          :dotted-key concat
          :escaped (fn [x & xs]
                     (if (and xs (or (= "u" x)
                                     (= "U" x)))
                       (-> (apply str xs)
                           (Integer/parseUnsignedInt 16)
                           (Character/toString))
                       (get {"b" "\b"
                             "f" "\f"
                             "n" "\n"
                             "r" "\r"
                             "t" "\t"}
                            x x)))
          :false (constantly false)
          :float-number #(Double/parseDouble (apply str %&))
          :hex-int #(Long/parseLong (apply str %&) 16)
          :inf (constantly ##Inf)
          :inline-table merge-exps
          :literal-string str
          :local-date #(LocalDate/parse (apply str %&))
          :local-date-time #(LocalDateTime/parse (apply str %&))
          :local-time #(LocalTime/parse (apply str %&))
          :ml-basic-string (let [ws? #(or (= " "  %)
                                          (= "\t" %)
                                          (= "\r" %)
                                          (= "\n" %))]
                             (fn [& xs]
                               (loop [res nil
                                      xs (drop-while ws? xs)]
                                 (let [[line-chars xs] (split-with #(and (not= "\n" %) (not (vector? %))) xs)
                                       [sep & xs] xs
                                       [sep xs] (if (vector? sep)
                                                  [nil (drop-while ws? xs)]
                                                  [sep xs])
                                       res (str (apply str res line-chars) sep)]
                                   (if (empty? xs)
                                     res
                                     (recur res xs))))))
          :ml-literal-string #(str/trim (apply str %&))
          :nan (constantly ##NaN)
          :neg-inf (constantly ##-Inf)
          :newline (constantly "\n")
          :oct-int #(Long/parseLong (apply str %&) 8)
          :offset-date-time #(OffsetDateTime/parse (apply str %&))
          :simple-key #(vector (apply str %&))
          :string str
          :time-delim (constantly "T")
          :toml merge-exps
          :true (constantly true)}]
  (defn toml-transfrom
    "Transform parsed data to Clojure associative structure."
    [tree]
    (when-let [res (insta/transform tm tree)]
      (vary-meta res #(-> % (dissoc ::gll/start-index) (dissoc ::gll/end-index))))))

(defn parse-toml
  "Parses TOML structure from string into Clojure associative structure."
  [input]
  (->> input
       (toml-parser)
       (toml-transfrom)))

(def parse-string
  "Alias for `parse-toml`."
  parse-toml)

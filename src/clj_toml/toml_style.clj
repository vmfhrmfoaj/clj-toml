(ns clj-toml.toml-style
  (:use [blancas.kern.core]
        [clojure.string :only [join]])
  (:require [blancas.kern.lexer :as lex]))

(def toml-def
  (assoc lex/basic-def
    :comment-line "#"
    :identifier-letter (<|> alpha-num (one-of* "~!@#$%^&*()_-=+[]{}\\|;:<>,./?"))))

(def- rec (lex/make-parsers toml-def))

(def trim       (:trim       rec))
(def lexeme     (:lexeme     rec))
(def sym        (:sym        rec))
(def new-line   (:new-line   rec))
(def one-of     (:one-of     rec))
(def none-of    (:none-of    rec))
(def token      (:token      rec))
(def word       (:word       rec))
(def identifier (:identifier rec))
(def field      (:field      rec))
(def char-lit   (:char-lit   rec))
(def string-lit (:string-lit rec))
(def dec-lit    (:dec-lit    rec))
(def oct-lit    (:oct-lit    rec))
(def hex-lit    (:hex-lit    rec))
(def float-lit  (:float-lit  rec))
(def bool-lit   (:bool-lit   rec))
(def nil-lit    (:nil-lit    rec))
(def parens     (:parens     rec))
(def braces     (:braces     rec))
(def angles     (:angles     rec))
(def brackets   (:brackets   rec))
(def semi       (:semi       rec))
(def comma      (:comma      rec))
(def colon      (:colon      rec))
(def dot        (:dot        rec))
(def semi-sep   (:semi-sep   rec))
(def semi-sep1  (:semi-sep1  rec))
(def comma-sep  (:comma-sep  rec))
(def comma-sep1 (:comma-sep1 rec))

(ns cljcc
	(:require
		[potemkin :refer [import-vars]]
		(cljcc lexer parser)))

(import-vars
	[cljcc.lexer  make-lexer]
	[cljcc.parser make-parser])

(defn make-combined
	[tokens productions initial-symbol]
	(let
		[lex (make-lexer tokens)
		 parse (make-parser productions initial-symbol)]
		(fn [string] (-> string lex parse))))
(ns cljcc.lexer-test
	(:use
		midje.sweet)
	(:require
		cljcc.lexer
		[midje.util :refer [expose-testables]]))

(expose-testables cljcc.lexer)

(fact "reserved token names are caught"
	(make-lexer #{{:name :$ :pattern "a"}}) => (throws Exception))

(def tokens-1
	#{	{:name :whitespace
		 :pattern #"\s+"
		 :ignore true}
		{:name :identifier
		 :pattern #"\w+"
		 :priority -1}
		{:name :hex
		 :pattern #"([\da-fA-F]+)h"
		 :valuation-fn #(Integer/parseInt (second %) 16)}
		{:name :if
		 :pattern "if"}
		{:name :assign
		 :pattern "="}
		{:name :equals
		 :pattern "=="}})

(def result-1
	((make-lexer tokens-1) "if ifoobar == 12h"))

(fact "ignored tokens are ignored"
	result-1 => (fn [actual] (not-any? #(= :whitespace (:token-name %)) actual)))
(fact "end sentinel present"
	(last result-1) => #(= :$ (:token-name %)))
(fact "longest matching token wins"
	result-1 => (fn [actual] (not-any? #(= :assign (:token-name %)) actual)))
(fact "priority override works"
	result-1 => (fn [actual] (some #(= :if (:token-name %)) actual)))
(fact "valuation fn works"
	result-1 => (fn [actual] (some #(and (= :hex (:token-name %)) (= 18 (:value %))) actual)))

(def tokens-2
	#{	{:name :a
		 :pattern "a"}
		{:name :b
		 :pattern #"b"
		 :valuation-fn (fn [_] (throw (Exception. "trap")))}})

(def result-2
	((make-lexer tokens-2) "aab"))

(fact "laziness works"
	(first result-2) => truthy
	(last result-2) => (throws Exception "trap"))

(def tokens-3
	#{	{:name :p1
		 :pattern "AB"}
		{:name :p2
		 :pattern "CD"
		 :ci true}})

(def lex-3
	(make-lexer tokens-3))

(fact "case-sensitive string pattern works"
	(lex-3 "ab") => (throws Exception)
	(lex-3 "aB") => (throws Exception)
	(lex-3 "Ab") => (throws Exception)
	(lex-3 "AB") => (fn [actual] (some #(= :p1 (:token-name %)) actual)))
(fact "case-insensitive string pattern works"
	(lex-3 "cd") => (fn [actual] (some #(= :p2 (:token-name %)) actual))
	(lex-3 "cD") => (fn [actual] (some #(= :p2 (:token-name %)) actual))
	(lex-3 "Cd") => (fn [actual] (some #(= :p2 (:token-name %)) actual))
	(lex-3 "CD") => (fn [actual] (some #(= :p2 (:token-name %)) actual)))
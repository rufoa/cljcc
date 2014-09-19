(ns cljcc.convert-pattern-test
	(:use
		midje.sweet)
	(:require
		cljcc.lexer
		[midje.util :refer [expose-testables]]))

(expose-testables cljcc.lexer)

(defn- matches
	[pattern string]
	(-> pattern
		(Pattern->RegExp)
		(. toAutomaton)
		(. run string)))

(tabular
	(fact
		(matches ?pattern ?string) => ?expected ; check the implementation
		(not= nil (re-matches ?pattern ?string)) => ?expected) ; check the checks
	?pattern   ?string ?expected
	; things dk.brics.automaton can do itself
	#"a"       "a"     true
	#"a"       "z"     false
	#"[a-c]"   "a"     true
	#"[a-c]"   "z"     false
	; conversion of groups
	#"(?<n>a)" "a"     true
	#"(?<n>a)" "z"     false
	#"(?:a)"   "a"     true
	#"(?:a)"   "z"     false
	; reluctant and possessive quantifiers
	#"a*?"     "a"     true
	#"a*?"     "z"     false
	#"a\*?"    "a"     true
	#"a\*?"    "a*"    true
	#"a\*?"    "aa"    false
	#"a*+"     "a"     true
	#"a*+"     "z"     false
	#"a\*+"    "a*"    true
	#"a\*+"    "a**"   true
	#"a\*+"    "aa"    false
	#"a\\*+"   "a\\"   true
	#"a+?"     "a"     true
	#"a+?"     ""      false
	#"a+?"     "z"     false
	#"a\+?"    "a"     true
	#"a\+?"    "a+"    true
	#"a\+?"    "aa"    false
	#"a++"     "a"     true
	#"a++"     "z"     false
	#"a\++"    "a+"    true
	#"a\++"    "a++"   true
	#"a\++"    "aa"    false
	; standard character classes
	#"\d"      "1"     true
	#"\d"      "z"     false
	#"\D"      "z"     true
	#"\D"      "1"     false
	#"\s"      " "     true
	#"\s"      "z"     false
	#"\S"      "z"     true
	#"\S"      " "     false
	#"\w"      "a"     true
	#"\w"      " "     false
	#"\W"      " "     true
	#"\W"      "a"     false
	; nested character classes
	#"[\da]"   "1"     true
	#"[\da]"   "a"     true
	#"[\da]"   "d"     false
	#"[^\d]"   "1"     false
	#"[^\d]"   "a"     true
	#"[^a\d]"  "1"     false
	#"[^a\d]"  "a"     false
	#"[^a\d]"  "z"     true
	#"[\\\d]"  "\\"    true
	#"[\]\d]"  "]"     true
	; double quotes
	#"\""      "\""    true
	#"[\"]"    "\""    true
	; escaping
	#"\\"      "\\"    true
	#"\\d"     "1"     false
	#"\\d"     "\\d"   true
	#"\\\d"    "\\1"   true
	#"\\\d"    "\\d"   false)
(ns cljcc.lexer
	(:require
		[clojure.string :as s]
		[slingshot.slingshot :refer [throw+]])
	(:import
		(dk.brics.automaton RegExp Automaton RunAutomaton State)))


(defrecord Position [line char]
	Object
	(toString [_] (str "line " line ", char " char)))

(defn- update-position
	[{:keys [line char] :as position} consumed]
	(let
		[	newlines (count (filter (partial = \newline) consumed))
			offset (count (take-while (partial not= \newline) (reverse consumed)))
			base (if (zero? newlines) char 1)]
		(assoc position :line (+ line newlines) :char (+ base offset))))


(defmulti ^:private matcher        "Is the given string an instance of the token?" (comp type :pattern))
(defmulti ^:private prefix-matcher "Could the given string go on to become an instance of the token, given a suitable suffix?" (comp type :pattern))

(defn- ^:testable Pattern->RegExp
	"Convert java.util.regex.Pattern to dk.brics.automaton.RegExp so we can manipulate it as a DFA"
	^dk.brics.automaton.RegExp [re]
	(let [classes {"d" "\u0030-\u0039"
		            "D" "\u0000-\u0029\u0040-\uffff"
		            "s" "\u0009-\u000d\u0020"
		            "S" "\u0000-\u0008\u000e-\u0019\u0021-\uffff"
		            "w" "\u0030-\u0039\u0041-\u005a\u005f\u0061-\u007a"
		            "W" "\u0000-\u002f\u003a-\u0040\u005b-\u005e\u0060\u007b-\uffff"}]
		(-> re
			(str)
			(s/replace #"(?<!\\)(\\\\)*\(\?<\w+>" "$1(") ; turn named groups into normal groups
			(s/replace #"(?<!\\)(\\\\)*\(\?:" "$1(") ; turn non-capturing groups to normal groups
			(s/replace #"(?<!\\)(\\\\)*([\*\+])(?:[\?\+])" "$1$2") ; reluctant and possessive quantifiers
			(s/replace #"(\[(?:[^\\\]]|\\[^\]sSwWdD]|\\\])*)\\([sSwWdD])" #(str (nth % 1) (get classes (nth % 2)))) ; standard predefined character classes inside []
			(s/replace #"(?<!\\)(\\\\)*\\([dDsSwW])" #(str (nth % 1) "[" (get classes (nth % 2)) "]")) ; standard predefined character classes outside []
			(s/replace #"(?<!\\)(\\\\)*\u0034" "$1\\\u0034") ; double quotes need to be escaped
			(RegExp.))))

(defmethod matcher java.lang.String
	[{:keys [pattern ci] :as token}]
	(fn [string]
		(let [string'  (if ci (s/lower-case string)  string)
		      pattern' (if ci (s/lower-case pattern) pattern)]
			(when
				(and
					(>= (count string') (count pattern'))
					(= (subs string' 0 (count pattern')) pattern'))
				{:consumed (subs string 0 (count pattern))
				 :token token}))))

(defmethod prefix-matcher java.lang.String
	[{:keys [pattern ci]}]
	(fn [string]
		(let [string  (if ci (s/lower-case string)  string)
		      pattern (if ci (s/lower-case pattern) pattern)]
			(and
				(<= (count string) (count pattern))
				(= string (subs pattern 0 (count string)))))))

; you can't memoize with defmethod
(.addMethod ^clojure.lang.MultiFn prefix-matcher java.util.regex.Pattern
	(memoize (fn [{:keys [pattern]}]
		(let [automaton (.toAutomaton (Pattern->RegExp pattern))]
			(doseq [^State state (.getStates automaton)]
				(.setAccept state true))
			(.restoreInvariant automaton)
			(.setDeterministic automaton true)
			(let [run-automaton (RunAutomaton. automaton)]
				(fn [string]
					(.run run-automaton string)))))))

(defmethod matcher java.util.regex.Pattern
	[{:keys [pattern valuation-fn] :as token}]
	(let [pattern (re-pattern (str "^" pattern))] ; anchor at start with ^
		(fn [string]
			(when-let [match (re-find pattern string)]
				(if valuation-fn
					{:consumed (if (string? match) match (first match))
					 :value (valuation-fn match)
					 :token token}
					{:consumed (if (string? match) match (first match))
					 :token token})))))


(defn- buffer-enough
	"Pull on the input just enough that we can correctly determine the next token"
	[tokens buffer input]
	(loop [buffer buffer input input]
		(let [cands (filter #((prefix-matcher %) buffer) tokens)]
			(if (and (seq cands) (seq input))
				(recur
					(str buffer (first input))
					(next input))
				[buffer input]))))

(defn ^:testable make-lexer
	"Given a list of tokens in our grammar, return a lazy lexing function:[string]->[token]"
	[tokens]
	(if (some #(= (:name %) :$) tokens)
		(throw+ {:type ::reserved-symbol :message "Token name :$ is reserved for internal use"})
		(letfn
			[(lazy-lex
				[buffer input position]
				(let [[buffer input] (buffer-enough tokens buffer input)]
					(if (empty? buffer)
						(cons {:token {:name :$} :position position} nil) ; end sentinel
						(let [cands
							(->> tokens
								(map #((matcher %) buffer) ,,,)
								(filter #(and (not (nil? %)) (not-empty (:consumed %))) ,,,))] ; must match and must also consume input
							(if (empty? cands)
								(throw+ {:type ::unexpected-input :message (str "Unexpected input or possible premature end of input at " position)})
								(let [best-match
									(last (sort-by
										(fn [{:keys [consumed token]}] [(count consumed) (:priority token 0)]) ; scoring fn: longest match, then :priority
										cands))]
									(lazy-seq
										(cons
											(assoc best-match :position position)
											(lazy-lex
												(subs buffer (count (:consumed best-match)))
												input
												(update-position position (:consumed best-match)))))))))))]
			(fn [input]
				(map
					#(->
						(assoc % :token-name (get-in % [:token :name]))
						(dissoc % :token))
					(filter (comp not :ignore :token)
						(lazy-lex "" input (Position. 1 1))))))))
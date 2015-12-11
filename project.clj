(defproject cljcc "0.1.3"
	:description "A parser generator for Clojure"
	:url "https://cljcc.com/"
	:dependencies [
		[org.clojure/clojure "1.7.0"]
		[dk.brics.automaton/automaton "1.11.2"]
		[potemkin "0.4.1"]
		[slingshot "0.12.2"]]
	:profiles {
		:dev {
			:dependencies [[midje "1.8.2"]]
			:plugins [[lein-midje "3.2"]]}})
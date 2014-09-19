(defproject cljcc "0.1.0-SNAPSHOT"
	:description "A parser generator for Clojure"
	:url "http://cljcc.com/"
	:dependencies [
		[org.clojure/clojure "1.4.0"]
	 	[dk.brics.automaton/automaton "1.11-8"]
	 	[potemkin "0.3.4"]
	 	[rhizome "0.2.0"]]
	:profiles {
		:dev {
			:dependencies [[midje "1.5.1"]]
			:plugins [[lein-midje "2.0.4"]]}})
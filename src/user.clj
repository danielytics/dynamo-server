(ns user
  (:require
    [reloaded.repl :refer [system init start stop go reset]]
    [dynamo.system :refer [new-system]]
    [dynamo.core :refer [make-system]]
    [dynamo.components.command-parser :refer [builtin-rules]]))

(reloaded.repl/set-init! #(new-system make-system {:port 9000}))

(defn parser
  [& [command]]
  (case command
    :list     (do
                (println "Listing commands:")
                (doseq [cmd (get-in system [:command-parser :commands])]
                  (println "  " cmd)))
    :grammar  (println (get-in system [:command-parser :grammar]))
    :builtins (do
                (println "listing builtin rules:")
                (doseq [[rule-name rule-body] builtin-rules]
                  (println "  " (name rule-name) "\t:=" rule-body)))
    :?        (do
                (println "Listing options:")
                (doseq [[cmd desc] [[:list "List user commands"]
                                    [:grammar "Display full grammar"]
                                    [:builtins "Display list of builtin rules"]
                                    [:? "Display this help text"]]]
                  (println "  " cmd "\t " desc)))
    (println "Unknown command. Try :?")))


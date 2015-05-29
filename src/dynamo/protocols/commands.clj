(ns dynamo.protocols.commands)

(defprotocol Command
  (parser [component] "Return a grammer to parse command"))


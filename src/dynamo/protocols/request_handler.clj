(ns dynamo.protocols.request-handler)

(defprotocol RequestHandler
  (handle [_ connection-state request]))


(ns dynamo.protocols.updates)


(defprotocol Updater
  "API for issuing updates to be handled"
  (send! [component command data] "Send a update to be performed on game world"))


(defprotocol Handler
  "Handle a update"
  (handle [component game-data update-data] "Handle the update"))


(defprotocol RegisterHandler
  "Register a single update to be handled"
  (update-id [component] "Return the keyword id of the update"))


(defprotocol RegisterMultiHandler
  "Register multiple update to be handled"
  (update-ids [component] "Rutern a collection of keyword ids of the updates to handle"))


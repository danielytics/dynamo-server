(ns dynamo.lib.player
  (:require
    [dynamo.components.server :refer [*connection*]]
    [dynamo.comms :as comms]))

(defn current
  "Get the player currently being processed (ie who generated the update)"
  []
  (:player-id *connection*))

(defn reply!
  "Send a message to the current player. Silently ignore if no stream available."
  [& texts]
  (when-let [s (:stream *connection*)]
    (comms/put! s (apply str texts))))


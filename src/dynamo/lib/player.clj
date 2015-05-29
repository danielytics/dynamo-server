(ns dynamo.lib.player
  (:require
    [dynamo.lib.comms :as comms]))

(def ^{:dynamic true} *current-player* nil)


(defn current
  "Get the player currently being processed (ie who generated the update)"
  []
  (:player-id *current-player*))


(defn preferences
  "Get the current players preferences"
  []
  (:preferences *current-player*))


(defn reply!
  "Send a message to the current player. Silently ignore if no stream available."
  [& texts]
  (when-let [s (:stream *current-player*)]
    (comms/put! s (apply str texts))))


(ns dynamo.lib.commands
  "Utilities for processing player commands"
  (:require
    [dynamo.lib.player :as player]))


(defn handle 
  ""
  [[_ game-data [command command-data]] command-handler]
  (let [player-data (get-in game-data [:players (player/current)])]
    (command-handler
      command
      game-data
      player-data
      (get-in game-data [:rooms (:room player-data)])
      command-data))) 

(defn updated
  [game-data player-data & updates]
  (let [update-list (for [u updates]
                      (if (keyword? u)
                        [u nil]
                        u))
        game-data   (assoc-in game-data
                              [:players (player/current)]
                              player-data)]
    [game-data update-list]))


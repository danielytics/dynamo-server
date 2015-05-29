(ns dynamo.components.commands.movement
  (:require
    [dynamo.protocols.updates :as updates]
    [dynamo.protocols.commands :as commands]
    [dynamo.lib.commands :as cmds]
    [dynamo.lib.player :as player]))

(def direction-to-text {"n" "north" "s" "south" "e" "east" "w" "west"})


(defn do-commands
  [command game-data player-data room-data {:keys [direction] :as params}]
  (let [dir     (str (first direction))
        room-id (get-in room-data [:exits dir])]
    (if room-id
      (do
        (player/reply! "You walk " (get direction-to-text dir))
        (cmds/updated
          game-data
          (assoc player-data :room room-id)
          (if (:auto-look? (player/preferences))
            :command/look
            :command/quick-look)))
      (do
        (player/reply! "You cannot go that way.")
        game-data))))


(defrecord MovementCommands []
  updates/RegisterMultiHandler
  (updates/update-ids [_]
    [:command/walk])

  commands/Command 
  (commands/parser [_]
    {:walk "direction
            direction = direction-north | direction-south | direction-west | direction-east
            <direction-north> = 'n' | 'north'
            <direction-south> = 's' | 'south'
            <direction-west> = 'w' | 'west'
            <direction-east> = 'e' | 'east'"})

  updates/Handler
  (updates/handle [c data command]
    (cmds/handle [c data command] do-commands)))


(defn new-movement-commands []
  (map->MovementCommands {}))



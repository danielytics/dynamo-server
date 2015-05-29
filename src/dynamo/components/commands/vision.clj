(ns dynamo.components.commands.vision
  (:require
    [dynamo.protocols.updates :as updates]
    [dynamo.protocols.commands :as commands]
    [dynamo.lib.player :as player]))


(defn do-commands
  [command game-data player-data room-data {:keys [target]}]
    (case command
      :command/look
        (if target
          (player/reply!  "You look at " target " and see nothing.\n")
          (player/reply!  "You look around and see " (:name room-data) "\n"))
      :command/glance
        (if target
          (player/reply! "You glance at " target " and see nothing.\n")
          (player/reply! "What do you wish to glance at?")))
    game-data)


(defrecord VisionCommands []
  updates/RegisterMultiHandler
  (updates/update-ids [_]
    [:command/look
     :command/glance])

  commands/Command 
  (commands/parser [_]
    {:look "<look-aliases> target?
            look-aliases = 'l' | 'look'"
     :glance "<glance-aliases> <'at'>? target?
              glance-aliases = 'gl' | 'glance'"})

  updates/Handler
  (updates/handle [_ game-data [command command-data]]
    (let [player-data (get-in game-data [:players (player/current)])]
      (do-commands
        command
        game-data
        player-data
        (get-in game-data [:rooms (:room player-data)])
        command-data))))


(defn new-vision-commands []
  (map->VisionCommands {}))


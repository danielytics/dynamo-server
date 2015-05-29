(ns dynamo.components.commands.vision
  (:require
    [dynamo.protocols.updates :as updates]
    [dynamo.protocols.commands :as commands]
    [dynamo.components.commands.movement :refer [direction-to-text]]
    [dynamo.tags :as t]  
    [dynamo.lib.commands :as cmds]
    [dynamo.lib.player :as player]))


(defn do-commands
  [command game-data player-data room-data {:keys [target]}]
    (case command
      :command/quick-look
        (player/reply! "You see " (:name room-data))
      :command/look
        (if target
          (player/reply! "You look at " target " and see nothing.")
          (player/reply!
            t/secure-line
            "You look around and you are in " (t/room-name (:name room-data)) ".\n"
            t/secure-line
            "You see " (t/room-description (:description room-data)) ".\n"
            (when-let [content (keys (:content room-data))]
              (str t/secure-line "There are " (clojure.string/join ", " content) " here.\n"))
            t/secure-line
            (if-let [exits (:exits room-data)]
              (t/exits "Obvious exits:" (map direction-to-text (keys exits)))
              "There are no obvious exits.\n")))
      :command/glance
        (if target
          (player/reply! "You glance at " target " and see nothing.")
          (player/reply! "What do you wish to glance at?")))
    game-data)


(defrecord VisionCommands []
  updates/RegisterMultiHandler
  (updates/update-ids [_]
    [:command/look
     :command/quick-look
     :command/glance])

  commands/Command 
  (commands/parser [_]
    {:look "<look-aliases> target?
            look-aliases = 'l' | 'look'"
     :glance "<glance-aliases> <'at'>? target?
              glance-aliases = 'gl' | 'glance'"})

  updates/Handler
  (updates/handle [c data command]
    (cmds/handle [c data command] do-commands)))


(defn new-vision-commands []
  (map->VisionCommands {}))


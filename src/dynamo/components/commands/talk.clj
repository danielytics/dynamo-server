(ns dynamo.components.commands.talk
  (:require
    [dynamo.protocols.updates :as updates]
    [dynamo.protocols.commands :as commands]
    [dynamo.lib.player :as player]))


(defrecord TalkCommands []
  updates/RegisterMultiHandler
  (updates/update-ids [_]
    [:command/say
     :command/shout
     :command/whisper
     :command/tantrum
     :error/invalid-command])

  commands/Command 
  (commands/parser [_]
    {:+whisper "<whisper-to-or-with> target message
                whisper-to-or-with = '' | 'to' | 'with'"
     :+say "say-optional-target message
            <say-optional-target> = '' | 'to' target"
     :+tantrum nil})


  updates/Handler
  (updates/handle [_ game-data [command command-data]]
    (if (= command :error/invalid-command)
      (println "Invalid:" command-data)
      (println "Performing" (name command) "for player" (player/current) ":" command-data))
    game-data))


(defn new-talk-commands []
  (map->TalkCommands {}))


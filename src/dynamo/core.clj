(ns dynamo.core
  (:require
    [com.stuartsierra.component :as component]  
    [clojure.core.async :as async]
    [dynamo.protocols.updates :as updates]
    [dynamo.tags :as t]  
    [dynamo.components.server :refer [new-server]]
    [dynamo.components.game-loop :refer [new-game-loop]]
    [dynamo.components.world :refer [new-world]]
    [dynamo.components.command-parser :refer [new-command-parser]]
    [dynamo.components.commands.talk :refer [new-talk-commands]]
    [dynamo.components.commands.vision :refer [new-vision-commands]]
    [dynamo.components.commands.movement :refer [new-movement-commands]]
    ))


#_(defn handle-command
  [u player command]
  (binding [t/*player* player]
    (let [[verb & other] (clojure.string/split command #" ")
          {:keys [room]} (:player player)]
      (case verb
        "act"   (case (first other)
                  "test" (updates/send! u :test (second other))
                  "foo"   (updates/send! u :foo (second other))
                  "bar"   (updates/send! u :bar (second other))
                  player)
        "look"  (let [{:keys [name description content exits]} (get rooms room)
                      noun (first other)]
                  (if noun
                    (if-let [noun-description (get content noun)]
                      (t/send-lines! :secure
                        (str "You look at " noun ".")
                        (str "you see "
                             (t/room-description noun-description)
                             "."))
                      (t/send-lines! :secure "You do not see that here."))
                    (t/send-lines! :secure
                      (str "You look around. You are in "
                           (t/room-name name)
                           ".")
                      (str "You see "
                           (t/room-description description)
                           ".")
                      (str "There are " (clojure.string/join ", " (keys content)) " here.")
                      (t/exits "Obvious exits:" (keys exits)))))
        (when-let [exit (get-in rooms [room :exits verb])]
          (handle-command
            (assoc-in player [:player :room] exit)
            "look"))))))


(defrecord TestCommandHandler []
  updates/RegisterHandler
  (update-id [_] :test)

  updates/Handler
  (handle [_ game-world command-data]
    (println "TestCommandHandler Handling :test with data:" command-data)))

(defn new-test-command-handler []
  (map->TestCommandHandler {}))


(defrecord FooBarCommandHandler []
  updates/RegisterMultiHandler
  (update-ids [_] [:foo :bar])

  updates/Handler
  (handle [_ game-world [command data]]
    (println "FooBarCommandHandler Handling" command "with data:" data)))

(defn new-foobar-command-handler []
  (map->FooBarCommandHandler {}))


(defn make-system
  [config]
  {:system  {:server          (new-server (:port config))
             :game-loop       (new-game-loop) 
             :game-world      (new-world)
             :command-parser  (component/using
                                (new-command-parser)
                                {:updater :game-loop})
             :cmds/talk       (new-talk-commands)
             :cmds/vision     (new-vision-commands)
             :cmds/move       (new-movement-commands)}
   :depends {:server          {:handler :command-parser}
             ;; Register all update handlers with the game-loop
             :game-loop       [:game-world
                               :cmds/talk :cmds/vision :cmds/move]
             ;; Register all command components
             :command-parser  [:cmds/talk :cmds/vision :cmds/move]}})


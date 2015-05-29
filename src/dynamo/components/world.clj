(ns dynamo.components.world
  (:require
    [com.stuartsierra.component :as component]  
    [dynamo.protocols.updates :as updates]))


(def players
  {0 {:name "bob"
      :description "This is a Bob, no doubt about it."
      :room 0}})


(def rooms
  {0    {:name "A Garden"
         :description "a well kept garden, sheltered by the large arches of the academy hall entrance"
         :content {"lawn" "a beautifully kept lawn, with short lush grass"
                   "path" "a polished stone-paved path"
                   "fountain" "a marble fountain"
                   "arches" "the arches rise high above you, to the north. They guard the entrance to the academy hall"
                   "academy" "the academy is in a large hall with an entrance hidden behind arches rising high into the sky"}
         :exits {"north" 1}
         :entities []
         }
  1     {:name "Arches"
         :description "huge stone arches, looming so high above that you cannot see their top"
         :content {"arches" "the arches are made from a bright stone and loom high above your head. You cannot see their top"
                   "garden" "there is a garden with a beautifully kept lawn and a marble fountain"}
         :exits {"south" 0}
         :entities []
         }})


(defrecord World []
  component/Lifecycle
  (start [component] component)
  (stop [component] component)
  
  updates/RegisterHandler
  (update-id [_] :game/init)

  updates/Handler
  (handle [_ game-data _]
    (assoc
      game-data
      :rooms    rooms
      :players  players)))


(defn new-world []
  (map->World {}))


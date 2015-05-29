(ns dynamo.system
  (:require
    [com.stuartsierra.component :as component]))


(defn new-system
  [system-ctor config]
  (let [{:keys [system depends]} (system-ctor config)] 
    (-> (component/map->SystemMap system)
        (component/system-using depends))))


(defn start-system
  [system-ctor]
  (component/start (new-system system-ctor {})))


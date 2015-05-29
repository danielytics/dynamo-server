(ns dynamo.components.game-loop
  (:require
    [com.stuartsierra.component :as component]  
    [clojure.core.async :as async]
    [dynamo.protocols.updates :as updates]
    [dynamo.components.server :refer [*connection*]]))


(defn- register-handlers
  "Package all components that satisy Handler or MultiHandler into a map:
    {update-id-kw handler-fn}"
  [component]
  (let [components (vals component)]
    (merge-with
      concat
      ;; Generate a map of all Handler instances
      (->>
        components
        (filter #(and (satisfies? updates/Handler %)
                      (satisfies? updates/RegisterHandler %)))
        ;; Create a vector of {update-id [handler-fn]} pairs
        (map (fn [h]
               {(updates/update-id h)
                [(fn [game-data [_ update-data]]
                   (updates/handle h game-data update-data))]}))
        (apply merge-with concat))
      ;; Generate a map of all MultiHandler instances
      (->>
        components
        (filter #(and (satisfies? updates/Handler %)
                      (satisfies? updates/RegisterMultiHandler %)))
        (map
          (fn [mh]
            (for [id (updates/update-ids mh)]
              {id [(partial updates/handle mh)]})))
        flatten
        (apply merge-with concat)))))


(defn- handle-forward
  [{:keys [handlers] :as component}
   game-world
   [_ {:keys [command data on-failure] :as x}]]
  (if (get handlers command)
    (updates/send! component command data)
    (updates/send! component :error/invalid-command command))
  game-world)


(defrecord GameLoop []
  component/Lifecycle
  (start [component]
    (let [chan                (async/chan)
          registered-handlers (register-handlers component)
          registered-handlers (assoc
                                registered-handlers
                                :update/forward
                                [(partial
                                  handle-forward
                                  (assoc component
                                         :cmd      chan
                                         :handlers registered-handlers))])]
      ;; Receive and process updates to the game world in a loop
      (async/go-loop [game-world {}]
        (when-let [[conn update-id data] (async/<! chan)]
          ;; Perform updates against the world
          (let [handlers (get registered-handlers update-id)]
            (recur
              (binding [*connection* conn]
                (reduce
                  (fn [game-world handler]
                    (handler game-world [update-id data]))
                  game-world
                  handlers))))))

      ;; Store the channel for future access
      (let [component (assoc component
                        :handlers register-handlers
                        :cmd chan)] 
        ;; A second from now, tell all components that want to init the game-world
        ;; to do so
        (async/go (async/<! (async/timeout 1000))
                  (println "Sending init")
                  (updates/send! component :game/init nil))
        component)))

  (stop [{:keys [cmd] :as component}]
    ;; Close the update channel, which will terminate the update loop
    (when cmd (async/close! cmd))
    (dissoc component :cmd))
  
  updates/Updater
  (send! [component update-id data]
    ;; Asynchronously send an update to be processed by the update loop
    (async/go
      (async/>! 
        (:cmd component)
        [*connection* update-id data]))))


(defn new-game-loop []
  (map->GameLoop {}))


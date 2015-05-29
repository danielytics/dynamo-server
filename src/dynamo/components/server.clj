(ns dynamo.components.server
  (:require
    [com.stuartsierra.component :as component]  
    [manifold.deferred :as d]
    [manifold.stream :as s]
    [clojure.edn :as edn]
    [aleph.tcp :as tcp]
    [clansi.core :as c]
    [dynamo.lib.comms :refer [put! put-bytes! decode]]
    [dynamo.lib.player :refer [*current-player*]]
    [dynamo.tags :as t]
    [dynamo.protocols.request-handler :as rh]))


(defn make-handler
  [req-handler]
  (fn [s info]
    (put-bytes! s [0xff 0xfb 0x5b 0x00])

    (d/loop [state {:id           (-> (rand) (* 100000) int)
                    :preferences  {:mxp?        false
                                   :auto-look?  false}
                    :player-id    0
                    :stream       s}]
      ;; take a message, and define a default value that tells us if the connection is closed
      (-> (s/take! s ::none)
        (d/chain
          ;; Step one: decode to string, if possible
          (fn [coded-msg]
            (if (= ::none coded-msg)
              [::closed nil]
              (try
                [::command (decode coded-msg)]
                (catch Exception e
                  [::continue coded-msg]))))
          ;; Step two: apply state-machine-specific rules to msg
          (fn [[command msg]]
            (case command
              ::continue  (if (= (vec msg)
                                 (vec (byte-array [0xff 0xfd 0x5b]))) ; Telnet negotiation
                            (do
                              (t/init s)
                              (assoc-in state
                                        [:preferences :mxp?]
                                        true))
                            state)
              ::command   (d/future
                            (cond
                              (= msg "prefs set auto-look?")
                                (assoc-in state [:preferences :auto-look?] true)
                              (= msg "prefs unset auto-look?")
                                (assoc-in state [:preferences :auto-look?] false)
                              (= msg "prefs set mxp?")
                                (assoc-in state [:preferences :mxp?] true)
                              (= msg "prefs unset mxp?")
                                (assoc-in state [:preferences :mxp?] false)
                              (= msg "prefs list")
                                (do
                                  (put! s
                                        (apply str "Preferences:\n"
                                          (for [[pref value] (:preferences state)]
                                            (str "  " (name pref) "\t" (when-not value "un") "set\n"))))
                                  state)
                              :else
                                (binding [*current-player* state]
                                  (rh/handle req-handler state msg))))
              nil))
          ;; Step three: write output
          (fn [state]
            (when state
              (when-let [response (:response state)]
                (put! s response))
              (d/recur state))))
        ;; if there were any issues on the far end, send a stringified exception back
        ;; and close the connection
        (d/catch
          (fn [ex]
            (println "ERROR:" ex)
            (s/put! s "You have been disconnected.")
            (s/close! s)))))))


#_(defn make-handler
  [updater]
  (fn [s info]
    (d/loop []
      (->
        (d/let-flow [msg (s/take! s ::none)]
          (when-not (= ::none msg)
            (d/let-flow [msg'   (d/future (f msg))
                         result (s/put! s msg')]
              (when result
                (d/recur)))))
        (d/catch
          (fn [ex]
            (s/put! s (str "ERROR: " ex))
            (s/close! s)))))))



(defrecord Server [port handler]
  component/Lifecycle
  (start [component]
    (assert (satisfies? rh/RequestHandler handler))
    (assoc
      component
      :server
      (tcp/start-server
        (make-handler handler)
        {:socket-address
          (java.net.InetSocketAddress. "0.0.0.0" port)})))

  (stop [{:keys [server]}]
    (when server
      (.close server))))


(defn new-server
  [port]
  (map->Server {:port port}))


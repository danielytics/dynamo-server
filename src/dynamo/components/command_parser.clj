(ns dynamo.components.command-parser
  (:require
    [com.stuartsierra.component :as component]  
    [instaparse.core :as insta] 
    [dynamo.protocols.request-handler :as request-handler]
    [dynamo.protocols.updates :as updates]
    [dynamo.protocols.commands :as commands]))


;; Built in rules for use in command grammars
(def builtin-rules
  {:target  "word"
   :message "text"})


;; Parser to skip whitespace between tokens
(def whitespace
  (insta/parser
    "whitespace = #'\\s+'"))


(defn maybe-strip
  "If string starts with a +, strip it"
  [string]
  (if (= \+ (first string))
    (subs string 1)
    string))


(defn generate-command-rules
  "Taking a map of command rules, generate an instaparse compatible rules string"
  [commands]
  (for [[k v] commands]
    (let [k (name k)
          k? (= \+ (first k))
          k (maybe-strip k)]
      (str k " = "
           (when k? (str "<'" k "'> " (when-not v "end-of-command")))
           v "\n"))))


(defn make-grammar
  "Given a map of command rules, generate a complete instaparse grammar"
  [commands]
  (str
    "cmd = "
    (clojure.string/join " | " (map (comp maybe-strip name key) commands))
    "\n"
    "<end-of-command> = <#'$'>\n"
    "space = #'\\s+'\n"
    "text = #'[^ ].+'\n"
    "word = #'[^\\s]+'\n"
    (apply str (generate-command-rules builtin-rules))
    (apply str (generate-command-rules commands))))


(defn make-parser
  "Given a grammar as a staing, generate a parser"
  [grammar]
  (insta/parser grammar :auto-whitespace whitespace))


(defn make-command-list
  "Take a component and retrieve command rules from any dependency components
   that satisfy the Command protocol"
  [component]
  (reduce
    (fn [cmd-list cmd]
      (if (satisfies? commands/Command cmd)
        (merge cmd-list (commands/parser cmd))
        cmd-list))
    {}
    (vals component)))


(defn process-command
  "Given a parsed command, send an appropriate update to handle the command"
  [updater command [raw args] raw-data]
  (if-not (instance? clojure.lang.IRecord command)
    ;; Send the command to update game data
    (updates/send!
      updater
      ;; Generate an update id: :command/<parsed-command-id>
      (->> command first name (keyword "command"))
      (into
        ;; Store raw arguments as string
        {:_raw raw-data}
        ;; Make sure arguments were parsed in pairs
        (map (fn [[k & [v]]]
               [k (or v)])
             ;; Rest of parsed data is arguments
             (next command))))
    ;; Could not parse command, try simple split
      (updates/send!
        updater
        :update/forward
        {:command (keyword "command" raw)
         :data    {:text raw-data :tokens args}
         :on-failure :command/unknown})))


(defrecord CommandParser [updater]
  component/Lifecycle
  (start [component]
    (let [command-list (make-command-list component)]
      ;; If the command list is not empty, generate a parser and add it to the
      ;; component for later access
      (if-not (empty? command-list)
        (let [grammar (make-grammar command-list)
              parser  (make-parser grammar)]
          (assoc component
                 ;; TODO: commands and grammar only need to be stored in :dev
                 ;; profile
                 :commands  (into [] (map (comp maybe-strip name)
                                          (keys command-list)))
                 :grammar   grammar
                 :parser    parser))
        component)))

  (stop [component]
    (dissoc component :parser :grammar :parser))
  
  ;; Handle TCP requests by parsing the request string with the command parser
  ;; and routing the result as an update
  request-handler/RequestHandler
  (handle [{:keys [parser]} connection request]
    (when parser
      ;; Parse the request
      (let [command   (->> (parser request)
                           (insta/transform
                             {:cmd  identity
                              :text identity
                              :word identity}))]
        ;; Process the parsed command
        (process-command
          updater
          command
          (clojure.string/split request #"\s+")
          (second (clojure.string/split request #"\s+" 2)))))
    ;; Return the connection to keep it open
    connection))


(defn new-command-parser []
 (map->CommandParser {}))


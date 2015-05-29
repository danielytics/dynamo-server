(ns dynamo.tags
  (:require
    [dynamo.comms :refer [put! put-bytes!]]))


(def mxp-elements
  ["<!ELEMENT RName '<FONT COLOR=Red><B>' FLAG=\"RoomName\">"
   "<!ELEMENT RDesc FLAG='RoomDesc'>"
   "<!ELEMENT RExits '<FONT COLOR=Blue>' FLAG=\"RoomExit\">"
   "<!ELEMENT Ex '<SEND>'>"
   "<!ELEMENT Chat '<FONT COLOR=Gray>' OPEN>"
   "<!ELEMENT Gossip '<FONT COLOR=Cyan>' OPEN>"
   "<!ELEMENT ImmChan '<FONT COLOR=Red,Blink>'>"
   "<!ELEMENT Auction '<FONT COLOR=Purple>' OPEN>"
   "<!ELEMENT Group '<FONT COLOR=Blue>' OPEN>"
   "<!ELEMENT Prompt FLAG=\"Prompt\">"
   "<!ELEMENT Hp FLAG=\"Set hp\">"
   "<!ELEMENT MaxHp FLAG=\"Set maxhp\">"
   "<!ELEMENT Mana FLAG=\"Set mana\">"
   "<!ELEMENT MaxMana FLAG=\"Set maxmana\">"]
   #_"<RName>The Main Temple</RName>"
   #_"<RDesc>This is the main hall of the MUD where everyone starts."
   #_"Marble arches lead south into the town, and there is a <i>lovely</i>"
   #_"<send 'drink &text;'>fountain</send> in the center of the temple,</RDesc>"
   #_"<RExits>Exits: <Ex>N</Ex>, <Ex>S</Ex>, <Ex>E</Ex>, <Ex>W</Ex></RExits>"
   #_"<Prompt>[<Hp>100</Hp>/<MaxHp>120</MaxHp>hp <Mana>50</Mana>/<MaxMana>55</MaxMana>mana]</Prompt>")

(def open-line- (str \u001b "[0z"))
(def secure-line- (str \u001b "[1z"))


(defn init
  "Initialise MXP for a user by sending element definitions"
  [s]
  ;; Enable MXP
  (put-bytes! s [0xff 0xfa 0x5B 0xff 0xf0])
  ;; Send element definition
  (doseq [element mxp-elements]
    (put! s (str secure-line- element "\n"))))


(defn- gen-attrs
  [attrs]
  (when-not (empty? attrs)
    (->>
      attrs
      (map (fn [k v] (str k "=\"" v "\"")))
      (clojure.string/join " ")
      (str " "))))


(defn- tagged
  "Tag data with MXP element"
  ([player tag data]
    (tagged player tag nil data))
  ([{:keys [preferences]} tag conf data]
    (if (:mxp? preferences)
      (str "<" tag (gen-attrs conf) ">" data "</" tag "/>")
      data)))

(def ^{:dynamic true} *player* nil)

(defn- make-tag
  [tag]
  (fn [& [conf text]]
    (let [[conf text] (if text [text text] [nil conf])
          player *player*]
      (tagged player tag conf text))))


(def room-name        (make-tag "RName"))
(def room-description (make-tag "RDesc"))
(def em               (make-tag "i"))

(defn exits
  [prefix exits]
  (->>
    exits
    (map (partial tagged *player* "Ex"))
    (clojure.string/join ", ") 
    (str prefix " ")
    (tagged *player* "RExits")))



(defn lines
  [type & lines]
  (let [prefix (if (= type :secure) secure-line- open-line-)]
    (apply
      str
      (for [line lines]
        (str prefix line "\n")))))


(defn send-lines!
  [type & lines-to-send]
  (let [{:keys [socket]} *player*]
    (put! socket (apply lines type lines-to-send))
    *player*))


(defn send-secure
  "Send a secure line of MXP data"
  [{:keys [socket preferences]} line]
  (put! socket (str (when (:mxp? preferences) secure-line-) line "\n")))


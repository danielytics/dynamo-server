(ns dynamo.lib.comms
  (:require
    [gloss.core :as gloss]
    [gloss.io :as io]
    [manifold.stream :as s]))


(def protocol
  (gloss/compile-frame
    (gloss/string :us-ascii :delimiters ["\n" "\r" "\r\n"])))


(defn put!
  [s value]
  (s/put! s (io/encode protocol value)))


(defn put-bytes!
  [s bytes]
  (s/put! s (byte-array bytes)))

(defn decode
  [encoded]
  (io/decode protocol encoded))


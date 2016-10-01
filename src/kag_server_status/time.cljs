(ns kag-server-status.time)


(defn get-time []
  (.now js/Date))


(defn fresh? [time-limit since]
  (< (- (get-time) since) time-limit))


(defn string->timestamp [string]
  (.parse js/Date string))
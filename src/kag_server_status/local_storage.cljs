(ns kag-server-status.local-storage
  (:require [cljs.reader :as reader]))


(defn has-item? [key]
  (.hasOwnProperty js/localStorage key))


(defn get-item [key]
  (reader/read-string (.getItem js/localStorage key)))


(defn set-item [key value]
  (.setItem js/localStorage key (pr-str value)))
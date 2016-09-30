(ns kag-server-status.http-utils
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [kag-server-status.time :as time]
            [kag-server-status.local-storage :as ls]))


(defn keep-fresh [time-limit cache]
  (select-keys
    cache
    (for [[key {time :time}] cache
          :when (time/fresh? time-limit time)] key)))


(defn make-requester [namespace time-limit fun]
  (fn [cont & params]
    (when-not
      (ls/has-item? namespace)
      (ls/set-item namespace {}))
    (let [cache (ls/get-item namespace)
          key (pr-str params)]
      (if
        (and
          (contains? cache key)
          (time/fresh? time-limit (:time (get cache key))))
        (cont (:value (get cache key)))
        (apply fun
               (concat [(fn [value]
                          (let [time (time/get-time)
                                cache (ls/get-item namespace)
                                fresh-cache (keep-fresh time-limit cache)
                                updated (assoc fresh-cache key {:value value :time time})]
                            (ls/set-item namespace updated)
                            (cont value)))]
                       params))))))


(defn make-generic-requester [namespace time-limit]
  (make-requester
    namespace
    time-limit
    (fn [cont url params]
      (go
        (cont (:body (<! (http/get url params))))))))
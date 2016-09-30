(ns kag-server-status.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [reagent.core :as r]
            [kag-server-status.http-utils :refer [make-generic-requester]]))


(enable-console-print!)


(defn official? [server]
  (some
    #(.includes (:serverName server) %)
    ["KAG Official"]))


(defn populated? [server]
  (pos? (:currentPlayers server)))


(def state (r/atom (hash-map)))
(def timer (r/atom 0))


(def get-servers-unbound
  (make-generic-requester "servers" (* 5 60 1000)))


(defn get-servers [cont]
  (get-servers-unbound
    cont
    "https://api.kag2d.com/servers"
    {:with-credentials? false}))


(def get-status
  (make-generic-requester "status" (* 30 1000)))


(defn request-status []
  (reset! state {})
  (get-servers
    (fn [servers]
      (let [filtered
            (->> (:serverList servers)
                 (filter official?)
                 (filter populated?)
                 (map #(select-keys
                        %
                        [:serverName :currentPlayers :serverIPv4Address :serverPort])))]
        (doseq [server filtered]
          (get-status
            #(when
              (pos? (-> % :serverStatus :connectable))
              (reset!
                state
                (assoc
                  @state
                  (-> % :serverStatus :serverName)
                  (-> % :serverStatus))))
            (str
              "https://api.kag2d.com/server/ip/"
              (:serverIPv4Address server)
              "/port/"
              (:serverPort server)
              "/status")
            {:with-credentials? false}))))))


(defn tick []
  (when
    (pos? @timer)
    (swap! timer dec)
    (js/setTimeout tick 1000)))


(defn server-list []
  [:div.container
   [:ul
    (for [[name status] @state]
      ^{:key name}
      [:li
       [:div
        (:serverName status)]
       [:div
        "players " (:currentPlayers status) "/" (:maxPlayers status)]])]
   [:button
    {:on-click
     #(when
       (zero? @timer)
       (reset! timer 30)
       (request-status)
       (tick))
     :disabled (pos? @timer)}
    "Refresh"
    (if (zero? @timer) "" (str " in " @timer " seconds"))]])


(defn render []
  (r/render-component [server-list]
                      (.getElementById js/document "app")))


(render)
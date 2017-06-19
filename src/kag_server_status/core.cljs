(ns kag-server-status.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<!]]
            [reagent.core :as r]
            [kag-server-status.time :as time]
            [kag-server-status.http-utils :refer [make-generic-requester]]))


(enable-console-print!)


(defn official? [server]
  (and
    (not (nil? (:serverName server)))
    (some
      #(.includes (:serverName server) %)
      ["KAG Official"])))


(defn populated? [server]
  (pos? (:currentPlayers server)))


(defn recent? [server]
  (time/fresh?
    (* 8 60 60 1000)
    (time/string->timestamp (:lastUpdate server))))


(def state (r/atom (hash-map)))
(def timer (r/atom 0))
(def candidates (r/atom nil))
(def pinged (r/atom 0))


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
  (reset! candidates nil)
  (reset! pinged 0)
  (get-servers
    (fn [servers]
      (let [filtered
            (->> (:serverList servers)
                 (filter official?)
                 (filter recent?)
                 (filter populated?)
                 (map #(select-keys
                        %
                        [:serverName :currentPlayers :serverIPv4Address :serverPort])))]
        (reset! candidates (count filtered))
        (doseq [server filtered]
          (get-status
            #(when
              (pos? (-> % :serverStatus :connectable))
              (reset!
                state
                (assoc
                  @state
                  (-> % :serverStatus :serverName)
                  (-> % :serverStatus)))
              (swap! pinged inc))
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
       [:div.players
        {:class (if (>= (:currentPlayers status) (:maxPlayers status)) "full" nil)}
        (:currentPlayers status) "/" (:maxPlayers status)]])]
   [:button
    {:on-click
     #(when
       (zero? @timer)
       (reset! timer 30)
       (request-status)
       (tick))
     :disabled (pos? @timer)}
    (cond
      (nil? @candidates) "fetching server list"
      (< @pinged @candidates) (str "pinged " @pinged " / " @candidates)
      (pos? @timer) (str "cooling down in " @timer " seconds")
      :else "refresh")
    ]])


(defn render []
  (r/render-component [server-list]
                      (.getElementById js/document "app")))


(reset! timer 30)
(request-status)
(tick)


(render)
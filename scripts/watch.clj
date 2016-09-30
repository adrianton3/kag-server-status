(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'kag-server-status.core
   :output-to "out/kag_server_status.js"
   :output-dir "out"})

(ns reschedul2.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [reschedul2.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[reschedul2 started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[reschedul2 has shut down successfully]=-"))
   :middleware wrap-dev})

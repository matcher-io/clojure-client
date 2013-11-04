(ns
  ^{:doc "general utility functions"
    :author "manzur"}
  io.matcher.utils
  (:use io.matcher.config)
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(defn make-request [action properties capabilities match ttl & [version]] 
  (let [version (or version default-version)]
    {
     :action action
     :properties properties
     :capabilities capabilities
     :match match
     :ttl  ttl
     :version version
     }))

(def place-request 
  (partial make-request "PLACE"))

(defn update-request [id properties capabilities match ttl & [version]]
  (def update-fn (partial make-request "UPDATE"))
  (let [request (update-fn properties capabilities match ttl version)
        result (merge request {:request id})]
    result))

(defn retract-request [id & [version]]
  (let [version (or version default-version)]
    {
     :action "RETRACT"
     :request id
     :version version
    }))

(defn payload->json [payload]
  (json/read-str (String. payload "UTF-8") :key-fn keyword))

(defn make-counting-listener [count promise]
  (let [counter (atom count) results (atom '())]
	  (fn [t m] 
	     (let [c (swap! counter dec)]
          (swap! results conj m)
          
          (when (= c 0) 
            (log/debug "counting listener reached 0 " @results)
            (deliver promise (reverse @results)))))))

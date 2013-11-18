(ns ^{:doc "api-library for using Matcher"} 
  io.matcher.client
  (:use io.matcher.config)
  (:require [io.matcher.utils      :as utils]
            [io.matcher.amqp-utils :as aqutils]
            [langohr.channel       :as lch]
            [langohr.core          :as lcore]
            [langohr.queue         :as lq]
            [langohr.consumers     :as lc]
            [langohr.basic         :as lb]
            [clojure.tools.logging :as log]
            [clojure.data.json     :as json]))

(def ^:dynamic *-transactor-* (atom nil))

(defmacro with-matcher 
  [transactor & actions]
  `(binding [*-transactor-* ~transactor]
     ~@actions))

(defprotocol Matcher
  (request-sync-with-listener [this request listener])
  (request-sync [this request])
  (close [this]))

(defn place
  "PLACEs request asynchronously on the matcher. Callback is called when MATCH or TIMEOUT occurs."
  [& {:keys [properties capabilities match ttl callback] :or {ttl default-ttl}}]
  (let [request (utils/place-request properties capabilities match ttl)]
    (request-sync-with-listener *-transactor-* request callback)))

(defn update
  "UPDATEs the request(placed earlier) asyncrhonously with the give id."
  [& {:keys [id properties capabilities match ttl] :or {ttl default-ttl}}]
  (let [request (utils/update-request id properties capabilities match ttl)]
    (request-sync *-transactor-* request)))

(defn retract
  "RETRACTs the request asynchronously with the given id."
  [id]
  (let [request (utils/retract-request id)]
    (request-sync *-transactor-* request)))

(defn do-requests
  "Asynchrounously executes all the given requests and executes callback after all"
  [requests callback]
  (dorun
    (map #(request-sync *-transactor-* %) requests)))

(defn make-delivery-handler [callback-listeners match-listeners]
  (fn [channel metadata ^bytes payload]
    (let [{:keys [type]} metadata 
          content (utils/payload->json payload)]
      
      (if (or (= type "confirm")
              (= type "match"))
        (do 
          (let [{:keys [correlation-id]} metadata]
            
            (log/debug (str "type: " type " response: " content))
            
            (when-let [listener (@callback-listeners correlation-id)]
              (swap! callback-listeners dissoc correlation-id)
              (listener content))
            
            
            (log/debug "Metadata:" metadata "; Correlation-id: " correlation-id "--------->" @match-listeners)
            
            (when-let [listener (@match-listeners correlation-id)]
              (log/debug "+++TYPE: " type)
              (when (= type "match")
                (swap! match-listeners dissoc correlation-id)
                (listener content)))
            
            ))
            
;            (if-let [listener (@listeners correlation-id)]
;              (do
;                (swap! listeners dissoc correlation-id)
;                (listener content))
;              (log/warn (str "unknown correlation id: " correlation-id)))))
        
        ;default action
        (log/debug (str "undefined message type: " type " " content))))))


(defn matcher
  "creates Matcher implementation for the given connection, input queue name and match-listener, which is call on matching"
  [connection output-queue-name]
  (let [queue-name default-input-queue
        channel (lch/open connection)
        input-queue (aqutils/make-queue channel queue-name) 
        output-queue (aqutils/make-queue channel output-queue-name)
        correlation (atom 0)
        callback-listeners (atom {}) match-listeners (atom {})
        publish (fn [request callback match] 
                  (let [correlation (str (swap! correlation inc))
                        request (json/write-str (assoc request :match_response_key output-queue-name))]

                    (when callback
                      (swap! callback-listeners assoc correlation callback))
                    
                    (when match
                      (swap! match-listeners assoc correlation match))
                    
                    (lb/publish channel "" queue-name request 
                                :content-type "application/json" 
                                :type "request" 
                                :reply-to output-queue-name
                                :correlation-id correlation)
                    correlation))]
        
    (lb/consume channel output-queue-name
                (lc/create-default channel 
                                   :handle-delivery-fn (make-delivery-handler callback-listeners match-listeners))
                :auto-ack true)
    
    (reify Matcher

      (close [_]
        (lq/delete channel output-queue)
        (lch/close channel))
      
      (request-sync-with-listener [this request match-listener]
        (log/debug (str "request: " request))
        (let [result (promise)
              listener (fn [response]
                         (deliver result response))
              correlation-id (publish request listener match-listener)]
          (let [result (deref result 5000 :timeout)]
            (when (= result :timeout)
              (swap! callback-listeners dissoc correlation-id))                 
            result)))
              
      (request-sync [this request]
        (let [result (promise)
              listener (fn [response]
                         (deliver result response))
              correlation-id (publish request listener nil)]
          
          (let [result (deref result 5000 :timeout)]
            (when (= result :timeout)
              (swap! callback-listeners dissoc correlation-id))                 
            result))))))
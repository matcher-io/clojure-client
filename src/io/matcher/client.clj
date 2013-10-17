(ns io.matcher.client
  (:require 
    [langohr.channel   :as lch]
    [langohr.queue     :as lq]
    [langohr.consumers :as lc]
    [langohr.basic     :as lb]
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
  )
)


(defn make-request [action properties capabilities match ttl] 
  (let [structure 
       {
         :action action
         :properties properties
         :capabilities capabilities
         :match match
         :ttl  ttl
       }]

     structure  
  )
)

(def place-request 
  (partial make-request "PLACE"))

(def update-request 
  (partial make-request "UPDATE"))

(defn retract-request [request]
  {:action "RETRACT"
  :request request})



(defprotocol Transactor
   (request-async [this request listener])
   (request-sync [this request])
   (close [this])  
)

(defn transactor [connection in-queue match-listener]
   (let [channel (lch/open connection) queue (.getQueue (lq/declare channel)) correlation (atom 0) listeners (atom {}) transactor (atom nil)]
      (lb/consume channel queue
	       (lc/create-default channel 
	                         :handle-delivery-fn 
	                            (fn [ch metadata ^bytes payload]
                                (let [type (:type metadata) json (json/read-str (String. payload "UTF-8") :key-fn keyword)]
                                   (cond
                                     (= type "confirm")
                                       (let [correlation-id (:correlation-id metadata)]
                                         (log/debug (str "response: " json))
	                                       (if-let [listener (@listeners correlation-id)]
                                           (do
	                                           (swap! listeners dissoc correlation-id)
	                                           (listener @transactor json))
                                           (log/warn (str "unknown correlation id: " correlation-id))
	                                       )
                                       )
                                       
                                     (and (= type "match"))
                                       (do
                                         (log/debug (str "match: " json))
                                       
                                         (when match-listener
                                           (match-listener @transactor json)))
                                       
                                     :else
                                        (log/debug (str "undefined message type: " type " " json))
                                        
                                   )
                                )
                              )
	                         
                           )       
      :auto-ack true)
      
      (reset! transactor
	      (reify Transactor
	        (close [_]
	          (lq/delete channel queue)
	          (lch/close channel)
	        )
	        
	        (request-async [_ request listener]
             (log/debug (str "request: " request))
	           (let [correlation (str (swap! correlation inc)) request (assoc request :match_response_key queue)]
	             
	           (swap! listeners assoc correlation listener)  
	             
		         (lb/publish channel "" in-queue (json/write-str request) 
		            :content-type "application/json" 
		            :type "request" 
		            :reply-to queue
		            :correlation-id correlation)     
	           )
	           
	           correlation
	        )
	        
	        (request-sync [this req]
	          (let [result (promise)]
	             (let [correlation-id
	                (request-async this req 
	                   (fn [response]
	                      (deliver result response)))]
	            
	               (let [result (deref result 5000 :timeout)]
	                 (when (= result :timeout)
	                   (swap! listeners dissoc correlation-id))
	
	                 result                 
	               )
	             )
	          )
	        )
	      ))
   )  
)



(defn request-async-multi [transactor callback requests]
  (dorun
    (map #(request-async transactor % callback) requests)))


(defn place-one [transactor request]
   (request-sync transactor request))

(defn retract-one [transactor id]
   (request-sync transactor (retract-request id)))


(defn make-counting-listener [count promise]
  (let [counter (atom count) results (atom '())]
	  (fn [t m] 
	     (let [c (swap! counter dec)]
          (swap! results conj m)
          
          (when (= c 0) 
            (log/debug "counting listener reached 0 " @results)
            (deliver promise (reverse @results))
          )
	     )
	  )
  )
)


(defn place-many [transactor requests]
   (let [confirms (promise)]
      (request-async-multi transactor 
        (make-counting-listener (count requests) confirms) 
           requests)
      
      confirms
   )
)

(defn retract-many [transactor ids]
  (let [confirms (promise)]
    (request-async-multi transactor 
      (make-counting-listener (count ids) confirms) 
         (map retract-request ids))
     
    confirms    
  )
)

  


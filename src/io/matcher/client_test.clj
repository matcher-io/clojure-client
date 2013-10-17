(ns io.matcher.client-test
  (:require
    [langohr.core   :as rmq]
    [io.matcher.client :as matcher]
    [clojure.tools.logging :as log]
  )
)

(defonce connection 
  (rmq/connect 
     {:uri "amqp://matcherserver:I4FkqPnsgtCo@amqp.suprematic.net:5672/matcher" :requested-heartbeat 10}))


(defn test-place-request [id]
  (let [age (rand-int 100) position (rand-int 100) price (rand-int 100) class (rand-int 100)]
	  [
	     (matcher/place-request 
	          { ; Properties
	             :name (str "Lucie" id) 
	          }
	      
	          {
	             :id id
	             :type 0
	             :age age
	             :position position
	          }
	          
	         (str "type == 1 and price == " price " and class == " class " and id == " id)
           
           (* 600 1000)
	      )
	     
	      (matcher/place-request
	          { ; Properties
	              :name (str "ticket" id) 
	          }
	          
	          {
	             :id id
	             :type 1
	             :price price
	             :class class
	          }
	          
	          (str "type == 0 and age == "  age " and position == " position " and id == " id)
	          
	          (* 600 1000)
	      )
	  ]
	  )
)
  
(defn make-pairs [npairs]
  (map #(test-place-request %) (range npairs)))

  
(defn make-counting-listener [count promise]
  (let [counter (atom count) results (atom '())]
	  (fn [t m] 
	     (let [c (swap! counter dec)]
          (swap! results conj m)
          
          (when (= c 0)
            (deliver promise (reverse @results)))
	     )
	  )
  )
)


(defn test-retract [cnt]
  (let [pairs (make-pairs cnt)
        persons (map first pairs)
        tickets (map second pairs)

        transactor (matcher/transactor connection "match_input_queue" nil)
       ]
    
     (let [placed (matcher/place-many transactor persons) 
           placed-ids (map :request @placed)
          ; retracts (deref (retract-many transactor placed-ids) 5000 :timeout)
           retracts  (matcher/retract-many transactor placed-ids)
           ]
       
        ;(matcher/close transactor)
        
        nil
     )
  )
)  


(defn test-place [npairs]
  (let [results (promise)
    transactor (matcher/transactor connection "match_input_queue" nil)]

    (deref 
      (matcher/place-many transactor 
          (shuffle (flatten (make-pairs npairs)))) 5000 :timeout)

    (matcher/close transactor)
  )  
)











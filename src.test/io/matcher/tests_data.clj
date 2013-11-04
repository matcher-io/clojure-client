(ns io.matcher.tests-data)

(def place-request-1 
  (let [money 70000 location "Tashkent" hand-wheel "left"]
    {:properties {:name "Manzur"}
     :capabilities {:money money :location location :type "human"}
     :match (str "handwheel == 'left'") 
     }))

(def place-request-2
  (let [price 60000 hand-wheel "left"]
    {:properties {:name "Saab 919"}
     :capabilities {:price price :handwheel hand-wheel :type "car"}
     :match ""
     }))

(def place-request-3
  (let [price 70000 direction "left"]
    {:properties {:name "BMW"}
     :capabilities {:price price :direction direction :type "car"}
     :match "type == 'human'"
     }))

(def place-request-4
  (let [money 10000 location "Glasgow" direction "right"]
    {:properties {:name "Bob"}
     :capabilities {:money money :location location :direction direction :type "human"}
     :match (str "price <= " money " and type == 'car'")
     }))

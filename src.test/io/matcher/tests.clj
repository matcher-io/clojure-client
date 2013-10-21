(ns io.matcher.tests
  (:use io.matcher.config 
        clojure.test)
  (:require [io.matcher.client :as mt]
            [clojure.string    :as str]
            [langohr.core      :as lc]
            [clojure.tools.logging :as log]))

(defn matched-listener [content]
  (let [status (:status content)
        [code _] (str/split status #" ")]
    (is (= code "253"))))

(defn timeout-listener [content]
  (let [status (:status content)
        [code _] (str/split status #" ")]
    (is (= code "408"))))
                              
(def placeRequest1 
  (let [money 60000 location "Tashkent" direction "left"]
    {:properties {:name "Manzur"}
     :capabilities {:money money :location location :direction direction}
     :match "price <= 60000"
     }))

(def placeRequest2
  (let [price 60000 direction "left"]
    {:properties {:name "Saab 919"}
     :capabilities {:price price :direction direction}
     :match ""
     }))

(def placeRequest3
  (let [price 70000 direction "left"]
    {:properties {:name "BMW"}
     :capabilities {:price price :direction direction}
     :match ""
     }))


(defn test-place[]
  (let [connection (lc/connect CONNECTION_OPTIONS)
        {p1 :properties c1 :capabilities m1 :match} placeRequest1
        {p2 :properties c2 :capabilities m2 :match} placeRequest2
        {p3 :properties c3 :capabilities m3 :match} placeRequest3]
    
    (mt/with-matcher connection matched-listener
      (mt/place :properties p1 :capabilities c1 :match m1)
      (mt/place :properties p2 :capabilities c2 :match m2))
    
    (mt/with-matcher connection timeout-listener
      (mt/place :properties p1 :capabilities c1 :match m1)
      (mt/place :properties p3 :capabilities c3 :match m3))))

(test-place)

(run-tests)
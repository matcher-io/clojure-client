(ns io.matcher.tests
  (:use io.matcher.config 
        clojure.test)
  (:require [io.matcher.client :as mt]
            [clojure.string    :as str]
            [langohr.core      :as lc]
            [clojure.tools.logging :as log])
  (:import [java.util.concurrent CountDownLatch]))


(defn- status-code [content]
  (let [status (:status content)
        [code _] (str/split status #" ")]
    (Integer/valueOf code)))

(defn- extract-name [content]
  (let [name (or (get-in content [:request :properties :name])
                 (get-in content [:properties :name]))]
    name))

(defn match-listener [testsCount sucesses fails]
  (fn [content]
    (let [code (status-code content)
          name (extract-name content)]      
      (if (= code 253)
        (is (contains? sucesses name))
        (is (contains? fails name)))
      (.countDown testsCount))))
      
(def placeRequest1 
  (let [money 70000 location "Tashkent" hand-wheel "left"]
    {:properties {:name "Manzur"}
     :capabilities {:money money :location location :hand-wheel hand-wheel :type "human"}
     :match (str "direction == left") 
     }))

(def placeRequest2
  (let [price 60000 hand-wheel "left"]
    {:properties {:name "Saab 919"}
     :capabilities {:price price :hand-wheel hand-wheel :type "car"}
     :match ""
     }))

(def placeRequest3
  (let [price 70000 direction "left"]
    {:properties {:name "BMW"}
     :capabilities {:price price :direction direction :type "car"}
     :match "type == human"
     }))

(def placeRequest4 
  (let [money 10000 location "Glasgow" direction "right"]
    {:properties {:name "Bob"}
     :capabilities {:money money :location location :direction direction :type "human"}
     :match (str "price <= " money " and type == car")
     }))

(deftest test-place
  (testing "Testing place"
           (let [TESTS_COUNT (CountDownLatch. 2)
                 connection (lc/connect CONNECTION_OPTIONS)
                 listener (match-listener TESTS_COUNT #{"Manzur" "Saab 919"} #{"BMW" "Bob"})
                 transactor (mt/transactor connection listener)
                 ]
             
             (mt/with-matcher transactor listener 
               (let [{p1 :properties c1 :capabilities m1 :match} placeRequest1
                     {p2 :properties c2 :capabilities m2 :match} placeRequest2
                     {p3 :properties c3 :capabilities m3 :match} placeRequest3
                     {p4 :properties c4 :capabilities m4 :match} placeRequest4]
                 
                 (mt/place :properties p1 :capabilities c1 :match m1)
                 (mt/place :properties p2 :capabilities c2 :match m2)
                 
                 (mt/place :properties p3 :capabilities c3 :match m3)
                 (mt/place :properties p4 :capabilities c4 :match m4)))
             (.await TESTS_COUNT)
             (log/debug "END"))))
                 
(run-tests)
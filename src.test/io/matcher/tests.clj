(ns io.matcher.tests
  (:use io.matcher.config
        io.matcher.tests-data
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
      
(deftest test-place
  (testing "Testing place"
           (let [TESTS_COUNT (CountDownLatch. 2)
                 connection (lc/connect CONNECTION_OPTIONS)
                 listener (match-listener TESTS_COUNT #{"Manzur" "Saab 919"} #{"BMW" "Bob"})
                 transactor (mt/transactor connection "matcher_output1" listener)
                 ]
             
             (mt/with-matcher transactor
               (let [{p1 :properties c1 :capabilities m1 :match} placeRequest1
                     {p2 :properties c2 :capabilities m2 :match} placeRequest2
                     {p3 :properties c3 :capabilities m3 :match} placeRequest3
                     {p4 :properties c4 :capabilities m4 :match} placeRequest4]
                 
                 (mt/place :properties p1 :capabilities c1 :match m1)
                 (mt/place :properties p2 :capabilities c2 :match m2)
                 
                 (mt/place :properties p3 :capabilities c3 :match m3)
                 (mt/place :properties p4 :capabilities c4 :match m4)))
             (.await TESTS_COUNT)
             (mt/close transactor))))


(deftest test-update
  (testing "Testing update"
           (let [TESTS_COUNT (CountDownLatch. 1)
                 connection (lc/connect CONNECTION_OPTIONS)
                 listener (match-listener TESTS_COUNT #{"BMW" "Bob"} #{}) 
                 transactor (mt/transactor connection "matcher_output1" listener)
                 ]
             
             (mt/with-matcher transactor
               (let [{p3 :properties c3 :capabilities m3 :match} placeRequest3
                     {p4 :properties c4 :capabilities m4 :match} placeRequest4
                     id3 (get (mt/place :properties p3 :capabilities c3 :match m3) :id)
                     id4 (get (mt/place :properties p4 :capabilities c4 :match m4) :id)
                     c3New (merge c3 {:price 10000})]
                 
                 (is id3)
                 (when id3 
                   (mt/update :id id3 :properties p3 :capabilities c3New :match m3))
                 (.await TESTS_COUNT)
                 (mt/close transactor))))))

(deftest test-retract
  (testing "Testing retract"
           (let [TESTS_COUNT (CountDownLatch. 1)
                 connection (lc/connect CONNECTION_OPTIONS)
                 listener (match-listener TESTS_COUNT #{"BMW" "Bob"} #{}) 
                 transactor (mt/transactor connection "matcher_output1" listener)
                 ]
             (mt/with-matcher transactor
               (let [{p1 :properties c1 :capabilities m1 :match} placeRequest1
                     {p2 :properties c2 :capabilities m2 :match} placeRequest2
                     id1 (get (mt/place :properties p1 :capabilities c1 :match m1) :id)]
                 (mt/retract id1)
                 (mt/place :properties p2 :capabilities c2 :match m2)
                 (.await TESTS_COUNT)
)))))
                 ;                 (mt/close transactor))))))

(run-tests)
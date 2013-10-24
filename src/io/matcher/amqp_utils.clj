(ns
  ^{:doc "ampq specific utils"}
  io.matcher.amqp-utils
  (:require [langohr.queue :as lq]))

(defn make-queue [channel queueName]
  (.getQueue (lq/declare channel queueName :auto-delete false)))
(ns
  ^{:doc "ampq specific utils"}
  io.matcher.amqp-utils
  (:require [langohr.queue :as lq]))

(defn make-queue [channel queue-name]
  (.getQueue (lq/declare channel queue-name :auto-delete false)))
(ns
  ^{:doc "default config values for client"} 
  io.matcher.config)

(defonce ^:const default-version "1.0")
(defonce ^:const default-connection-options {
                                             :host "localhost" 
                                             :port 5672 
                                             :username "guest" 
                                             :password "guest" 
                                             :vhost "/"
                                             })

(defonce ^:const amqp-suprematic 
  {
   :uri "amqp://matcherserver:I4FkqPnsgtCo@amqp.suprematic.net:5672/matcher" 
   :requested-heartbeat 10
   })


(defonce connection-options default-connection-options)

(defonce default-ttl (* 1000 10))

(defonce default-input-queue "matcher_input")

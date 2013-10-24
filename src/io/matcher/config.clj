(ns
  ^{:doc "default config values for client"} 
  io.matcher.config)

(defonce ^{:const true} DEFAULT_VERSION "1.0")
(defonce ^{:const true} DEFAULT_CONNECTION_OPTIONS {
                                                    :host "localhost" 
                                                    :port 5672 
                                                    :username "guest" 
                                                    :password "guest" 
                                                    :vhost "/"
                                                    })

(defonce ^{:const true} AMQP_SUPREMATIC 
  {
   :uri "amqp://matcherserver:I4FkqPnsgtCo@amqp.suprematic.net:5672/matcher" 
   :requested-heartbeat 10
   })


(defonce CONNECTION_OPTIONS DEFAULT_CONNECTION_OPTIONS)

(defonce DEFAULT_TTL (* 1000 10))

(defonce IN_QUEUE "matcher_input")
(defonce OUT_QUEUE "matcher_output")

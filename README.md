## *How-to: developing drivers for matcher*

### Table of contents
- [Requirements](#requirements)
- [Basics](#basics)
- [Place requests](#place-requests)
- [Update requests](#update-requests)
- [Retract requests](#retract-requests)
- [Response types](#response-types)
- [Matcher query language](#matcher-query-language)

### Requirements:
- Erlang (see Matcher requirements)
- RabbitMQ (see Matcher requirements)
- Matcher

### Interaction with Matcher happens via:
- AMQP protocol 
- Erlang

This manual concentrates on communication over AMQP.

### Basics

Client communicates with Matcher by sending requests, after processing the request Matcher sends response to the client via AMQP.  
Request and responses all are in JSON format. 

#### The request types are:
- PLACE - places the request in the matcher, so that it can be matched further
- UPDATE - updates the already posted request
- RETRACT - retracts/deletes the request that was posted before

### PLACE requests
Let's suppose that Alice wants to buy a car which costs less than 30000, has red color and left hand wheel. Sample request:  
```javascript

        {
            "action": "PLACE",
            "properties": {"name" : "Alice"},
            "capabilities": {"id" : 1, "money" : 30000, "favorite-color" : "red"},
            "match" : "price < 30000 and color == red and hand-wheel == left",
            "ttl" : 10000,
            "version": "1.0",
            "match_response_key": "matcher_output"
        }
```
Let's see each of the field one by one:
    action - is the type of request;  
    properties - is property of the object which is posted(not used in matching);  
    capabilities - capabilities of the object that can be used for matching;  
    match - request that describes the critirias of desired object (see [Matcher query language](#matcher-query-language) section);  
    ttl - time to leave(in microseconds). After given time request is deleted from Matcher;  
    version - version of Client-Matcher protocol. Currently: "1.0";  
    match_response_key - name of the queue where to return response(recommended it to keep the same with AMQP reply-to header);  

After placing request Matcher gives response like this:  
    250 PLACED ....
        
If the other side posts requests on selling cars - Matcher will try to find the compatible match for the Alice's request. Let's imagine that the following request was posted onto the Matcher:  

```javascript        
        {
            "action": "PLACE",
            "properties": {"name" : "Saab 919"},
            "capabilities": {"id" : 2, "price" : 20000, "color" : "red"},
            "match" : "",
            "ttl" : 10000,
            "version": "1.0",
            "match_response_key": "matcher_output"
        }
```

The request is almost same as the upper on except that match field is empty string, so that car doesn't have any requirements on the buyer.    
    
When Matcher finds two compatible requests it sends acknowledgement to both sides. Something like this:  
```javascript        
        {
            "version": "1.0",
            "status": "253 MATCHED", 
            "request": {
                            "id": "undefined", 
                            "capabilities": {
                                                "price": 30000, 
                                                "hand-wheel": "left", 
                                                "color" : "red"
                                            },
                            "properties" :  {"name: "Saab 919"}
                        },
            "match":   {
                            "id": "256_667c15e2aaae28bb7657578b45ab163a", 
                            "capabilities": {
                                                "money": 60000, 
                                                "hand-wheel" : "left",
                                                "favorite-color": "red"
                                            }, 
                            "properties": {"name": "Alice"}
                        }
        }
```
            
In case that no match found in time-to-live period, then response is this:
```javascript
        {
            "version": "1.0", 
            "status" : "408 TIMEOUT", 
            "request": "512_667c16ea8a52fcd245037187826267e6", 
            "properties":  {:name "Saab 919"}
        }
```
            
### UPDATE requests
The UPDATE requests are almost the same as PLACE requests but also have id("request" field) of the previously posted request:  
```javscript

        {
            "action": "UPDATE",
            "request":"512_667c16ea8a52fcd245037187826267e6", 
            "properties": {"name" : "Saab 919"},
            "capabilities": {"id" : 2, "price" : 20000, "color" : "grey"},
            "match" : "",
            "ttl" : 10000,
            "version": "1.0",
            "match_response_key": "matcher_output"
        }
```

### RETRACT requests
The RETRACT is used for deleting prevously posted messages:

```javascript
        {
            "action": "RETRACT",
            "request":"512_667c16ea8a52fcd245037187826267e6", 
            "version": "1.0",
            "match_response_key": "matcher_output"
        }
```
### Response types
Matcher can return the following response codes:  
- 251 UPDATED   
- 252 RETRACTED    
- 253 MATCHED  
- 404 NOT FOUND  
- 400 BAD REQUEST  
- 408 TIMEOUT  
- 409 PLACE TIMEOUT  
- 410 UPDATE TIMEOUT  
- 411 RETRACT TIMEOUT  
- 420 RETRACT ERROR  
- 500 SERVER ERROR  
        
### Matcher query language
Currently matcher supports following comparision operators: <, >, <=, >=, !=, ==, and, or.  
Operator "and" has bigger priority than operator "or".

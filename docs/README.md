# SMS Scheduler - Technical Documentation

For an overview of the SMS Scheduler, see the top-level [README](../README.md).


## Development

@TODO@ describe setting up dev environment  
@TODO@ include diagram for scheduler and blocker processes 

#### Configuration

@TODO@


## Interfacing

#### REST API

@TODO@

#### Messages

@TODO@


## Deployment

@TODO@


---

## Development Notes

* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.4/maven-plugin/reference/html/)
* [Flowable Event Registry](https://blog.flowable.org/2020/02/08/introducing-the-flowable-event-registry/) and
  [Processing Kafka Events in Flowable](https://blog.flowable.org/2020/03/24/flowable-business-processing-from-kafka-events/)
* If you want to use your own inbound channel, then you can use expression as the type and set adapterDelegateExpression to
  your own @KafkaListener. Your bean needs to then implement InboundEventChannelAdapter which will get the channel.


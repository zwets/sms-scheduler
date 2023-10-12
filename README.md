# sms-scheduler

_Secure and reliable scheduled delivery of SMS messages_

**WORK IN PROGRESS**


## Background

This SMS scheduler was built to fulfil the requirements of health research
projects with strong privacy and reliability requirements.  In particular:

 * Phone numbers and message content must remain confidential, even to an
   adversary with physical access to the system
 * Delivery of SMS must be maximally reliable, subject to the (notorious)
   unreliability of SMS.
 * Each SMS has its own schedule, a sequence of time slots within which
   delivery will be attempted, until it succeeds or expires.

Additional requirements:

 * Multi-tenancy: multiple independent clients can concurrently use it
 * Reporting: clients can query for the status of scheduled SMSes

Overview of Mechanism:

 * Client encrypts the SMS payload, which includes both destination number
   and message content, with the public key of the backend SMS gateway
 * Client hands payload with delivery schedule to SMS Scheduler, as well as
   an optional unique key to later refer to this scheduled SMS
 * The SMS Scheduler, not having the private key, has no access to the
   plaintext payload
 * At the scheduled time, SMS Scheduler forwards the payload as-is to the
   backend SMS Gateway
 * The backend SMS Gateway decrypts the SMS and transmits to the SMSC;
   it reports delivery notifications from the SMSC back to SMS Scheduler
 * SMS Scheduler may reschedule if it is notified of non-delivery, taking
   into account the SMS delivery schedule

Overview of Implementation

 * The SMS Scheduler is a standalone WAR that can either be deployed on
   Tomcat or run standalone on Java 17+ using its embedded Tomcat server
 * It is built on top of the [Flowable](https://flowable.org) process
   engine using Spring Boot 3.
 * A database (PostgreSQL or other) is required for maintaining process
   state
 * A backend SMS Gateway is required to handle the actual transmission of
   the SMS to the SMSC; communication is over a Kafka topic
 * Clients submit requests over REST or a Kafka topic


## Technical Details

@TODO@


#### License

sms-scheduler - Secure and reliable scheduled delivery of SMS messages
Copyright (C) 2023  Marco van Zwetselaar

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


---
## Development

* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.4/maven-plugin/reference/html/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)


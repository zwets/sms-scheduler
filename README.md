# sms-scheduler

_Secure and reliable scheduled delivery of SMS messages_

See also the related [SMS Gateway](https://github.com/zwets/sms-gateway).


## Architecture Overview

### Requirements

This SMS scheduler was built to fulfil the requirements of health research
projects with strong privacy and reliability requirements.  In particular:

 * Phone numbers and message content must remain confidential, even to an
   adversary with physical access to the system
 * Delivery of SMS must be maximally reliable, subject to the (notorious)
   unreliability of SMS.
 * Each SMS has its own schedule, a sequence of time slots within which
   delivery will be attempted, until it succeeds or expires.

**Additional requirements**

 * Multi-tenancy: multiple independent clients can concurrently use the
   system
 * Reporting: clients can query the status of scheduled SMSes
 * Cancellation: a client can request cancellation of a scheduled SMS, of
   all SMS for a specific target, or all its scheduled SMS
 * Blocking: a client can request that all future requests to a specific
   target ID will be ignored
 * API: all functionality of the scheduler is exposed to clients over REST;
   one-way requests (schedule, cancel, block) can also be sent over Kafka

### Overview of Mechanism

 * Client encrypts the SMS payload, which includes both destination number
   and message content, with the public key of the backend SMS gateway
 * Client hands payload with delivery schedule to SMS Scheduler, as well
   as two optional keys to later refer to the (group of) SMS:
   * Target ID, a "moniker" for the destination/receiver of the SMS
   * Instance ID, to reference this specific SMS send
 * The SMS Scheduler, not having the private key, has no access to the
   plaintext payload, it only knows about the schedule
 * At the scheduled time, SMS Scheduler forwards the payload as-is to the
   backend [SMS Gateway](https://github.com/zwets/sms-gateway).
 * The backend SMS Gateway decrypts the SMS and transmits to the SMSC;
   it reports delivery notifications from the SMSC back to SMS Scheduler
 * SMS Scheduler may reschedule if it is notified of non-delivery, taking
   into account the SMS delivery schedule

### Overview of Infrastructure

 * The SMS Scheduler is a standalone WAR that can either be deployed on
   Tomcat or run standalone on Java 17+ using its embedded Tomcat server
 * The scheduler consumes requests from Kafka topic `schedule-sms` or from
   REST requests; it can be queried over REST.
 * It is built on top of the [Flowable](https://flowable.org) process
   engine using Spring Boot 3.
 * A database (PostgreSQL or other) maintains process state
 * The backend [SMS Gateway](https://github.com/zwets/sms-gateway) handles
   the actual transmission of the SMS to the SMSC
   * Requests to the Gateway go out (fire-forget) on Kafka topic `send-sms`
   * Gateway sends asynchronous notifications (delivery, failure) in on
     Kafka topic `sms-status`


## Technical Documentation

See the [docs](docs) directory.


---

#### Licence

sms-scheduler - Secure and reliable scheduled delivery of SMS messages  
Copyright (C) 2023  Marco van Zwetselaar <io@zwets.it>

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


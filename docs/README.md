# SMS Scheduler - Technical Documentation

For an overview of the SMS Scheduler, see the top-level [README](../README.md).


## Development

@TODO@ describe setting up dev environment (including BPMN editor)
@TODO@ include diagrams for scheduler and blocker processes 

#### Configuration

See [Running](#running) below.  The `dev` profile is for development, and
has `src/test/resources/application-dev.properties` for configuration.

#### Building & unit test

A `mvn package` should do the trick.


## Interfacing

#### REST API

@TODO@

#### Messages

Messages on the Kafka topic `schedule-sms` are defined in `eventregistry`
directory under `src/main/resources`.  Here for reference:

```json
{ 
    "action": "schedule",
    "client": the ID of the client (tenant), e.g. "test"
    "target": opaque ID for the recipient, assigned by the client
    "key": client-defined ID to identify this request (or a group)
    "schedule": slots in which the message may be forwarded
    "message": the base64 encoded payload
}
```


## Running

See the scripts in the `bin` directory, and [deployment](#deployment) below.

 * `run-boot-test.sh` (development) runs code directly with `test` profile
 * `run-scheduler-test.sh` runs the WAR/JAR with the `test` profile activated

### Configuration

Configuration is in `src/main/application*.properties`.  Use Spring Boot's
[Externalised Configuration](https://docs.spring.io/spring-boot/docs/3.0.4/reference/html/features.html#features.external-config)
to override settings at run time by dropping a file `application.properties`
(or separate files `application-{prod,test}.properties`) in your PWD or 
PWD/config.

The default active profile set in `application.properties` is `dev`, which
is not in the JAR hence you must activate either the `test` or `prod` profile
when running the application.


## Testing

#### Unit test

Unit tests are included.  Since testing scheduled events is tricky, what we do
in the `dev` (and optionally `test`) profiles is to set very short timeouts
and use backend mocks.

@TODO@ add tests that use Flowable's mutable wall clock.

#### Mock Server

In `test` (integration) and `prod` deployment, i.e. when connected to the SMS
Gateway backend, use the `test` client (tenant) ID.  The SMS Scheduler treats
requests from this client like all others, but the SMS Gateway has special
content-based routing, and requests will never be sent for real.

See the [documentation for SMS Gateway](https://github.com/zwets/sms-gateway)
for instructions.

#### Test Client

The `test-client` directory has scripts for both the IAM (account management)
and message scheduling endpoints, over both REST and Kafka.

See [test-client/README.md](test-client/README.md) for details.


## Deployment

This section describes deployment to production.

#### Requirement: SMS Gateway

We assume the SMS Gateway is up and running on its server and the Kafka topics
to (`send-sms`) and from it (`sms-status`) are available on the broker.

#### Requirement: PostgreSQL

In `dev` Flowable uses an in-mem database by default.  In `test` the default
is an on-disc H2 database.  In `prod` we prefer a PostgreSQL database.

Install and configure a PostgreSQL server according to best practices.

    # E.g. on an 8 core 16GB machine with SSD, I use:
    max_connections = 64            # max(4*cores,100) but we can do with less
    shared_buffers = 1280MB         # allocated at all times (less than 1/4 of RAM)
    work_mem = 64MB                 # increase with complexity but times workers
    maintenance_work_mem = 256M     # only eaten occasionally 512MB fine too
    effective_cache_size = 8GB      # memory available under normal load (1/2 RAM)
    random_page_cost = 1.0          # on SSD

Create user and database:

    sudo -u postgres createuser -P smes  # Copy password to application.properties
    sudo -u postgres createdb -O smes smes_prod

Test that the `smes` user can connect to the database:

    psql -U smes -W -h localhost smes_prod

### First time installation

Create the installation directory `/opt/sms-scheduler`

    mkdir /opt/sms-scheduler && cd /opt/sms-scheduler
    cp sms-scheduler-${VERSION}.war .
    ln -sf sms-scheduler-${VERSION}.war sms-scheduler.jar

Create the `smes` user and group:

    adduser --system --gecos 'SMS Scheduler' --group --no-create-home --home /opt/sms-scheduler smes

Create config dir

    mkdir config &&
    chown root:smes config &&
    chmod 0750 config

Add `application-prod.properties` to the config dir and read-protect it:

    # This creates an empty file; easier to copy it from the repo or JAR
    touch config/application-prod.properties &&
    chown root:smes config/application.properties &&
    chmod 0640 config/application-prod.properties

    # Now edit config/application-prod.properties to set at least:
    - database connection
    - kafka broker

Create the Kafka incoming topic (`schedule-sms`)

    BOOTSTRAP_SERVER='localhost:9092'  # or what you set in application properties
    kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --create --if-not-exists --topic schedule-sms

First time manual run to create the database

    cd /opt/sms-scheduler
    sudo -u smes java -jar sms-scheduler.jar --spring.profiles.active=prod

If all goes well, stop the running service with `ctrl-C`.  To restart from scratch,
recreate the database (`sudo -u postgres dropdb smes_prod`) and recreate as above.

Create the systemd service by editing `etc/sms-scheduler.service` and symlinking
this into `/etc/systemd/system`

    systemctl enable /opt/sms-scheduler/etc/sms-scheduler.service
    systemctl start sms-scheduler

To see and follow the logging output

    sudo journalctl -xeu sms-scheduler
    sudo journalctl -fu sms-scheduler

#### Set up the test-client

Before exposing the service to public internet, we need to remove the default
`admin:test` account.  This requires the test-client to be installed on the machine:

    # As your own user (not root)
    mkdir ~/src
    git clone -C ~/src https://github.com/zwets/sms-scheduler

Create the defaults file:

    cd ~/src/sms-scheduler/test-client/lib
    cp defaults.example defaults

    # Edit: defaults
    #keep the default user (admin) and password (test) for now

Check that the REST interface works

    cd ~/src/sms-scheduler/test-client/admin
    ./account-list   # shows just the 'admin' account

Create an admin account for yourself

    # This creates account USERNAME as a member of all three current groups
    # Make sure the PASSWORD is long and complex (you will store it in defaults!)
    ./create-account USERNAME YOUR_FULL_NAME EMAIL PASSWORD admins users test

Now edit the defaults file again, to have the username and password you created:

    # Edit ~/src/sms-scheduler/test-client/lib/defaults
    DEFAULT_UNAME=...
    DEFAULT_PWORD=...

Then protect it

    chmod 0750 ~/src/sms-scheduler/test-client/lib
    chmod 0640 ~/src/sms-scheduler/test-client/lib/defaults

Now *remove* the admin user

    ./account-list
    ./account-delete admin


Create

Make 
Also install the `sms-client` 

#### Requirement: Apache reverse proxy

In `dev` and `test` we use `http` connections by default.  In `prod` we front
the application with an Apache reverse proxy.

Install and configure an Apache2 web server according to best practices.  In
particular, set up **https** because the REST calls use basic auth by default.

@TODO@ migrate to certificate-based authentication (we need them anyway on
the clients).

Make the proxy set the `X-Forwarded-For` and `X-Forwarded-Proto` headers.  The
scheduler's application.properties needs `server.use-forward-headers=true`, which
we set by default.

The SMS Scheduler port is set with the `server.port` property (built-in is 8082).

#### Requirement: open firewall port

On the web server, remember to open the port to the Apache2 proxy and ascertain
that the port to SMS Scheduler (or any other component) is closed.

@TODO@ better yet would be a VPN from the client(s) to the server.

#### Requirement: test-client

In order to test the installation 



## Implementation Notes

#### Development Notes

* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.4/maven-plugin/reference/html/)
* [Flowable Event Registry](https://blog.flowable.org/2020/02/08/introducing-the-flowable-event-registry/) and
  [Processing Kafka Events in Flowable](https://blog.flowable.org/2020/03/24/flowable-business-processing-from-kafka-events/)
* If you want to use your own inbound channel, then you can use expression as the type and set adapterDelegateExpression to
  your own @KafkaListener. Your bean needs to then implement InboundEventChannelAdapter which will get the channel.


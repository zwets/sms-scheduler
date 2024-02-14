# SMS Scheduler - Technical Documentation

For an overview of the SMS Scheduler, see the top-level [README](../README.md).


## Development

@TODO@ describe setting up dev environment (including BPMN editor)
@TODO@ include diagrams for scheduler and blocker processes 

#### Configuration

@TODO@

#### Building

@TODO@

#### Testing

@TODO@


## Interfacing

#### REST API

@TODO@

#### Messages

@TODO@


## Running

See the scripts in the `bin` directory, and [deployment](#deployment) below.

 * `run-boot-test.sh` (development) runs code directly with `test` profile
 * `run-scheduler-test.sh` runs the WAR/JAR with the `test` profile activated

### Configuration

Drop a file `application.properties` (or separate files
`application-{prod,test}.properties`) in your PWD or PWD/config, and these
will be picked up.

The Spring Boot docs for [Externalised Configuration](https://docs.spring.io/spring-boot/docs/3.0.4/reference/html/features.html#features.external-config)
have all the details.


## Testing

#### Unit test

Unit tests are included.  Since testing scheduled events is tricky, what we do
in the `dev` (and optionally `test`) profiles is to set very short timeouts
and use backend mocks.

@TODO@ add tests that use Flowable's mutable wall clock.

#### Mock Server

In `test` (integration) deployment, i.e. when connection to the SMS Gateway,
the convenient way to test the scheduler is by sending requests from the "test"
client (tenant).  The scheduler treats these requests just like any other
request, but in the SMS Gateway backend the "test" client requests have
special content-based routing and never will be sent for real.

See the [documentation for SMS Gateway](https://github.com/zwets/sms-gateway)
for instructions.

#### Test Client

The `test-client` directory has scripts for both the IAM (account management)
and message scheduling endpoints, over both REST and Kafka.

See [test-client/README.md](test-client/README.md) for details.


## Deployment

#### Requirements: PostgreSQL

In `dev` Flowable uses an in-mem database by default.  In `test` use either
that or (for persistency across restarts) an on-disc SQLite3 database.
In `prod` we use a PostgreSQL database.

Install and configure a PostgreSQL database according to best practices.

    # E.g. on a 8 core 16GB machine with SSD
    max_connections = 64            # max(4*cores,100) but we can do with less
    shared_buffers = 1280MB         # allocated at all times (less than 1/4 of RAM)
    work_mem = 64MB                 # increase with complexity but times workers
    maintenance_work_mem = 256M     # only eaten occasionally 512MB fine too
    effective_cache_size = 8GB      # memory available under normal load (1/2 RAM)
    random_page_cost = 1.0          # on SSD

Create the database and user:

    sudo -u postgres createuser -P smes  # password in application.properties
    sudo -u postgres createdb -O smes smes_prod

Test that you can connect to the database:

    psql -U smes -W -h localhost smes_prod

#### Requirements: Apache reverse proxy

In `dev` and `test` we use `http` connections by default; in `prod` I prefer
fronting the application with an Apache reverse proxy.

Install and configure an Apache2 web server according to best practices.  In
particular, make sure it runs over **https** because for the REST calls we
use basic authentication (by default).

@TODO@ migrate to certificate-based authentication (we need them anyway on
the client)

Make the proxy set `X-Forwarded-For` and `X-Forwarded-Proto` headers and the
scheduler application.properties have `server.use-forward-headers=true` (set
in the built-in `application-prod.properties`).

Set the server port with `server.port=...` (built-in set to 8082).


#### Requirement: open firewall ports

Recommend using high ports and only opening them to IP addresses where REST
calls may originate from.

@TODO@ better yet would be a VPN from the client(s) to the server.

#### First time installation

Create the installation directory `/opt/sms-scheduler`

    mkdir /opt/sms-scheduler && cd /opt/sms-scheduler
    cp sms-scheduler-${VERSION}.war .
    ln -sf sms-scheduler-${VERSION}.war sms-scheduler.jar

Create the `smes` user and group it will run as

    adduser --system --gecos 'SMS Scheduler' --group --no-create-home --home /opt/sms-scheduler smes

Create config dir

    mkdir config &&
    chown root:smes config &&
    chmod 0750 config

Add the application properties to config

    vi config/application-prod.properties &&
    chmod 0640 config/application-prod.properties

Create the Kafka incoming topic

    BOOTSTRAP_SERVER='localhost:9092'
    kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --create --if-not-exists --topic schedule-sms

Set up the PostgreSQL database

    @TODO@

Set up Apache Reverse Proxy

    @TODO@

Open ports in UFW / proxy over Apache!

    @TODO@


Create the systemd service by editing `etc/sms-scheduler.service` and copying or
symlinking into `/etc/systemd/system`

    systemctl enable etc/sms-scheduler.service
    systemctl start sms-scheduler

To see and follow the logging output

    sudo journalctl -xeu sms-scheduler
    sudo journalctl -fu sms-scheduler

Open ports in UFW

    # Depending on what you set in application.properties


## Implementation Notes

#### Development Notes

* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.1.4/maven-plugin/reference/html/)
* [Flowable Event Registry](https://blog.flowable.org/2020/02/08/introducing-the-flowable-event-registry/) and
  [Processing Kafka Events in Flowable](https://blog.flowable.org/2020/03/24/flowable-business-processing-from-kafka-events/)
* If you want to use your own inbound channel, then you can use expression as the type and set adapterDelegateExpression to
  your own @KafkaListener. Your bean needs to then implement InboundEventChannelAdapter which will get the channel.


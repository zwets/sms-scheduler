README for the 'lib' directory

This directory holds the configuration and common functionality of all the
test-client scripts.

CREATE THE defaults FILE

In order to use the test-client scripts, you need to create a file 'defaults'
in this directory.

Use the file 'defaults.example' as the template, and change the settings to
suit your local setup (broker address, etc).

ADD PUBLIC KEYS

The schedule-sms scripts need to encrypt the payload for the backend, so they
need the backend's public keys for the clients you will be testing with.  You
can pass these on the command-line, but it will be much more convenient to
drop them in this directory.

The 'sms-client' tools can be used to obtain the public key for each client
for the gateway's vault.


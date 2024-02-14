In this directory drop the public keys registered in the SMS Gateway vault.
For each client, name the file ${CLIENT}.pub.  The schedule-sms scripts in
../../{rest,kafka} will then find them automatically when they need to
encrypt the SMS payload for a specific client.

See the sms-client and sms-gateway repositories for instructions on how to
retrieve the public keys.  Note also that sms-client can be used to create
encrypted SMS messages on the fly.


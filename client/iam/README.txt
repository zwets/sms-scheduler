README.txt for the iam scripts

We use this common setup (more details in the source code):

 - users have *accounts*

 - accounts can be assigned to *roles*:
   - users: regular users
   - admins: accounts that can do IAM
   - (none): an account that is not in either can't do much

 - accounts can be assigned to *clients*
   - test: the built-in testing client (see the docs)
   - ...: any other clients you create
   - (none): an account that has no client can't submit/query

Client "groups" can be created and deleted by admins.

Roles are static and come in exactly two flavours: users, admins

REMEMBER when creating a new user to always add the account to
(at least) the 'users' group.  Otherwise the user cannot login.

ALSO remember to add the new account to the client(s) it will be
operating for, or it will get 403 FORBIDDENs.


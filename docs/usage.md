# Usage of the MongoDB resource

When a MongoDB Operator is installed in a Kubernetes cluster, it watches `MongoDB` custom resources.
Creating a `MongoDB` resource in a namespace triggers that a database user for resource is created.

To request a MongoDB database with associated user in a cluster with the MongoDB Operator, a
resource like the following must be applied:

```yaml
apiVersion: persistence.sda-se.com/v1beta1
kind: MongoDb
metadata:
  name: my-db
  namespace: test-namespace
spec:
  database:
    pruneAfterDelete: true # optional, default false
  secret:
    databaseKey: d # optional, default 'database'
    usernameKey: u # optional, default 'username'
    passwordKey: p # optional, default 'password'
```

This will create a database named `test-namespace_my-db` and the user `test-namespace_my-db` with
read-write access to that database and a secret named `my-db` in `test-namespace`.
The secret will provide the data `d: test-namespace_my-db`, `u: test-namespace_my-db` and
`p: <random-password>` (with base64 encoded values).

When the MongoDB resource is deleted, the database user and the secret are deleted as well.
If `spec.database.pruneAfterDelete` is true, the whole database with all content will be deleted.

With an appropriate Kustomize configuration (similar to the configuration required for Sealed
Secrets), databases created this way can be used in PR deployments with name suffix.


## Caveats

* `spec.database.pruneAfterDelete: true` is only supported if the user of the MongoDB Operator is
  allowed to drop databases.
* Other settings than available in the secret for the database instance are not covered by the
  MongoDB Operator yet.
  `host`, `options`, etc. must be requested from the DevOps team of each Kubernetes cluster.
* The `authSource` is the allowed database itself for MongoDB instances and the admin database for
  DocumentDB instances. As DocumentDB uses the admin database for all users, there is no need to
  configure `authSource`.

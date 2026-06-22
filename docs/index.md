# MongoDB Operator

A Kubernetes Operator that creates users and databases in existing MongoDB clusters based on a
custom resource.

Based on the resource name, a secret is created that contains the dynamically generated database
name, username, password and connection string.

The operator uses a write-only model for Kubernetes Secrets.
It creates Secrets for new `MongoDb` resources, but does not read existing Secrets.

When the operator is installed in a cluster, a database can be requested with a MongoDB resource
[as shown on the usage page](usage.md).

The MongoDB Operator is tested with MongoDB and DocumentDB.

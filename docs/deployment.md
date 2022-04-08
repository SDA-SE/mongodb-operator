# Deployment of the MongoDB Operator

## MongoDB Operator Requirements

To function properly, some requirements in the Kubernetes deployment must be met, and a MongoDB
database instance must be set up.


### Kubernetes

An [example deployment of the MongoDB Operator](https://github.com/SDA-SE/mongodb-operator/tree/master/kustomize/bases/operator)
that covers the following requirements is available in the GitHub Repository of the MongoDB
Operator.


#### Service Account

The MongoDB Operator requires a `ServiceAccount` with some privileges for the Kubernetes API from a
`ClusterRole`:

* For the resource `mongodbs` the following verbs are required:
    * `watch`
    * `list`
    * `get`
    * `update`
* For the resource `mongodbs/status` the following verbs are required:
    * `watch`
    * `list`
    * `get`
    * `update`
* For the resource `secrets` the following verbs are required:
    * `create`
    * `update`
* For the resource `customresourcedefinitions` with the resource name
  `mongodbs.persistence.sda-se.com` the following verbs are required:
    * `get`


#### Custom Resource Definition

The [CRD `mongodbs`](https://github.com/SDA-SE/mongodb-operator/tree/master/kustomize/bases/operator/mongodbs-crd.yaml)
must be applied.


### Database    

A MongoDB database instance or a AWS DocumentDB cluster must be set up separately from the
deployment of the MongoDB Operator.

A user for MongoDB Operator must be created.

The user of the MongoDB Operator must be granted
[`userAdminAnyDatabase`](https://docs.mongodb.com/v4.4/reference/built-in-roles/#mongodb-authrole-userAdminAnyDatabase)
to function properly.

[`dbAdminAnyDatabase`](https://docs.mongodb.com/v4.4/reference/built-in-roles/#mongodb-authrole-dbAdminAnyDatabase)
is needed to support `spec.database.pruneAfterDelete: true` of the MongoDB custom resource.
`pruneAfterDelete` is suggested for develop and test environments only where pull request or
temporary test deployments could create a big amount of temporary used databases.


## MongoDB Operator Image

The image is hosted at [quay.io/sdase/mongodb-operator](https://quay.io/repository/sdase/mongodb-operator).

### Base Image

This container is based on the distroless [SDA OpenJDK base image](https://quay.io/repository/sdase/openjdk-runtime).
The base image provides both manual and automatic ways to configure memory limits of the JVM.

### Environment Variables

The following environment variables can be used to configure the Docker container:

#### Java

* `JAVA_TOOL_OPTIONS` _string_
    * Set options for the JVM.
      If Java options are set, you will find _Picked up JAVA_TOOL_OPTIONS: "-Xmx340m"_ or similar in
      the log.
    * Example: `-Xmx340m` to set the max heap size

#### MongoDB

When connecting to the MongoDB, the configured hosts are checked.
If any ends with `.docdb.amazonaws.com`, a connection to AWS DocumentDB is assumed, and the behavior
slightly changes.
While users for requested databases are created in the database when using MongoDB, all users will
be created in the `admin` database when AWS DocumentDB is used.

* `MONGODB_CONNECTION_STRING` _string_
    * The connection String that covers all configuration to access the MongoDB database.
    * Example: `mongodb://username:password@mongodb.mongodb:27017`
* `TRUSTED_CERTIFICATES_DIR`
    * Directory in the container where CA certificates or certificate bundles in PEM format can be
      mounted. These certificates are used to verify SSL connections to the database.
      The configuration is ignored if no files are mounted.
      Startup will fail if the directory is not readable.
    * Default: `/var/trust/certificates`

### Endpoints

The image exposes port `8081` for monitoring purposes.

It provides the following endpoints:

* Readiness: `http://{serviceUrl}:8081/health/readiness`
* Liveness: `http://{serviceUrl}:8081/health/liveness`

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
    connectionStringOptions: "" # optional, defaults to the ones used by MongoDB operator
  secret:
    databaseKey: d # optional, default 'database'
    usernameKey: u # optional, default 'username'
    passwordKey: p # optional, default 'password'
    connectionStringKey: c  # optional, default 'connectionString'
```

This will create a database named `test-namespace_my-db` and the user `test-namespace_my-db` with
read-write access to that database and a secret named `my-db` in `test-namespace`.
The secret will provide the data `d: test-namespace_my-db`, `u: test-namespace_my-db` and
`p: <random-password>` (with base64 encoded values).

When the MongoDB resource is deleted, the database user and the secret are deleted as well.
If `spec.database.pruneAfterDelete` is true, the whole database with all content will be deleted.

With an appropriate Kustomize configuration (similar to the configuration required for Sealed
Secrets), databases created this way can be used in PR deployments with name suffix.

The `connectionStringOptions` will overwrite the defaults which are used by the MongoDB operator itself
to connect to the MongoDB.

MongoDB Operator will set the `authSource` as the allowed database itself for MongoDB instances and
as the admin database for DocumentDB instances.
These settings are the defaults for the respective implementations when connecting to a specific
database.
Therefore `authSource` should not be configured in the connection options on client side when
connecting to a database provided by the MongoDB Operator.

## Caveats

* `spec.database.pruneAfterDelete: true` is only supported if the user of the MongoDB Operator is
  allowed to drop databases.
* Other settings than available in the secret for the database instance are not covered by the
  MongoDB Operator yet.
  `host`, `options`, etc. must be configured separately for each Kubernetes cluster unless the
  workload is configured with the `connectionString`.
* There is a hard limit of 64 characters for the database name.
  The database name is built from `<metadata.namespace>_<metadata.name>`.
  The namespace is used to avoid collisions and therefore data security issues.
  Be aware that the length of `metadata.namespace` plus the length of `metadata.name` does not
  exceed 63 characters.
  This error can be recognized in the log of the Operator and by the fact that no Secret is created
  for the MongoDb resource.
* In some rare cases the created secret does not match the created MongoDB user due to concurrency
  issues.
  We are still investigating on this bug.
  In such cases, the MongoDB resource can be deleted and created again to trigger a new setup of the
  user.
  This workaround **will delete the database and all collections** if
  `spec.database.pruneAfterDelete: true` is set and the MongoDB Operator has the
  [required privileges](deployment.md#database).
  It is important to disable `spec.database.pruneAfterDelete` and do not grant more than
  `userAdminAnyDatabase` to the MongoDB Operator user in production environments.


## Kustomize

When using [Kustomize](https://kustomize.io/) with `namePrefix` or `nameSuffix`, the MongoDb
resource must be treated the same way as a Secret, because a Secret with the same name will be
created by the MongoDB Operator.

The following configuration needs to be added to the `kustomization.yaml`.
It is derived from the built in `Secret` configuration. 

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

configurations:
  - mongodb-configuration.yaml
```

```yaml
# mongodb-configuration.yaml
nameReference:

  # MongoDbs must be treated like Secrets when used in conjunction with name suffix or prefix
  - group: persistence.sda-se.com
    version: v1beta1
    kind: MongoDb
    fieldSpecs:
      # taken from the specs of v1/Secret
      # https://github.com/kubernetes-sigs/kustomize/blob/master/api/konfig/builtinpluginconsts/namereference.go#L120
      - path: spec/volumes/secret/secretName
        version: v1
        kind: Pod
      - path: spec/containers/env/valueFrom/secretKeyRef/name
        version: v1
        kind: Pod
      - path: spec/initContainers/env/valueFrom/secretKeyRef/name
        version: v1
        kind: Pod
      - path: spec/containers/envFrom/secretRef/name
        version: v1
        kind: Pod
      - path: spec/initContainers/envFrom/secretRef/name
        version: v1
        kind: Pod
      - path: spec/imagePullSecrets/name
        version: v1
        kind: Pod
      - path: spec/volumes/projected/sources/secret/name
        version: v1
        kind: Pod
      - path: spec/template/spec/volumes/secret/secretName
        kind: Deployment
      - path: spec/template/spec/containers/env/valueFrom/secretKeyRef/name
        kind: Deployment
      - path: spec/template/spec/initContainers/env/valueFrom/secretKeyRef/name
        kind: Deployment
      - path: spec/template/spec/containers/envFrom/secretRef/name
        kind: Deployment
      - path: spec/template/spec/initContainers/envFrom/secretRef/name
        kind: Deployment
      - path: spec/template/spec/imagePullSecrets/name
        kind: Deployment
      - path: spec/template/spec/volumes/projected/sources/secret/name
        kind: Deployment
      - path: spec/template/spec/volumes/secret/secretName
        kind: ReplicaSet
      - path: spec/template/spec/containers/env/valueFrom/secretKeyRef/name
        kind: ReplicaSet
      - path: spec/template/spec/initContainers/env/valueFrom/secretKeyRef/name
        kind: ReplicaSet
      - path: spec/template/spec/containers/envFrom/secretRef/name
        kind: ReplicaSet
      - path: spec/template/spec/initContainers/envFrom/secretRef/name
        kind: ReplicaSet
      - path: spec/template/spec/imagePullSecrets/name
        kind: ReplicaSet
      - path: spec/template/spec/volumes/projected/sources/secret/name
        kind: ReplicaSet
      - path: spec/template/spec/volumes/secret/secretName
        kind: DaemonSet
      - path: spec/template/spec/containers/env/valueFrom/secretKeyRef/name
        kind: DaemonSet
      - path: spec/template/spec/initContainers/env/valueFrom/secretKeyRef/name
        kind: DaemonSet
      - path: spec/template/spec/containers/envFrom/secretRef/name
        kind: DaemonSet
      - path: spec/template/spec/initContainers/envFrom/secretRef/name
        kind: DaemonSet
      - path: spec/template/spec/imagePullSecrets/name
        kind: DaemonSet
      - path: spec/template/spec/volumes/projected/sources/secret/name
        kind: DaemonSet
      - path: spec/template/spec/volumes/secret/secretName
        kind: StatefulSet
      - path: spec/template/spec/containers/env/valueFrom/secretKeyRef/name
        kind: StatefulSet
      - path: spec/template/spec/initContainers/env/valueFrom/secretKeyRef/name
        kind: StatefulSet
      - path: spec/template/spec/containers/envFrom/secretRef/name
        kind: StatefulSet
      - path: spec/template/spec/initContainers/envFrom/secretRef/name
        kind: StatefulSet
      - path: spec/template/spec/imagePullSecrets/name
        kind: StatefulSet
      - path: spec/template/spec/volumes/projected/sources/secret/name
        kind: StatefulSet
      - path: spec/template/spec/volumes/secret/secretName
        kind: Job
      - path: spec/template/spec/containers/env/valueFrom/secretKeyRef/name
        kind: Job
      - path: spec/template/spec/initContainers/env/valueFrom/secretKeyRef/name
        kind: Job
      - path: spec/template/spec/containers/envFrom/secretRef/name
        kind: Job
      - path: spec/template/spec/initContainers/envFrom/secretRef/name
        kind: Job
      - path: spec/template/spec/imagePullSecrets/name
        kind: Job
      - path: spec/template/spec/volumes/projected/sources/secret/name
        kind: Job
      - path: spec/jobTemplate/spec/template/spec/volumes/secret/secretName
        kind: CronJob
      - path: spec/jobTemplate/spec/template/spec/volumes/projected/sources/secret/name
        kind: CronJob
      - path: spec/jobTemplate/spec/template/spec/containers/env/valueFrom/secretKeyRef/name
        kind: CronJob
      - path: spec/jobTemplate/spec/template/spec/initContainers/env/valueFrom/secretKeyRef/name
        kind: CronJob
      - path: spec/jobTemplate/spec/template/spec/containers/envFrom/secretRef/name
        kind: CronJob
      - path: spec/jobTemplate/spec/template/spec/initContainers/envFrom/secretRef/name
        kind: CronJob
      - path: spec/jobTemplate/spec/template/spec/imagePullSecrets/name
        kind: CronJob
      - path: spec/tls/secretName
        kind: Ingress
      - path: metadata/annotations/ingress.kubernetes.io\/auth-secret
        kind: Ingress
      - path: metadata/annotations/nginx.ingress.kubernetes.io\/auth-secret
        kind: Ingress
      - path: metadata/annotations/nginx.ingress.kubernetes.io\/auth-tls-secret
        kind: Ingress
      - path: spec/tls/secretName
        kind: Ingress
      - path: imagePullSecrets/name
        kind: ServiceAccount
      - path: parameters/secretName
        kind: StorageClass
      - path: parameters/adminSecretName
        kind: StorageClass
      - path: parameters/userSecretName
        kind: StorageClass
      - path: parameters/secretRef
        kind: StorageClass
      - path: rules/resourceNames
        kind: Role
      - path: rules/resourceNames
        kind: ClusterRole
      - path: spec/template/spec/containers/env/valueFrom/secretKeyRef/name
        kind: Service
        group: serving.knative.dev
        version: v1
      - path: spec/azureFile/secretName
        kind: PersistentVolume

```

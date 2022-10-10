# How the MongoDB Operator works

The MongoDB Operator reacts on reconcile requests of the Kubernetes API.

When a `MongoDb` resource is reconciled, it checks the status and creates a new database user and
the Kubernetes secret if needed.

On cleanup requests, it will delete the user and, if requested, removes the database.
Kubernetes will care about deleting the secret when deletion of the `MongoDb` resource is requested.

The process is described in the following sequence diagram:

```plantuml
!theme cerulean

box
  participant "K8S" as k8s
  entity MongoDb as MDB
  entity Secret as S
end box
participant "MongoDB\nOperator" as MO
database "DB" as DB

[--> MDB: apply
activate MDB
k8s -> MO: reconcile MongoDB resource
  activate MO
  MO <-- MDB: read spec
  MO -> MO: not up to date?
  activate MO
  MO -> MO: build db name
  MO -> DB++: create user
  return created
  MO -> S: create secret
  activate S
  MO -> MDB: update status
  deactivate MO
deactivate MO

[--> MDB: delete
k8s -> MO: cleanup MongoDB resource
  activate k8s
  activate MO
  MO <-- MDB: read spec
  MO -> MO: build db name
  MO -> DB++: delete user
  return deleted
  MO -> MO: prune after delete?
  activate MO
    MO -> DB++: delete database
    return deleted
  deactivate MO
return finalized
k8s -> S !! : delete
k8s -> MDB !! : delete
deactivate k8s
```
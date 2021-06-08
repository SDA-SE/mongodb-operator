# MongoDB Operator

---

**DRAFT**

This project is a draft version and may become a useful tool in the future.

---


A Kubernetes Operator that creates users and databases in existing MongoDB clusters based on a
custom resource.

## Development

TODO

## Local Deployment

Prerequisites: 

- a running Docker Desktop installation.
- properly installed [Kind](https://kind.sigs.k8s.io/docs/user/quick-start)
- Kubernetes CLI [kubectl 1.21.1+](https://kubectl.docs.kubernetes.io/)
- [kubectx](https://github.com/ahmetb/kubectx) (optional)
- a shell with the root of this project as working directory

1. Create Kubernetes cluster
   
   Use the prepared configuration to create a local cluster with Kind.
   Kind will configure the context for the local cluster running on Docker.
    
   ```console
   $ kind create cluster --config=local/setup/mongodb-operator-cluster.yaml
   Creating cluster "mongodb-operator-cluster" ...
    ✓ Ensuring node image (kindest/node:v1.20.2) 🖼 
    ✓ Preparing nodes 📦  
    ✓ Writing configuration 📜 
    ✓ Starting control-plane 🕹️ 
    ✓ Installing CNI 🔌 
    ✓ Installing StorageClass 💾 
   Set kubectl context to "kind-mongodb-operator-cluster"
   You can now use your cluster with:
   
   kubectl cluster-info --context kind-mongodb-operator-cluster
   
   Have a question, bug, or feature request? Let us know! https://kind.sigs.k8s.io/#community 🙂
   $ kubectx -c
   kind-mongodb-operator-cluster
   ```
   
1. Install infrastructure
   
   Install Kubernetes infrastructure components and wait until they are deployed.
   
   ```console
   $ kubectl apply -k kustomize/overlays/infra/
   namespace/ingress-nginx created
   serviceaccount/ingress-nginx created
   serviceaccount/ingress-nginx-admission created
   role.rbac.authorization.k8s.io/ingress-nginx created
   role.rbac.authorization.k8s.io/ingress-nginx-admission created
   clusterrole.rbac.authorization.k8s.io/ingress-nginx created
   clusterrole.rbac.authorization.k8s.io/ingress-nginx-admission created
   rolebinding.rbac.authorization.k8s.io/ingress-nginx created
   rolebinding.rbac.authorization.k8s.io/ingress-nginx-admission created
   clusterrolebinding.rbac.authorization.k8s.io/ingress-nginx created
   clusterrolebinding.rbac.authorization.k8s.io/ingress-nginx-admission created
   configmap/ingress-nginx-controller created
   service/ingress-nginx-controller created
   service/ingress-nginx-controller-admission created
   deployment.apps/ingress-nginx-controller created
   job.batch/ingress-nginx-admission-create created
   job.batch/ingress-nginx-admission-patch created
   validatingwebhookconfiguration.admissionregistration.k8s.io/ingress-nginx-admission created
   $ kubectl wait -n ingress-nginx --for=condition=ready pod -l=app.kubernetes.io/component=controller --timeout=90s
   pod/ingress-nginx-controller-6c74dd986c-vkjfv condition met
   ```

1. Install MongoDB
   
   Install the MongoDB used for testing and check if the frontend is exposed.
   The web frontend is available at [http://localhost](http://localhost).
   
   ```console
   $ kubectl apply -k kustomize/overlays/mongodb/
   namespace/mongodb created
   secret/mongodb-root-ttdd8k9hf6 created
   secret/mongoku-mongodb-admin-9g9g827kcc created
   service/mongodb created
   service/mongoku created
   deployment.apps/mongodb created
   deployment.apps/mongoku created
   ingress.networking.k8s.io/mongodb created
   $ curl -s http://localhost | grep "<title>"
     <title>Mongoku</title>
   ```
   
   
1. Install the operator
   
   Install the operator and check if the CRD is available.
   
   ```console
   $ kubectl apply -k kustomize/overlays/operator/
   customresourcedefinition.apiextensions.k8s.io/mongodbs.persistence.sda-se.com created
   $ kubectl get crd
   NAME                              CREATED AT
   mongodbs.persistence.sda-se.com   2021-06-07T15:54:40Z
   ```
   
1. Install a MongoDB database for testing
   
   Install the MongoDB as you would do when you need it for a service and check if the operator will
   provide a secret:
     
   ```console
   $ kubectl kustomize kustomize/overlays/test/
   apiVersion: v1
   kind: Namespace
   metadata:
     name: local-test
   ---
   apiVersion: persistence.sda-se.com/v1beta1
   kind: MongoDb
   metadata:
     name: test
     namespace: local-test
   spec: {}
   $ kubectl apply -k kustomize/overlays/test/
   namespace/local-test created
   mongodb.persistence.sda-se.com/test created
   $ kubectl get -n local-test mongo
   NAME   AGE
   test   33s
   $ kubectl get -n local-test secret
   TODO this needs to be implemented
   ```
   
1. Cleanup
   
   To value resources of your Docker installation, delete the local cluster:
   
   ```console
   $ kind delete cluster --name mongodb-operator-cluster
   Deleting cluster "kind-mongodb-operator-cluster" ...
   ```
   
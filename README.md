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

1. Create local Docker Registry
   
   Start a local registry in Docker so you can run locally built images in the kubernetes cluster.
   
   ```console
   $ docker run -d --restart=always -p "127.0.0.1:5000:5000" --name "kind-registry" registry:2
   Unable to find image 'registry:2' locally
   2: Pulling from library/registry
   ddad3d7c1e96: Pull complete 
   6eda6749503f: Pull complete 
   363ab70c2143: Pull complete 
   5b94580856e6: Pull complete 
   12008541203a: Pull complete 
   Digest: sha256:bac2d7050dc4826516650267fe7dc6627e9e11ad653daca0641437abdf18df27
   Status: Downloaded newer image for registry:2
   71b8b621bf962a65a0b45b08731430be7b9c0f13ce540a19708be7cc3e325c56
   ```
   
1. Create Kubernetes cluster
   
   Use the prepared configuration to create a local cluster with Kind and connect it to the
   registry.
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
   $ docker network connect "kind" "kind-registry"
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
   configmap/local-registry-hosting created
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
   $ curl -f -s http://localhost | grep "<title>" || echo "FAILED"
     <title>Mongoku</title>
   ```
   
   If the last command shows _FAILED_, usually the Mongoku frontend is still starting.
   This can be checked with: `kubectl get -n mongodb pods`
   
1. Build operator image
   
   To be used locally, the operator image must be pushed to the registry created above.
   
   ```console
   $ docker build -t localhost:5000/mongodb-operator:local .
   [+] Building 0.2s (5/5) FINISHED                                                                                                                                                                                                                                                                                        
    => [internal] load build definition from Dockerfile                                                                                                                                                                                                                                                               0.0s
    => => transferring dockerfile: 123B                                                                                                                                                                                                                                                                               0.0s
    => [internal] load .dockerignore                                                                                                                                                                                                                                                                                  0.0s
    => => transferring context: 2B                                                                                                                                                                                                                                                                                    0.0s
    => [internal] load metadata for docker.io/library/nginx:latest                                                                                                                                                                                                                                                    0.0s
    => [1/1] FROM docker.io/library/nginx                                                                                                                                                                                                                                                                             0.1s
    => exporting to image                                                                                                                                                                                                                                                                                             0.0s
    => => exporting layers                                                                                                                                                                                                                                                                                            0.0s
    => => writing image sha256:4c0932b7341ac6d882d0dcea6d1ce0d38b796414f5fbd9b9ef72ded245d7651e                                                                                                                                                                                                                       0.0s
    => => naming to localhost:5000/mongodb-operator:local
   $ docker push localhost:5000/mongodb-operator:local
   The push refers to repository [localhost:5000/mongodb-operator]
   6b93c0e56d01: Pushed 
   2f2780a1a18d: Pushed 
   7278048f2330: Pushed 
   fc621d08b12b: Pushed 
   2230366c7c6c: Pushed 
   14a1ca976738: Pushed 
   local: digest: sha256:de97907522dc30885426c5125098d000982065e000ed41f2b47178a82479bd75 size: 1570
   ```
   
1. Install the operator
   
   Install the operator, check if the CRD is available and the operator is up and running.
   
   ```console
   $ kubectl apply -k kustomize/overlays/operator/
   customresourcedefinition.apiextensions.k8s.io/mongodbs.persistence.sda-se.com created
   $ kubectl get crd
   NAME                              CREATED AT
   mongodbs.persistence.sda-se.com   2021-06-07T15:54:40Z
   $ kubectl get -n mongodb-operator pods
   NAME                                READY   STATUS    RESTARTS   AGE
   mongodb-operator-79bf4d9bd7-xxrql   1/1     Running   0          55s
   ```
   
1. Install a MongoDB database for testing
   
   Install the MongoDB as you would do when you need it for a service and check if the operator will
   provide a secret.
     
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
     name: local-test-db
     namespace: local-test
   spec: {}
   $ kubectl apply -k kustomize/overlays/test/
   namespace/local-test created
   mongodb.persistence.sda-se.com/local-test-db created
   $ kubectl get -n local-test mongo
   NAME   AGE
   test   33s
   $ kubectl get -n local-test secret
   TODO this needs to be implemented
   ```
   
1. Cleanup
   
   To value resources of your Docker installation, delete the local cluster and remove the registry.
   
   ```console
   $ kind delete cluster --name mongodb-operator-cluster
   Deleting cluster "kind-mongodb-operator-cluster" ...
   $ docker stop kind-registry
   kind-registry
   $ docker rm kind-registry
   kind-registry
   ```
   
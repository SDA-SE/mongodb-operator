# MongoDB Operator

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SDA-SE_mongodb-operator&metric=alert_status&token=efbe8726a565fc89dc219aee7da6df82910a9fe4)](https://sonarcloud.io/summary/new_code?id=SDA-SE_mongodb-operator)
[![FOSSA Status](https://app.fossa.com/api/projects/custom%2B8463%2Fmongodb-operator.svg?type=shield)](https://app.fossa.com/projects/custom%2B8463%2Fmongodb-operator?ref=badge_shield)

A Kubernetes Operator that creates users and databases in existing MongoDB clusters based on a
custom resource.

## Local Deployment

### Prerequisites: 

- a running Docker Desktop installation.
- properly installed [Kind](https://kind.sigs.k8s.io/docs/user/quick-start)
- Kubernetes CLI [kubectl 1.21.1+](https://kubectl.docs.kubernetes.io/)
- [kubectx](https://github.com/ahmetb/kubectx) (optional)
- a shell with the root of this project as working directory

### Start

The local setup can be created with a single command:

```console
$ ./start.sh
🏗 Creating a local Docker registry …
🏗 Creating local Kubernetes cluster with KinD …
🏗 Connecting local Kubernetes cluster to local Docker registry …
🏗 Installing infrastructure components in local Kubernetes cluster …
⏳ Waiting until Ingress Controller is ready …
🏗 Installing MongoDB in local Kubernetes cluster …
⏳ Waiting for Mongoku to be ready …
🏗 Build MongoDB operator and publish to connected registry …
🏗 Installing MongoDB operator in local Kubernetes cluster …
⏳ Wait for MongoDB operator to be ready …
🏗 You can now install MongoDB custom resources and watch how secrets and databases are created by the operator:
   $ kubectl apply -k kustomize/overlays/test/
``` 

When things go wrong, please refer to the [manual installation](#manual-installation) documentation
where all steps are described in detail.
In most cases, errors are caused by timing issues.
In this case `start.sh` can be retried.
It will check for things that are already done if they can't be executed twice.

### Stop

To stop the manual setup and free resources just run:

```console
$ ./stop.sh
💣 Removing local Kubernetes cluster with KinD …
💣 Removing local Docker registry …
```

### Manual installation

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
   
   It may be needed to [remove the `ValidatingWebhookConfiguration`](https://stackoverflow.com/a/62044090) for the
   Ingress resource:
   
   ```console
   $ kubectl delete -A ValidatingWebhookConfiguration ingress-nginx-admission
   validatingwebhookconfiguration.admissionregistration.k8s.io "ingress-nginx-admission" deleted
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
   $ ./gradlew jib -Djib.to.image=localhost:5000/mongodb-operator:local -Djib.allowInsecureRegistries=true
   > Task :jib
   
   Containerizing application to localhost:5000/mongodb-operator:local...
   Base image 'quay.io/sdase/openjdk-runtime:11-hotspot-distroless' does not use a specific image digest - build may not be reproducible
   The credential helper (docker-credential-desktop) has nothing for server URL: localhost:5000
   
   Got output:
   
   credentials not found in native keychain
   
   Cannot verify server at https://localhost:5000/v2/. Attempting again with no TLS verification.
   Failed to connect to https://localhost:5000/v2/ over HTTPS. Attempting again with HTTP.
   Using base image with digest: sha256:c987f9fd30e233c4763a48cbf9d1164ac3eaf7ea01eda1f98d446cc8fcc33571
   
   Container entrypoint set to [java, -cp, /app/resources:/app/classes:/app/libs/*, com.sdase.k8s.operator.mongodb.MongoDbOperator]
   Container program arguments set to []
   
   Built and pushed image as localhost:5000/mongodb-operator:local
   Executing tasks:
   [==============================] 100,0% complete
   
   
   BUILD SUCCESSFUL in 7s
   2 actionable tasks: 1 executed, 1 up-to-date
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


## Documentation

This service contains a documentation that is built using [TechDocs](https://backstage.io/docs/features/techdocs/techdocs-overview)
and [MkDocs](https://www.mkdocs.org/).
Documentation is written as Markdown files inside the `docs` folder.
You have to register each file in the navigation inside `mkdocs.yml`.
To preview the documentation locally, use `npx @techdocs/cli serve` (requires Nodejs and Docker).

For more details, see [our documentation about publishing TechDocs to Backstage](https://sda.dev/developer-guide/development/backstage/provide-documentation-in-backstage/).

## Contributing

We are looking forward to contributions.
Take a look at our [Contribution Guidelines](CONTRIBUTING.md) before submitting Pull Requests.

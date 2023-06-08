# ArgoCD

If applications that use `MongoDb` resources are [deployed](deployment.md) with [ArgoCD](https://argo-cd.readthedocs.io/),
no information about success is available in the ArgoCD UI.
The application seems healthy even when no database can be created.

To enhance visibility of custom resources, ArgoCD has a feature to provide
[health checks](https://argo-cd.readthedocs.io/en/stable/operator-manual/health/#resource-health)
to consider the custom resource like `MongoDb` for the overall health of an application.

The specific [Lua health script](https://github.com/SDA-SE/mongodb-operator/blob/master/health/argocd/persistence.sda-se.com/MongoDb/health.lua)
for the `MongoDb` custom resource can be added to the `argocd-cm` ConfigMap.

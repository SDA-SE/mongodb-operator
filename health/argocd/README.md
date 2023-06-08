# Health Checks for ArgoCD

This directory contains the root source of an ArgoCD health check for `MongoDb` resources.
It is written in _Lua_ as [required by ArgoCD](https://argo-cd.readthedocs.io/en/stable/operator-manual/health/#resource-health).
The setup follows the [conventions of contribution](https://argo-cd.readthedocs.io/en/stable/operator-manual/health/#way-2-contribute-a-custom-health-check)
but adds a Lua script for easier local testing.

[`health.lua`](./persistence.sda-se.com/MongoDb/health.lua) can be used in the `argocd-cm`
ConfigMap.

## Development and Testing

### Preparation

```console
$ brew install lua
==> Fetching lua
==> Caveats
You may also want luarocks:
  brew install luarocks
==> Summary
🍺  /opt/homebrew/Cellar/lua/5.4.6: 29 files, 788.1KB
==> Running `brew cleanup lua`...
$ brew install luarocks
==> Fetching luarocks
==> Summary
🍺  /opt/homebrew/Cellar/luarocks/3.9.2: 103 files, 728.3KB
==> Running `brew cleanup luarocks`...
$ luarocks --server=http://rocks.moonscript.org install lyaml
lyaml 6.2.8-1 is now installed in /opt/homebrew (license: MIT/X11)
```

### Executing tests

In base dir of MongoDB Operator repository:

```console
$ lua health/argocd/persistence.sda-se.com/MongoDb/health_test.lua
```
